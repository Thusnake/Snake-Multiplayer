package thusnake.snakemultiplayer;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Thusnake on 01-Aug-16.
 */
public class AcceptThread extends Thread {
  private BluetoothServerSocket serverSocket;
  private OpenGLES20Activity originActivity;

  AcceptThread(OpenGLES20Activity activity) {
    // Use a temporary object that is later assigned to serverSocket,
    // because serverSocket is final
    BluetoothServerSocket tmp = null;
    this.originActivity = activity;
    try {
      // MY_UUID is the app's UUID string, also used by the client code
      UUID uuid = UUID.fromString("55199d92-8a72-4fb7-807e-f482efeff3d6");
      tmp = activity.bluetoothAdapter.listenUsingRfcommWithServiceRecord("host",uuid);
    } catch (IOException e) { System.out.println(e.getMessage()); }
    serverSocket = tmp;
  }

  @Override
  public void run() {
    // Create an empty socket object.
    BluetoothSocket socket;
    // Keep listening until exception occurs or a socket is returned.
    while (true) {
      if (serverSocket != null) {
        try {
          socket = serverSocket.accept();
        } catch (IOException e) {
          break;
        }
        if (socket != null) {
          // Do work to manage the connection (in a separate thread)
          System.out.println("Host: Socket Connected Completely");
          manageConnectedSocket(socket);
        }
      } else {
        BluetoothServerSocket tmp = null;
        try {
          // MY_UUID is the app's UUID string, also used by the client code
          UUID uuid = UUID.fromString("55199d92-8a72-4fb7-807e-f482efeff3d6");
          tmp = originActivity.bluetoothAdapter.listenUsingRfcommWithServiceRecord("host",uuid);
        } catch (IOException e) { System.out.println(e.getMessage()); }
        serverSocket = tmp;
      }
    }
  }

  /** Will cancel the listening socket, and cause the thread to finish */
  public void cancel() {
    try {
      serverSocket.close();
    } catch (IOException e) { System.out.println("Couldn't close accept socket."); }
  }

  private void manageConnectedSocket(BluetoothSocket socket) {
    int index;
    for (index = 0; index < 3; index++)
      if (originActivity.connectedThreads[index] == null)
        break;

    originActivity.connectedThreads[index] = new ConnectedThread(originActivity, socket);
    originActivity.connectedThreads[index].start();

    // Inform everyone of the change in number of devices.
    originActivity.writeBytesAuto(new byte[]
        {Protocol.NUMBER_OF_DEVICES, (byte) originActivity.getNumberOfRemoteDevices()});
  }
}
