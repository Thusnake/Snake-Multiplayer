package thusnake.snakemultiplayer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

final class GameSetupBuffer {
  protected String savingPrefix = "";

  protected enum GameMode {CLASSIC, SPEEDY, VS_AI, CUSTOM}
  protected GameMode gameMode = GameMode.CLASSIC;
  protected final AtomicInteger horizontalSquares = new AtomicInteger(20),
      verticalSquares = new AtomicInteger(20), speed = new AtomicInteger(8),
      numberOfApples = new AtomicInteger(1);
  protected int difficulty;
  protected final CornerMap cornerMap = new CornerMap();
  protected boolean stageBorders;

  CornerMap getCornerMap() { return cornerMap; }

  public void adjustToDifficultyAndGameMode() {
    switch(gameMode) {

      case CLASSIC:
        horizontalSquares.set(10 + difficulty * 5);
        verticalSquares.set(10 + difficulty * 5);
        speed.set(6 + difficulty * 3);
        stageBorders = true;
        break;

      case SPEEDY:
        horizontalSquares.set(10 + difficulty * 5);
        verticalSquares.set(10 + difficulty * 5);
        speed.set(10 + difficulty * 5);
        stageBorders = false;
        break;

      case VS_AI:
        horizontalSquares.set(20);
        verticalSquares.set(20);
        speed.set(10);
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

  /**
   * Turns settings saving and loading on.
   * @param prefix The prefix to use when saving settings. Make sure it doesn't interfere with
   *                any other prefixes.
   */
  public void enableSaving(String prefix) { savingPrefix = prefix; }

  public boolean savingIsEnabled() { return !savingPrefix.equals(""); }

  /** Gets you the player specific saving prefix string from a given saving prefix and player. */
  public String getPlayerSavingPrefix(Player player) {
    return savingPrefix + "-" + player.getControlCorner(this).toString();
  }

  /** Performs the saving of this buffer's settings to the shared preferences. */
  public void saveSettings(Context context) {
    if (savingPrefix.equals("")) return;

    // General settings.
    SharedPreferences.Editor settings
        = context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit();
    settings.putInt(savingPrefix + "-gamemode-last", gameMode.ordinal());
    settings.putInt(savingPrefix + "-difficulty-last", difficulty);

    // CornerMap settings.
    for (PlayerController.Corner corner : PlayerController.Corner.values()) {
      settings.putBoolean(savingPrefix + "-" + corner.toString() + "-player-enabled",
          cornerMap.getPlayer(corner) != null);
    }

    // Player settings.
    for (Player player : cornerMap.getPlayers()) {
      String playerPrefix = getPlayerSavingPrefix(player);
      settings.putString(playerPrefix + "-name", player.getName());
      settings.putInt(playerPrefix + "-skin", player.getSkinIndex());
      settings.putString(playerPrefix + "-controllerid", player.getPlayerController().identifier());
      player.getPlayerController().saveSettings(settings, this);
    }

    settings.apply();
  }

  /** Performs the loading of last saved settings for this buffer from the shared preferences. */
  public void loadSettings(Context context) {
    if (savingPrefix.equals("")) return;
    SharedPreferences settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);

    // General settings.
    int gameModeIndex = settings.getInt(savingPrefix + "gamemode-last", -1);
    for (int index = 0; index < GameMode.values().length; index++)
      if (index == gameModeIndex)
        gameMode = GameMode.values()[index];

    difficulty = settings.getInt(savingPrefix + "-difficulty-last", difficulty);

    // CornerMap settings.
    for (PlayerController.Corner corner : PlayerController.Corner.values())
      if (settings.getBoolean(savingPrefix + "-" + corner.toString() + "-player-enabled", false)
          && cornerMap.getPlayer(corner) == null)
        cornerMap.addPlayer(new Player(((OpenGLES20Activity) context).getRenderer()), corner);
      else if (!settings.getBoolean(savingPrefix+"-"+corner.toString() + "-player,enabled", false)
               && cornerMap.getPlayer(corner) != null)
        cornerMap.removePlayer(cornerMap.getPlayer(corner));

    // Player settings.
    for (Player player : cornerMap.getPlayers()) {
      String playerPrefix = getPlayerSavingPrefix(player);
      player.setName(settings.getString(playerPrefix + "-name", player.getName()));
      player.setSkin(SnakeSkin.allSkins.get(settings.getInt(playerPrefix + "-skin",
                                                            player.getSkinIndex())));
      player.loadSavedController(this);
    }
  }

  /**
   * Compiles a list of protocol calls to be aggregated for synchronization with remote devices.
   * @param thread The thread holding a socket to the receiver (different guests will receive
   *                different instructions for syncing).
   * @return The resulting list of calls.
   */
  public List<byte[]> allInformationCallList(ConnectedThread thread) {
    List<byte[]> calls = new LinkedList<>();
    // Game calls.
    calls.add(new byte[] {Protocol.HOR_SQUARES_CHANGED, (byte) horizontalSquares.get()});
    calls.add(new byte[] {Protocol.VER_SQUARES_CHANGED, (byte) verticalSquares.get()});
    calls.add(new byte[] {Protocol.SPEED_CHANGED, (byte) speed.get()});
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
