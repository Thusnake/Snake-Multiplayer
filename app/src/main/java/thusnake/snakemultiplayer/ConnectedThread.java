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

  private boolean isReady = false;
  private SimpleTimer disconnectRequestTimer = new SimpleTimer(0.0);
  private SimpleTimer lastActivityTimer = new SimpleTimer(0.0);

  public ConnectedThread(OpenGLES20Activity activity, BluetoothSocket socket) {
    this.socket = socket;
    this.originActivity = activity;
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
          inStream.read(buffer);
          originActivity.getRenderer().handleInputBytes(buffer, this);
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
    finally {
      // End the guest menu mode.
      if (originActivity.isGuest())
        originActivity.getRenderer().getMenu().endGuest();
    }
  }

  public void setReady(boolean ready) {
    this.isReady = ready;

    // Count the ready devices.
    int readyDevices = 0;
    for (ConnectedThread thread : originActivity.connectedThreads)
      if (thread != null && thread.isReady())
        readyDevices++;

    if (originActivity.isReady()) readyDevices++;

    // Tell everyone how many devices are ready and if they are ready themselves.
    for (ConnectedThread thread : originActivity.connectedThreads)
      if (thread != null)
        thread.write(new byte[] {Protocol.READY_NUMBER_AND_STATUS,
                                 (byte) readyDevices, thread.isReady ? (byte) 1 : (byte) 0});
  }

  public boolean isReady() { return isReady; }

  public SimpleTimer getDisconnectRequestTimer() { return disconnectRequestTimer; }

  public SimpleTimer getLastActivityTimer() { return lastActivityTimer; }
}
