package thusnake.snakemultiplayer;

import android.util.Pair;

/**
 * Created by Nick on 22/02/2018.
 */

public class Protocol {
  // Guest codes
  public static final byte REQUEST_CONNECT = 0;
  public static final byte REQUEST_MOVE = 1; // Followed by the move's id.
  public static final byte REQUEST_ADD_SNAKE = 2;
  public static final byte REQUEST_CONTROLLED_SNAKES = 3;

  // Universal codes
  public static final byte DIRECTION_CHANGE = 10; // Followed by 1 movement byte.
  public static final byte SNAKE1_COLOR_CHANGED = 20; // Followed by 1 color byte.
  public static final byte SNAKE2_COLOR_CHANGED = 21; // Followed by 1 color byte.
  public static final byte SNAKE3_COLOR_CHANGED = 22; // Followed by 1 color byte.
  public static final byte SNAKE4_COLOR_CHANGED = 23; // Followed by 1 color byte.
  public static final byte SNAKE1_CORNER_CHANGED = 30; // Followed by 1 corner byte.
  public static final byte SNAKE2_CORNER_CHANGED = 31; // Followed by 1 corner byte.
  public static final byte SNAKE3_CORNER_CHANGED = 32; // Followed by 1 corner byte.
  public static final byte SNAKE4_CORNER_CHANGED = 33; // Followed by 1 corner byte.
  public static final byte SNAKE1_NAME_CHANGED = 40; // Followed by 1 length byte and a string of chars.
  public static final byte SNAKE2_NAME_CHANGED = 41; // Followed by 1 length byte and a string of chars.
  public static final byte SNAKE3_NAME_CHANGED = 42; // Followed by 1 length byte and a string of chars.
  public static final byte SNAKE4_NAME_CHANGED = 43; // Followed by 1 length byte and a string of chars.
  public static final byte SPEED_CHANGED = 50; // Followed by 1 speed byte.

  // Host codes
  public static final byte REQUEST_NAME = -1;
  public static final byte CONTROLLED_SNAKES_LIST = -3; // Followed by 0-4 snake number bytes.
  public static final byte APPROVE_CONNECT = -10;
  public static final byte START_GAME = -20;
  public static final byte END_GAME = -30; // Followed by 1 winner byte.
  public static final byte GAME_MOVEMENT_OCCURRED = -40; // Followed by the move's id and 1 movement byte.
  public static final byte GAME_MOVEMENT_INFORMATION = -41; // Followed by the move's id and 1 movement byte.
  public static final byte GAME_MOVEMENT_MISSING = -42; // Followed by the move's id.
  public static final byte GAME_APPLE_MOVED = -50; // Followed by 2 coordinate bytes.

  // Movement code methods.
  public static byte getMovementCode(Player.Direction direction1, Player.Direction direction2,
                                     Player.Direction direction3, Player.Direction direction4) {
    byte dir1 = encodeDirection(direction1);
    byte dir2 = encodeDirection(direction2);
    byte dir3 = encodeDirection(direction3);
    byte dir4 = encodeDirection(direction4);
    dir2 = (byte) (dir2 << 2);
    dir3 = (byte) (dir3 << 4);
    dir4 = (byte) (dir4 << 6);
    return (byte) (dir1 | dir2 | dir3 | dir4);
  }

  public static byte getMovementCode(int playerNumber, Player.Direction direction) {
    switch (playerNumber) {
      case 0: return getMovementCode(direction, null, null, null);
      case 1: return getMovementCode(null, direction, null, null);
      case 2: return getMovementCode(null, null, direction, null);
      case 3: return getMovementCode(null, null, null, direction);
      default: throw new RuntimeException("Players are not counted from zero");
    }
  }
  
  public static void decodeMovementCode(byte code, Player.Direction[] array) {
    if (array == null || array.length < 4) return;
    for (int index = 0; index < 4; index++)
      array[index] = decodeDirection(code / (int) Math.pow(2, index * 2));
  }

  private static byte encodeDirection(Player.Direction direction) {
    switch (direction) {
      case UP: return 0;
      case DOWN: return 1;
      case LEFT: return 2;
      case RIGHT: return 3;
      default: return 0;
    }
  }
  
  private static Player.Direction decodeDirection(int code) {
    switch (code) {
      case 0: return Player.Direction.UP;
      case 1: return Player.Direction.DOWN;
      case 2: return Player.Direction.LEFT;
      case 3: return Player.Direction.RIGHT;
      default: return Player.Direction.UP;
    }
  }

  public static byte encodeCorner(PlayerController.Corner corner) {
    switch (corner) {
      case LOWER_LEFT: return 0;
      case UPPER_LEFT: return 1;
      case UPPER_RIGHT: return 2;
      case LOWER_RIGHT: return 3;
      default: return 0;
    }
  }

  public static PlayerController.Corner decodeCorner(byte code) {
    switch (code) {
      case 0: return PlayerController.Corner.LOWER_LEFT;
      case 1: return PlayerController.Corner.UPPER_LEFT;
      case 2: return PlayerController.Corner.UPPER_RIGHT;
      case 3: return PlayerController.Corner.LOWER_RIGHT;
      default: return PlayerController.Corner.LOWER_LEFT;
    }
  }

  public static Pair<Byte, Byte> encodeMoveID(int moveID) {
    return new Pair<>((byte) (moveID & 0xFF), (byte) ((moveID >> 8) & 0xFF));
  }

  public static int decodeMoveID(byte firstByte, byte secondByte) {
    return firstByte + (secondByte << 8);
  }
}
