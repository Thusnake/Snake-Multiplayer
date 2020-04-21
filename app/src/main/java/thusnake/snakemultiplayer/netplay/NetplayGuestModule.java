package thusnake.snakemultiplayer.netplay;

public class NetplayGuestModule extends NetplayModule {
  private ConnectedThread hostInterfaceThread;

  @Override
  public void writeBytesAuto(byte[] bytes) {
    if (hostInterfaceThread != null)
      hostInterfaceThread.write(bytes);
  }
}
