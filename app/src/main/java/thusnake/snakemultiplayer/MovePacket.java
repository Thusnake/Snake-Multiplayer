package thusnake.snakemultiplayer;

public class MovePacket {
  public enum Type {SNAKE_MOVE, APPLE_MOVE}

  private Type type;
  private final byte[] contents;

  // Constructor for a outbound snake move packet.
  public MovePacket(Snake.Direction direction1, Snake.Direction direction2,
                    Snake.Direction direction3, Snake.Direction direction4) {
    this.type = Type.SNAKE_MOVE;
    this.contents = new byte[4];
  }

  // Constructor for an inbound snake move packet.
  public MovePacket(byte[] receivedBytes) {
    this.contents = receivedBytes.clone();
  }

  // Produces a packet as a byte array ready for sending, given a protocol code to be placed as the
  // first byte. It does NOT check if the code makes sense.
  public byte[] producePacket(byte protocolCode) {
    this.contents[0] = protocolCode;
    return this.contents;
  }
}
