package thusnake.snakemultiplayer;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Thusnake on 01-Aug-16.
 */
public class ConnectThread extends Thread {
  private final BluetoothSocket mmSocket;
  private final BluetoothDevice mmDevice;
  private final OpenGLES20Activity originActivity;

  public ConnectThread(OpenGLES20Activity activity, BluetoothDevice device) {
    // Use a temporary object that is later assigned to mmSocket,
    // because mmSocket is final
    BluetoothSocket tmp = null;
    mmDevice = device;
    this.originActivity = activity;

    // Get a BluetoothSocket to connect with the given BluetoothDevice
    try {
      // MY_UUID is the app's UUID string, also used by the server code
      UUID uuid = UUID.fromString("55199d92-8a72-4fb7-807e-f482efeff3d6");
      tmp = device.createRfcommSocketToServiceRecord(uuid);
    } catch (IOException e) { }
    mmSocket = tmp;
  }

  public void run() {
    // Cancel discovery because it will slow down the connection
    originActivity.bluetoothAdapter.cancelDiscovery();

    try {
      // Connect the device through the socket. This will block
      // until it succeeds or throws an exception
      System.out.println("Guest: Attempting to connect");
      mmSocket.connect();
    } catch (IOException connectException) {
      // Unable to connect; close the socket and get out
      try {
        mmSocket.close();
      } catch (IOException closeException)
        { System.out.println("Something went wrong with the socket."); }
      return;
    } finally {
      // Remove the interrupting menu message upon connecting.
      originActivity.getRenderer().setInterruptingMessage(null);
    }

    // Do work to manage the connection (in a separate thread)
    System.out.println("Guest: Socket Connected Completely");
    manageConnectedSocket(mmSocket);
  }

  /** Will cancel an in-progress connection, and close the socket */
  public void cancel() {
    try {
      mmSocket.close();
    } catch (IOException e) {
      System.out.println("Connect socket unable to close properly: " + e.getMessage());
    }
  }

  private void manageConnectedSocket(BluetoothSocket socket) {
    originActivity.connectedThread = new ConnectedThread(this.originActivity, socket);
    originActivity.connectedThread.start();
  }

  BluetoothDevice getCurrentDevice() {
    return mmDevice;
  }
}
