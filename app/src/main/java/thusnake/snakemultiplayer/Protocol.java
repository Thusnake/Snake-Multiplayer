package thusnake.snakemultiplayer;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nick on 22/02/2018.
 */

public class Protocol {
  // Guest codes
  public static final byte REQUEST_MOVE = 1; // Followed by the move's id.
  public static final byte REQUEST_ADD_SNAKE = 2; // Followed by 1 corner byte.
  public static final byte REQUEST_AVAILABLE_SNAKES = 3;
  public static final byte REQUEST_CONTROLLED_SNAKES = 4;
  public static final byte IS_READY = 5;
  public static final byte IS_NOT_READY = 6;
  public static final byte REQUEST_NUMBER_OF_READY = 7;
  public static final byte REQUEST_NUMBER_OF_DEVICES = 8;
  public static final byte DISCONNECT_REQUEST = 66;
  public static final byte WILL_DISCONNECT = 67;

  // Universal codes
  public static final byte ALL_DIRECTIONS = 10; // Followed by 1 movement byte.
  public static final byte SNAKE_DIRECTION_CHANGE = 11; // Followed by 1 corner byte and 1 direction byte.
  public static final byte SNAKE_LL_SKIN = 20; // Followed by 1 skin byte.
  public static final byte SNAKE_UL_SKIN = 21; // Followed by 1 skin byte.
  public static final byte SNAKE_UR_SKIN = 22; // Followed by 1 skin byte.
  public static final byte SNAKE_LR_SKIN = 23; // Followed by 1 skin byte.
  public static final byte SNAKE_LL_NAME = 40; // Followed by 1 length byte and a string of chars.
  public static final byte SNAKE_UL_NAME = 41; // Followed by 1 length byte and a string of chars.
  public static final byte SNAKE_UR_NAME = 42; // Followed by 1 length byte and a string of chars.
  public static final byte SNAKE_LR_NAME = 43; // Followed by 1 length byte and a string of chars.
  public static final byte SPEED_CHANGED = 50; // Followed by 1 byte.
  public static final byte HOR_SQUARES_CHANGED = 51; // Followed by 1 byte.
  public static final byte VER_SQUARES_CHANGED = 52; // Followed by 1 byte.
  public static final byte STAGE_BORDERS_CHANGED = 53; // Followed by 1 boolean byte.

  // Aggregate call codes.
  public static final byte BASIC_AGGREGATE_CALL = 60; // Followed by series of calls and NEXT_CALL bytes.
  public static final byte NEXT_CALL = 61;
  public static final byte GAME_START_CALL = 62;
  public static final byte GAME_START_RECEIVED = 63;

  public static final byte PING = 68;
  public static final byte PING_ANSWER = 69;

  // Host codes
  public static final byte REQUEST_NAME = -1;
  public static final byte AVAILABLE_SNAKES_LIST = -2; // Followed by 0-4 snake number bytes.
  public static final byte CONTROLLED_SNAKES_LIST = -3; // Followed by 0-4 snake number bytes.
  public static final byte NUMBER_OF_READY = -4; // Followed by number of ready devices.
  public static final byte NUMBER_OF_DEVICES = -5; // Followed by number of devices.
  public static final byte READY_STATUS = -6; // Followed by a device's personal boolean ready value.
  public static final byte NUM_DEVICES_AND_READY_WITH_STATUS = -7; // Followed by number of connected devices, ready devices and device's personal boolean ready value.

  public static final byte DETAILED_SNAKES_LIST = -8; // Followed by 4 bytes representing one of the following states:
  public static final byte DSL_SNAKE_OFF = 0;
  public static final byte DSL_SNAKE_LOCAL = 1;
  public static final byte DSL_SNAKE_REMOTE = 2;

  public static final byte APPROVE_CONNECT = -10;
  public static final byte START_GAME = -20;
  public static final byte GAME_MODE = -21; // Followed by 1 game mode index byte.
  public static final byte END_GAME = -30; // Followed by 1 winner corner byte.
  public static final byte GAME_MOVEMENT_OCCURRED = -40; // Followed by the move's id and 1 movement byte.
  public static final byte GAME_MOVEMENT_MISSING = -41; // Followed by the move's id.
  public static final byte GAME_APPLE_EATEN_NEXT_POS = -42; // Followed by the action's id, the apple's entity number and 2 coordinate bytes.
  public static final byte GAME_ENTITY_POS_CHANGE = -43; // Followed by the action's id, the entity's number and 2 coordinate bytes.

  public static final byte DISCONNECT = 65;

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
  
  public static void decodeMovementCode(byte code, Player.Direction[] array) {
    if (array == null || array.length < 4) return;
    for (int index = 0; index < 4; index++)
      array[index] = decodeDirection((code >> (index * 2)) & 0x3);
  }

  public static byte encodeDirection(Player.Direction direction) {
    switch (direction) {
      case UP: return 0;
      case DOWN: return 1;
      case LEFT: return 2;
      case RIGHT: return 3;
      default: return 0;
    }
  }
  
  public static Player.Direction decodeDirection(int code) {
    switch (code) {
      case 0: return Player.Direction.UP;
      case 1: return Player.Direction.DOWN;
      case 2: return Player.Direction.LEFT;
      case 3: return Player.Direction.RIGHT;
      default: throw new RuntimeException("Fix your decodeMovementCode function.");
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
    // First transform to unsigned.
    int firstInt, secondInt;
    if (firstByte < 0) firstInt = firstByte + 0xFF + 1;
    else               firstInt = firstByte;
    if (secondByte < 0) secondInt = secondByte + 0xFF + 1;
    else                secondInt = secondByte;

    return firstInt | secondInt << 8;
  }

  public static byte[] encodeSeveralCalls(List<byte[]> callsList, byte header) {
    int totalLength = 1;
    for (byte[] call : callsList)
      totalLength += call.length + 1;

    byte[] outputCall = new byte[totalLength];
    outputCall[0] = header;
    int outputIndex = 1;
    for (byte[] call : callsList) {
      outputCall[outputIndex++] = (byte) call.length;
      for (byte callByte : call)
        outputCall[outputIndex++] = callByte;
    }

    return outputCall;
  }

  public static List<byte[]> decodeSeveralCalls(byte[] aggregateCall) {
    List<byte[]> callsList = new ArrayList<>();

    int currentIndex = 1;
    while(currentIndex < aggregateCall.length) {
      if (aggregateCall[currentIndex] == Protocol.GAME_START_CALL) break;

      // Go through as many bytes as the current one decides to and add them to an array.
      byte[] currentCall = new byte[aggregateCall[currentIndex]];

      for (int currentCallIndex = 0; currentCallIndex < currentCall.length; currentCallIndex++) {
        currentIndex++;
        currentCall[currentCallIndex] = aggregateCall[currentIndex];
      }

      // Add to the list of calls.
      callsList.add(currentCall);

      // Increment currentIndex to start from the next call's first byte.
      currentIndex++;
    }

    return callsList;
  }
}
