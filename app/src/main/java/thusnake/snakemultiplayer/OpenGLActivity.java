package thusnake.snakemultiplayer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
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
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import thusnake.snakemultiplayer.netplay.AcceptThread;
import thusnake.snakemultiplayer.netplay.ConnectThread;
import thusnake.snakemultiplayer.netplay.ConnectedThread;
import thusnake.snakemultiplayer.netplay.Protocol;

/**
 * Created by ASRock on 24-Feb-16.
 */

public class OpenGLActivity extends Activity implements RewardedVideoAdListener {
  public static OpenGLActivity current;
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

    current = this;
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

  @Override
  public boolean dispatchGenericMotionEvent(MotionEvent ev) {
    if ((ev.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        || (ev.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
      gameView.onGenericMotionEvent(ev);
    return super.dispatchGenericMotionEvent(ev);
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    if ((event.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        || (event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
      gameView.onKeyEvent(event);
    return super.dispatchKeyEvent(event);
  }

  public GameRenderer getRenderer() {return this.gameView.getGameRenderer(); }

  public GameSurfaceView getSurfaceView() { return gameView; }

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
    // Display the interrupting message.
    getRenderer().setInterruptingMessage(new FullscreenMessage(getRenderer(),
        "Thanks for the support! Take this " + reward.getAmount() + " " + reward.getType() + "!"));

    // If the reward is a ladder (which it should be)
    if (reward.getType().equals("ladder")) {
      SharedPreferences.Editor editor = getSharedPreferences("progress", MODE_PRIVATE).edit();
      editor.putInt("ladders", ladders.addAndGet(reward.getAmount()));
      editor.apply();
    }
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
