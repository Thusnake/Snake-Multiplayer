package thusnake.snakemultiplayer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ASRock on 24-Feb-16.
 */

public class OpenGLES20Activity extends Activity implements RewardedVideoAdListener {
  private GameSurfaceView gameView;
  public Vibrator vibrator;
  public AcceptThread acceptThread;
  public ConnectThread connectThread;
  public ConnectedThread connectedThread;
  public ConnectedThread[] connectedThreads = new ConnectedThread[3];
  public BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
  public final int REQUEST_ENABLE_BT = 1;
  public ArrayList<String> arrayAdapter = new ArrayList<>();
  public ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();

  public Collection<ConnectedThread> awaitingDisconnectThreads = new CopyOnWriteArraySet<>();

  // Bluetooth related variables
  private boolean isReady = false;
  private boolean discoveryStarted = false;

  // Guest-only
  public int numberOfRemoteDevices = 0;
  public int numberOfReadyRemoteDevices = 0;

  // Advertisements
  private RewardedVideoAd videoAd;
  private boolean videoAdLoaded;
  AtomicInteger ladders;

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
      } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
        discoveryStarted = true;
      } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
        discoveryStarted = false;
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

    // Create a GLSurfaceView instance and set it
    // as the ContentView for this Activity.
    gameView = new GameSurfaceView(this);
    setContentView(gameView);

    // Register the BroadcastReceiver
    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
    registerReceiver(broadcastReceiver, filter); // Don't forget to unregister during onDestroy

    MobileAds.initialize(this, getString(R.string.app_id));

    this.videoAd = MobileAds.getRewardedVideoAdInstance(this);
    videoAd.setRewardedVideoAdListener(this);
    this.loadAd();

    ladders = new AtomicInteger(getSharedPreferences("progress", MODE_PRIVATE).getInt("ladders",0));

    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
  }

  @Override
  protected void onResume() {
    super.onResume();
    gameView.onResume();
    videoAd.resume(this);

    // Hide both the navigation bar and the status bar.
    hideUI();

  }

  public void hideUI() {
    int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_FULLSCREEN
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    getWindow().getDecorView().setSystemUiVisibility(uiOptions);
  }

  @Override
  protected void onPause() {
    super.onPause();
    gameView.onPause();
    videoAd.pause(this);
  }

  @Override
  protected  void onDestroy(){
    super.onDestroy();
    videoAd.destroy(this);

    unregisterReceiver(broadcastReceiver);
  }

  @Override
  public void onBackPressed() {
    if (!getRenderer().isInGame()) {
      getRenderer().getMenu().goBack();
    }
  }

  public GameRenderer getRenderer() {return this.gameView.getGameRenderer(); }

  public GameSurfaceView getSurfaceView() { return gameView; }

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

  // Disconnects a thread and the corresponding socket.
  public void closeConnectedGuestThread(ConnectedThread thread) {
    int index;
    for (index = 0; index < connectedThreads.length; index++)
      if (connectedThreads[index] != null) {
        if (connectedThreads[index].equals(thread))
          break;
        else if (index == connectedThreads.length - 1)
          // Reached the end and found no match to break the loop.
          return;
      }

    connectedThreads[index].cancel();
    connectedThreads[index] = null;

    // Inform everyone of changes.
    writeBytesAuto(new byte[]
        {Protocol.NUMBER_OF_DEVICES, (byte) getNumberOfRemoteDevices()});
    writeBytesAuto(new byte[]
        {Protocol.NUMBER_OF_READY, (byte) getNumberOfReadyRemoteDevices()});
  }

  /**
   * Sets this device's remote lobby ready status.
   * Calling it as a guest sends a request for status change instead.
   */
  public void setReady(boolean ready) {
    // Send a request to the host if you're a guest.
    if (this.isGuest())
      connectedThread.write(new byte[] {(ready) ? Protocol.IS_READY : Protocol.IS_NOT_READY});

    // If you're a host yourself tell everyone how many are ready.
    else if (isHost()) {
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

  /** Directly sets the isReady value of this device. */
  public void forceSetReady(boolean ready) {
    this.isReady = ready;
  }

  public boolean isReady() { return this.isReady; }

  public boolean discoveryStarted() { return discoveryStarted; }


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


  public void loadAd() {
    videoAd.loadAd(getString(R.string.test_video_ad_id), new AdRequest.Builder().build());
    videoAdLoaded = false;
  }

  public void showAd() {
    if (videoAd.isLoaded())
      videoAd.show();
  }

  public boolean videoAdIsLoaded() {
    return videoAd != null && videoAdLoaded;
  }

  @Override
  public void onRewarded(RewardItem reward) {
    getRenderer().setInterruptingMessage(new FullscreenMessage(getRenderer(),
        "Thanks for the support! Take this " + reward.getAmount() + " " + reward.getType()));
  }

  @Override
  public void onRewardedVideoAdLeftApplication() {}

  @Override
  public void onRewardedVideoAdClosed() {
    // Load a new one.
    this.loadAd();
  }

  @Override
  public void onRewardedVideoAdFailedToLoad(int errorCode) {}

  @Override
  public void onRewardedVideoAdLoaded() {
    videoAdLoaded = true;
  }

  @Override
  public void onRewardedVideoAdOpened() {}

  @Override
  public void onRewardedVideoStarted() {}

  @Override
  public void onRewardedVideoCompleted() {}
}
