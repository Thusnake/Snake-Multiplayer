package thusnake.snakemultiplayer.netplay;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import thusnake.snakemultiplayer.OpenGLActivity;
import thusnake.snakemultiplayer.SimpleTimer;
import thusnake.snakemultiplayer.SnakeSkin;
import thusnake.snakemultiplayer.controllers.ControllerBuffer;

/**
 * Manages a Bluetooth connection.
 */
public abstract class ConnectedThread extends Thread {
  private final BluetoothSocket socket;
  final OpenGLActivity originActivity;
  private final InputStream inStream;
  private final OutputStream outStream;
  public final BluetoothDevice device;

  private boolean isReady = false;
  protected double lastActivityTime;

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

          this.handleInput(inputBytes);

          lastActivityTime = System.nanoTime();
        }
      } catch (IOException e) { break; }
    }
  }

  /**
   * Called every time an input is available.
   *
   * @param inputBytes Input to handle.
   */
  void handleInput(byte[] inputBytes) {
    switch(inputBytes[0]) {
      case Protocol.REQUEST_NUMBER_OF_DEVICES:
        write(new byte[] {
                Protocol.NUMBER_OF_DEVICES,
                (byte) originActivity.getNumberOfRemoteDevices()
        });
        break;

      case Protocol.REQUEST_NUMBER_OF_READY:
        write(new byte[] {
                Protocol.NUMBER_OF_READY,
                (byte) originActivity.getNumberOfReadyRemoteDevices()
        });
        break;

      case Protocol.PING:
        write(new byte[] {Protocol.PING_ANSWER});
        break;

      case Protocol.SNAKE_LL_SKIN:
        if (originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(ControllerBuffer.Corner.LOWER_LEFT) != null)
          originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(ControllerBuffer.Corner.LOWER_LEFT).setSkin(SnakeSkin.allSkins.get(inputBytes[1]));
        break;

      case Protocol.SNAKE_UL_SKIN:
        if (originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(ControllerBuffer.Corner.UPPER_LEFT) != null)
          originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(ControllerBuffer.Corner.UPPER_LEFT).setSkin(SnakeSkin.allSkins.get(inputBytes[1]));
        break;

      case Protocol.SNAKE_UR_SKIN:
        if (originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(ControllerBuffer.Corner.UPPER_RIGHT) != null)
          originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(ControllerBuffer.Corner.UPPER_RIGHT).setSkin(SnakeSkin.allSkins.get(inputBytes[1]));
        break;

      case Protocol.SNAKE_LR_SKIN:
        if (originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(ControllerBuffer.Corner.LOWER_RIGHT) != null)
          originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(ControllerBuffer.Corner.LOWER_RIGHT).setSkin(SnakeSkin.allSkins.get(inputBytes[1]));
        break;

      case Protocol.SNAKE_LL_NAME:
        if (originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(ControllerBuffer.Corner.LOWER_LEFT) != null)
          originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(ControllerBuffer.Corner.LOWER_LEFT).setName(new String(inputBytes, 1, inputBytes.length - 1));
        break;

      case Protocol.SNAKE_UL_NAME:
        if (originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(ControllerBuffer.Corner.UPPER_LEFT) != null)
          originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(ControllerBuffer.Corner.UPPER_LEFT).setName(new String(inputBytes, 1, inputBytes.length - 1));
        break;

      case Protocol.SNAKE_UR_NAME:
        if (originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(ControllerBuffer.Corner.UPPER_RIGHT) != null)
          originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(ControllerBuffer.Corner.UPPER_RIGHT).setName(new String(inputBytes, 1, inputBytes.length - 1));
        break;

      case Protocol.SNAKE_LR_NAME:
        if (originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(ControllerBuffer.Corner.LOWER_RIGHT) != null)
          originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(ControllerBuffer.Corner.LOWER_RIGHT).setName(new String(inputBytes, 1, inputBytes.length - 1));
        break;

      default:
        handleRoleSpecificInput(inputBytes);
    }
  }

  /**
   * Called when input request is found to be role-specific.
   * Therefore this method must be implemented by subclasses.
   *
   * @param inputBytes Input to handle. It is guaranteed that this input contains a role-specific or
   *                   an invalid request.
   */
  abstract void handleRoleSpecificInput(byte[] inputBytes);

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
}
