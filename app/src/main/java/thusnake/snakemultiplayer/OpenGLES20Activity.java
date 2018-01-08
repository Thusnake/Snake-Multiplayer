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
    private GameSurfaceView mGLView;
    public static AcceptThread acptThread;
    public static ConnectThread cnctThread;
    public static ConnectedThread cnctdThread;
    public static ConnectedThread[] cnctdThreads = new ConnectedThread[3];
    private Vibrator v;
    static BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public final static int REQUEST_ENABLE_BT = 1;
    public static ArrayList<String> mArrayAdapter = new ArrayList<>();
    public static ArrayList<BluetoothDevice> mDevices = new ArrayList<>();

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (!mDevices.contains(device))mDevices.add(device);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mBluetoothAdapter.cancelDiscovery();
                    for (int i=mGLView.getGameRenderer().getPairedDevices().length; i<mArrayAdapter.size(); i++) {
                        mArrayAdapter.remove(i);
                    }
                    mBluetoothAdapter.startDiscovery();
                } else {
                    mBluetoothAdapter.cancelDiscovery();
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
        mGLView = new GameSurfaceView(this);
        setContentView(mGLView);

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        if (mBluetoothAdapter != null) acptThread = new AcceptThread();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();

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
        mGLView.onPause();
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

        unregisterReceiver(mReceiver);
    }

    static int random(int min, int max){
        Random rand = new Random();
        return rand.nextInt((max-min)+1) + min;
    }

    static float randomf(float min, float max) {
        Random rand = new Random();
        return rand.nextFloat() * (max - min) + min;
    }
}
