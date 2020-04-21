package thusnake.snakemultiplayer.netplay;

import java.util.Collection;
import java.util.HashSet;

public class NetplayHostModule extends NetplayModule {
  private final Collection<ConnectedThread> guestInterfaceThreads = new HashSet<ConnectedThread>();

  @Override
  public void writeBytesAuto(byte[] bytes) {
    for (ConnectedThread thread : guestInterfaceThreads)
      thread.write(bytes);
  }

  @Override
  public void setReady(boolean ready) {
    this.forceSetReady(true);

    int readyDevices = 0;
    for (ConnectedThread thread : guestInterfaceThreads)
      if (thread != null && thread.isReady())
        readyDevices++;

    if (ready) readyDevices++;

    for (ConnectedThread thread : guestInterfaceThreads)
      if (thread != null)
        thread.write(new byte[]{Protocol.NUMBER_OF_READY, (byte) readyDevices});
  }

  @Override
  public int getNumberOfRemoteDevices() {
    int connected = 1;
    for (ConnectedThread thread : guestInterfaceThreads)
      if (thread != null)
        connected++;
    return connected;
  }

  @Override
  public int getNumberOfReadyRemoteDevices() {
    int ready = (this.isReady()) ? 1 : 0;
    for (ConnectedThread thread : guestInterfaceThreads)
      if (thread != null && thread.isReady())
        ready++;
    return ready;
  }

  /**
   * Cleanup method for after a HostThread has been closed.
   */
  void onGuestInterfaceThreadDisconnected(ConnectedThread disconnectedThread) {
    guestInterfaceThreads.remove(disconnectedThread);

    // Inform everyone of changes.
    writeBytesAuto(new byte[] {
            Protocol.NUMBER_OF_DEVICES,
            (byte) getNumberOfRemoteDevices()
    });
    writeBytesAuto(new byte[] {
            Protocol.NUMBER_OF_READY,
            (byte) getNumberOfReadyRemoteDevices()
    });
  }
}
