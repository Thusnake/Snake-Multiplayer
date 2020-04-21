package thusnake.snakemultiplayer.netplay;

/**
 * Interface for all network games.
 * Can be either a NetplayHostModule or a NetplayGuestModule.
 */
public abstract class NetplayModule {
  private boolean isReady = false;

  public NetplayModule() {}

  /** Sends a byte array to the host or to all the guests, determined automatically. */
  public abstract void writeBytesAuto(byte[] bytes);

  /**
   * Sets this device's remote lobby ready status.
   * Calling it as a guest sends a request for status change instead.
   */
  public abstract void setReady(boolean ready);

  /** Directly sets the isReady value of this device. */
  void forceSetReady(boolean ready) {
    this.isReady = ready;
  }

  public boolean isReady() { return this.isReady; }

  /**
   * @return The number of devices currently connected to this netplay session.
   */
  public abstract int getNumberOfRemoteDevices();

  /**
   * @return The number of ready devices currently connected to this netplay session.
   */
  public abstract int getNumberOfReadyRemoteDevices();
}
