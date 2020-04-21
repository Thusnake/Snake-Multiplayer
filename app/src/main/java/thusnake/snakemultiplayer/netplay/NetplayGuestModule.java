package thusnake.snakemultiplayer.netplay;

public class NetplayGuestModule extends NetplayModule {
  private ConnectedThread hostInterfaceThread;

  // Guest-only
  int numberOfRemoteDevices = 0;
  int numberOfReadyRemoteDevices = 0;

  @Override
  public void writeBytesAuto(byte[] bytes) {
    if (hostInterfaceThread != null)
      hostInterfaceThread.write(bytes);
  }

  @Override
  public void setReady(boolean ready) {
    hostInterfaceThread.write(new byte[] {(ready) ? Protocol.IS_READY : Protocol.IS_NOT_READY});
  }

  @Override
  public int getNumberOfRemoteDevices() {
    return numberOfRemoteDevices;
  }

  @Override
  public int getNumberOfReadyRemoteDevices() {
    return numberOfReadyRemoteDevices;
  }
}
