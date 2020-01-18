package thusnake.snakemultiplayer;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Thusnake on 01-Aug-16.
 */
public class ConnectedThread extends Thread {
  private final BluetoothSocket socket;
  final OpenGLActivity originActivity;
  private final InputStream inStream;
  private final OutputStream outStream;
  final BluetoothDevice device;

  private boolean isReady = false;
  private SimpleTimer disconnectRequestTimer = new SimpleTimer(0.0);
  private SimpleTimer lastActivityTimer = new SimpleTimer(0.0);

  public ConnectedThread(OpenGLActivity activity, BluetoothSocket socket) {
    this.socket = socket;
    this.originActivity = activity;
    this.device = socket.getRemoteDevice();
    InputStream tmpIn = null;
    OutputStream tmpOut = null;

    // Get the input and output streams, using temp objects because member streams are final.
    try {
      tmpIn = socket.getInputStream();
      tmpOut = socket.getOutputStream();
    } catch (IOException e) { }

    inStream = tmpIn;
    outStream = tmpOut;
  }

  @Override
  public void start() {
    super.start();
    // Call the method for setting up the guest menu.
    if (originActivity.getRenderer().getMenu().isGuest())
      originActivity.getRenderer().getMenu().beginGuest();
  }

  @Override
  public void run() {
    byte[] buffer = new byte[1024];  // buffer store for the stream

    // Keep listening to the InputStream until an exception occurs
    while (true) {
      try {
        if (inStream.available() > 0) {
          // Read from the InputStream
          int length = inStream.read(buffer);
          byte[] inputBytes = new byte[length];
          System.arraycopy(buffer, 0, inputBytes, 0, length);
          originActivity.getRenderer().handleInputBytes(inputBytes, this);
          lastActivityTimer.reset();
        }
      } catch (IOException e) { break; }
    }
  }

  /* Call this from the main activity to send data to the remote device */
  public void write(byte[] bytes) {
    try {
      outStream.write(bytes);
    } catch (IOException e) { System.out.println("Unable to send bytes : " + e.getMessage()); }
  }

  /* Call this from the main activity to shutdown the connection */
  public void cancel() {
    try {
      socket.close();
    } catch (IOException e) { System.out.println("Couldn't close connection: " + e.getMessage()); }
  }

  public void setReady(boolean ready) {
    this.isReady = ready;

    // Count the ready devices.
    int devices = 0, readyDevices = 0;
    for (ConnectedThread thread : originActivity.connectedThreads)
      if (thread != null) {
        devices++;
        if (thread.isReady) readyDevices++;
      }

    if (originActivity.isReady()) readyDevices++;

    // Tell everyone how many devices are ready and if they are ready themselves.
    for (ConnectedThread thread : originActivity.connectedThreads)
      if (thread != null)
        thread.write(new byte[] {Protocol.NUM_DEVICES_AND_READY_WITH_STATUS,
            (byte) devices, (byte) readyDevices, thread.isReady ? (byte) 1 : (byte) 0});
  }

  public boolean isReady() { return isReady; }

  public SimpleTimer getDisconnectRequestTimer() { return disconnectRequestTimer; }

  public SimpleTimer getLastActivityTimer() { return lastActivityTimer; }
}
