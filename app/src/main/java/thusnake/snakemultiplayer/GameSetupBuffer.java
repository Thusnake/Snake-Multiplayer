package thusnake.snakemultiplayer;

import java.util.LinkedList;
import java.util.List;

final class GameSetupBuffer {
  protected enum GameMode {CLASSIC, SPEEDY, VS_AI, CUSTOM}
  protected GameMode gameMode = GameMode.CLASSIC;
  protected int horizontalSquares, verticalSquares, speed;
  protected int difficulty;
  final protected CornerMap cornerMap = new CornerMap();
  protected boolean stageBorders;

  CornerMap getCornerMap() { return cornerMap; }

  public void adjustToDifficultyAndGameMode() {
    switch(gameMode) {

      case CLASSIC:
        horizontalSquares = 10 + difficulty * 5;
        verticalSquares = 10 + difficulty * 5;
        speed = 6 + difficulty * 3;
        stageBorders = true;
        break;

      case SPEEDY:
        horizontalSquares = 10 + difficulty * 5;
        verticalSquares = 10 + difficulty * 5;
        speed = 10 + difficulty * 5;
        stageBorders = false;
        break;

      case VS_AI:
        horizontalSquares = 20;
        verticalSquares = 20;
        speed = 10;
        stageBorders = true;
        break;

      default:
        break;
    }
  }

  public Game createGame(GameRenderer renderer) {
    switch(gameMode) {
      case CLASSIC:
      case SPEEDY:
        adjustToDifficultyAndGameMode();
        return new Game(renderer, this);
      case CUSTOM:
        return new Game(renderer, this);
      default:
        throw new RuntimeException("This game mode is not supported.");
    }
  }

  public List<byte[]> allInformationCallList(ConnectedThread thread) {
    List<byte[]> calls = new LinkedList<>();
    // Game calls.
    calls.add(new byte[] {Protocol.HOR_SQUARES_CHANGED, (byte) horizontalSquares});
    calls.add(new byte[] {Protocol.VER_SQUARES_CHANGED, (byte) verticalSquares});
    calls.add(new byte[] {Protocol.SPEED_CHANGED, (byte) speed});
    calls.add(new byte[] {Protocol.STAGE_BORDERS_CHANGED, (byte) (stageBorders ? 1 : 0)});
    calls.add(new byte[] {Protocol.GAME_MODE, (byte) gameMode.ordinal()});

    // Player calls.
    for (PlayerController.Corner corner : PlayerController.Corner.values()) {
      Player player = cornerMap.getPlayer(corner);
      if (player == null) continue;

      // Player skin index.
      byte skinCallByte = Protocol.SNAKE_LL_SKIN, nameCallByte = Protocol.SNAKE_LL_NAME;
      switch(corner) {
        case LOWER_LEFT: skinCallByte = Protocol.SNAKE_LL_SKIN; nameCallByte = Protocol.SNAKE_LL_NAME; break;
        case UPPER_LEFT: skinCallByte = Protocol.SNAKE_UL_SKIN; nameCallByte = Protocol.SNAKE_UL_NAME; break;
        case UPPER_RIGHT: skinCallByte = Protocol.SNAKE_UR_SKIN; nameCallByte = Protocol.SNAKE_UR_NAME; break;
        case LOWER_RIGHT: skinCallByte = Protocol.SNAKE_LR_SKIN; nameCallByte = Protocol.SNAKE_LR_NAME; break;
      }

      calls.add(new byte[] {skinCallByte, (byte) player.getSkinIndex()});

      byte[] nameCall = new byte[player.getName().length() + 1];
      nameCall[0] = nameCallByte;
      for (int index = 0; index < player.getName().length(); index++)
        nameCall[index + 1] = player.getName().getBytes()[index];
      calls.add(nameCall);
    }

    calls.add(makeDetailedSnakesList(thread));
    calls.add(new byte[] {Protocol.NUM_DEVICES_AND_READY_WITH_STATUS,
                          (byte) thread.originActivity.getNumberOfRemoteDevices(),
                          (byte) thread.originActivity.getNumberOfReadyRemoteDevices(),
                          (byte) (thread.isReady() ? 1 : 0)});

    return calls;
  }

  public byte[] makeDetailedSnakesList(ConnectedThread thread) {
    byte[] output = new byte[5];
    output[0] = Protocol.DETAILED_SNAKES_LIST;
    int outputIndex = 1;

    for (PlayerController.Corner corner : PlayerController.Corner.values()) {
      Player player = cornerMap.getPlayer(corner);
      if (player == null) output[outputIndex++] = Protocol.DSL_SNAKE_OFF;
      else if (player.getPlayerController() instanceof BluetoothController
          && ((BluetoothController) player.getPlayerController()).controllerThread.equals(thread))
        output[outputIndex++] = Protocol.DSL_SNAKE_LOCAL;
      else
        output[outputIndex++] = Protocol.DSL_SNAKE_REMOTE;
    }

    return output;
  }

  public static String gameModeToString(GameMode gameMode) {
    switch (gameMode) {
      case CLASSIC: return "Classic";
      case SPEEDY: return "Speedy";
      case VS_AI: return "AI Battle";
      case CUSTOM: return "Custom";
      default: return "Error";
    }
  }

  public static String difficultyToString(int difficulty) {
    switch (difficulty) {
      case 0: return "Mild";
      case 1: return "Fair";
      case 2: return "Tough";
      case 3: return "Bonkers";
      case 4: return "Ultimate";
      default: return "Error";
    }
  }
}
