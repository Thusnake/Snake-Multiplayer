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
}
