package thusnake.snakemultiplayer.netplay;

/**
 * Interface for all network games.
 */
public abstract class NetplayModule {
  // Bluetooth related variables
  private boolean isReady = false;
  private boolean discoveryStarted = false;

  public NetplayModule() {

  }

  /** Sends a byte array to the host or to all the guests, determined automatically. */
  public abstract void writeBytesAuto(byte[] bytes);

  // Disconnects a thread and the corresponding socket.
  public void closeConnectedGuestThread(ConnectedThread thread) {
    int index;
    for (index = 0; index < connectedThreads.length; index++)
      if (connectedThreads[index] != null) {
        if (connectedThreads[index].equals(thread))
          break;
        else if (index == connectedThreads.length - 1)
          // Reached the end and found no match to break the loop.
          return;
      }

    connectedThreads[index].cancel();
    connectedThreads[index] = null;

    // Inform everyone of changes.
    writeBytesAuto(new byte[]
            {Protocol.NUMBER_OF_DEVICES, (byte) getNumberOfRemoteDevices()});
    writeBytesAuto(new byte[]
            {Protocol.NUMBER_OF_READY, (byte) getNumberOfReadyRemoteDevices()});
  }

  /**
   * Sets this device's remote lobby ready status.
   * Calling it as a guest sends a request for status change instead.
   */
  public void setReady(boolean ready) {
    // Send a request to the host if you're a guest.
    if (this.isGuest())
      connectedThread.write(new byte[] {(ready) ? Protocol.IS_READY : Protocol.IS_NOT_READY});

      // If you're a host yourself tell everyone how many are ready.
    else if (isHost()) {
      this.isReady = ready;

      int readyDevices = 0;
      for (ConnectedThread thread : connectedThreads)
        if (thread != null && thread.isReady())
          readyDevices++;

      if (ready) readyDevices++;

      for (ConnectedThread thread : connectedThreads)
        if (thread != null)
          thread.write(new byte[]{Protocol.NUMBER_OF_READY, (byte) readyDevices});
    }
  }

  /** Directly sets the isReady value of this device. */
  public void forceSetReady(boolean ready) {
    this.isReady = ready;
  }

  public boolean isReady() { return this.isReady; }

  public boolean discoveryStarted() { return discoveryStarted; }


  public int getNumberOfRemoteDevices() {
    if (this.isGuest()) return numberOfRemoteDevices;
    else {
      int connected = 1;
      for (ConnectedThread thread : connectedThreads)
        if (thread != null)
          connected++;
      return connected;
    }
  }

  public int getNumberOfReadyRemoteDevices() {
    if (this.isGuest()) return numberOfReadyRemoteDevices;
    else {
      int ready = (isReady) ? 1 : 0;
      for (ConnectedThread thread : connectedThreads)
        if (thread != null && thread.isReady())
          ready++;
      return ready;
    }
  }
}
