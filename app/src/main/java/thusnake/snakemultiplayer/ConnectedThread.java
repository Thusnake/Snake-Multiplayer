package thusnake.snakemultiplayer;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Thusnake on 01-Aug-16.
 */
public class ConnectedThread extends Thread {
  private final BluetoothSocket socket;
  private final OpenGLES20Activity originActivity;
  private final InputStream inStream;
  private final OutputStream outStream;
  private int state = 1;
  private final int STATE_NOT_CONNECTED = 1;
  private final int STATE_CONNECTED = 2;

  public ConnectedThread(OpenGLES20Activity activity, BluetoothSocket socket) {
    this.socket = socket;
    this.originActivity = activity;
    InputStream tmpIn = null;
    OutputStream tmpOut = null;

    // Get the input and output streams, using temp objects because member streams are final.
    try {
      tmpIn = socket.getInputStream();
      tmpOut = socket.getOutputStream();
      state = 2;
    } catch (IOException e) { }

    inStream = tmpIn;
    outStream = tmpOut;
  }

  public void run() {
    byte[] buffer = new byte[1024];  // buffer store for the stream

    // Keep listening to the InputStream until an exception occurs
    while (true) {
      try {
        if (inStream.available() > 0) {
          // Read from the InputStream
          inStream.read(buffer);
          originActivity.getRenderer().handleInputBytes(buffer, this);
        }
      } catch (IOException e) { break; }
    }
  }

  /* Call this from the main activity to send data to the remote device */
  public void write(byte[] bytes) {
    try {
      outStream.write(bytes);
    } catch (IOException e) { }
  }

  /* Call this from the main activity to shutdown the connection */
  public void cancel() {
    try {
      socket.close();
    } catch (IOException e) { }
  }

  public int getCurrentState() {
    return state;
  }
}
