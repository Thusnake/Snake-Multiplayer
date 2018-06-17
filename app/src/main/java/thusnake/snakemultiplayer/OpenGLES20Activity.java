package thusnake.snakemultiplayer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by ASRock on 24-Feb-16.
 */

public class OpenGLES20Activity extends Activity {
  private GameSurfaceView gameView;
  public AcceptThread acceptThread;
  public ConnectThread connectThread;
  public ConnectedThread connectedThread;
  public ConnectedThread[] connectedThreads = new ConnectedThread[3];
  private Vibrator v;
  public BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
  public final int REQUEST_ENABLE_BT = 1;
  public ArrayList<String> arrayAdapter = new ArrayList<>();
  public ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();

  // Bluetooth related variables
  private boolean isReady = false;

  // Guest-only
  public int numberOfRemoteDevices = 0;
  public int numberOfReadyRemoteDevices = 0;

  // Create a BroadcastReceiver for ACTION_FOUND
  private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      // When discovery finds a device
      if (BluetoothDevice.ACTION_FOUND.equals(action)) {
        // Get the BluetoothDevice object from the Intent
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (!bluetoothDevices.contains(device)) bluetoothDevices.add(device);
        // Add the name and address to an array adapter to show in a ListView
        arrayAdapter.add(device.getName() + "\n" + device.getAddress());
        gameView.getGameRenderer().getMenu().addFoundDevice(device);
      }
    }
  };

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    switch (requestCode) {
      case 1: {
        if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          bluetoothAdapter.cancelDiscovery();
          for (int i=this.connectedThreads.length; i<arrayAdapter.size(); i++) {
            arrayAdapter.remove(i);
          }
          bluetoothAdapter.startDiscovery();
        } else {
          bluetoothAdapter.cancelDiscovery();
        }
      }
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    // Create a GLSurfaceView instance and set it
    // as the ContentView for this Activity.
    gameView = new GameSurfaceView(this);
    setContentView(gameView);

    // Register the BroadcastReceiver
    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    registerReceiver(broadcastReceiver, filter); // Don't forget to unregister during onDestroy
  }

  @Override
  protected void onResume() {
    super.onResume();
    gameView.onResume();

    View decorView = getWindow().getDecorView();
    // Hide both the navigation bar and the status bar.
    if (Build.VERSION.SDK_INT >= 19) {
      int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
          | View.SYSTEM_UI_FLAG_FULLSCREEN
          | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
          | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
          | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
          | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
      decorView.setSystemUiVisibility(uiOptions);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    gameView.onPause();
  }

  @Override
  protected  void onDestroy(){
        /*if (MyGLRenderer.playingBluetooth) {
            byte[] bytes = {8, 2};
            MyGLRenderer.writeHost(bytes);

            try {
                wait(1000);
            } catch (java.lang.InterruptedException e) {
            }
        }*/
    super.onDestroy();

    unregisterReceiver(broadcastReceiver);
  }

  public GameRenderer getRenderer() {return this.gameView.getGameRenderer(); }

  static int random(int min, int max){
    Random rand = new Random();
    return rand.nextInt((max-min)+1) + min;
  }

  public boolean isGuest() { return this.connectedThread != null; }

  public boolean isHost() {
    if (this.acceptThread != null)
      return true;
    else
      for (ConnectedThread thread : connectedThreads)
        if (thread != null)
          return true;
    return false;
  }

  // Sends a byte array to the host or to all the guests determined automatically.
  public void writeBytesAuto(byte[] bytes) {
    if (this.isGuest())
      connectedThread.write(bytes);
    else
      for (ConnectedThread thread : connectedThreads)
        if (thread != null)
          thread.write(bytes);
  }

  public void setReady(boolean ready) {
    // Send a request to the host if you're a guest.
    if (this.isGuest())
      connectedThread.write(new byte[] {(ready) ? Protocol.IS_READY : Protocol.IS_NOT_READY});

    // If you're a host yourself tell everyone how many are ready.
    else {
      this.isReady = ready;

      int readyDevices = 0;
      for (ConnectedThread thread : connectedThreads)
        if (thread != null && thread.isReady())
          readyDevices++;

      if (ready) readyDevices++;

      for (ConnectedThread thread : connectedThreads)
        if (thread != null)
          thread.write(new byte[]{Protocol.NUMBER_OF_READY, (byte) readyDevices});
    }
  }

  public void forceSetReady(boolean ready) {
    this.isReady = ready;
  }

  public boolean isReady() { return this.isReady; }


  public int getNumberOfRemoteDevices() {
    if (this.isGuest()) return numberOfRemoteDevices;
    else {
      int connected = 1;
      for (ConnectedThread thread : connectedThreads)
        if (thread != null)
          connected++;
      return connected;
    }
  }

  public int getNumberOfReadyRemoteDevices() {
    if (this.isGuest()) return numberOfReadyRemoteDevices;
    else {
      int ready = (isReady) ? 1 : 0;
      for (ConnectedThread thread : connectedThreads)
        if (thread != null && thread.isReady())
          ready++;
      return ready;
    }
  }
}
