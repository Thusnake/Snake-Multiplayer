package thusnake.snakemultiplayer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.UUID;

/**
 * Created by Thusnake on 01-Aug-16.
 */
public class AcceptThread extends Thread {
  private BluetoothServerSocket mmServerSocket;
  private OpenGLES20Activity originActivity;
  private int state = 1;
  private int connections = 0;

  public  AcceptThread(OpenGLES20Activity activity) {
    // Use a temporary object that is later assigned to mmServerSocket,
    // because mmServerSocket is final
    BluetoothServerSocket tmp = null;
    this.originActivity = activity;
    try {
      // MY_UUID is the app's UUID string, also used by the client code
      UUID uuid = UUID.fromString("55199d92-8a72-4fb7-807e-f482efeff3d6");
      tmp = activity.bluetoothAdapter.listenUsingRfcommWithServiceRecord("host",uuid);
    } catch (IOException e) { }
    mmServerSocket = tmp;
  }

  public void run() {
    state = 2;
    BluetoothSocket socket = null;
    // Keep listening until exception occurs or a socket is returned
    while (true) {
      if (mmServerSocket != null) {
        try {
          socket = mmServerSocket.accept();
        } catch (IOException e) {
          break;
        }
        // If a connection was accepted
        //try {
        if (socket != null) {
          // Do work to manage the connection (in a separate thread)
          System.out.println("Host: Socket Connected Completely");
          manageConnectedSocket(socket);
          connections++;
          //state = 1;
          //break;
        }
        //} catch (IOException e) {
        //    break;
        //}
      } else {
        BluetoothServerSocket tmp = null;
        try {
          // MY_UUID is the app's UUID string, also used by the client code
          UUID uuid = UUID.fromString("55199d92-8a72-4fb7-807e-f482efeff3d6");
          tmp = originActivity.bluetoothAdapter.listenUsingRfcommWithServiceRecord("host",uuid);
        } catch (IOException e) { }
        mmServerSocket = tmp;
      }
    }
  }

  /** Will cancel the listening socket, and cause the thread to finish */
  public void cancel() {
    try {
      mmServerSocket.close();
    } catch (IOException e) { }
  }

  private void manageConnectedSocket(BluetoothSocket socket) {
    int index = 0;
    for (index = 0; index < 4; index++) {
      if (originActivity.connectedThreads[index] == null) {
        break;
      }
    }
    originActivity.connectedThreads[index] = new ConnectedThread(socket);
    originActivity.connectedThreads[index].start();
    //byte bytes[] = {4,7,(byte)(4 - Player.getPlayingLocal())};
    //OpenGLES20Activity.cnctdThreads[index].write(bytes);
  }

  int getCurrentState() {
    return state;
  }
  int getConnections() {return connections;}
}
