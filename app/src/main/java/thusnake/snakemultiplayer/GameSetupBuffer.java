package thusnake.snakemultiplayer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedList;
import java.util.List;

import thusnake.snakemultiplayer.controllers.BluetoothControllerBuffer;
import thusnake.snakemultiplayer.controllers.ControllerBuffer;
import thusnake.snakemultiplayer.gamemodes.*;

/**
 * A singleton structure, which keeps information about the game, configured in the menu.
 */
public final class GameSetupBuffer {
  private static GameSetupBuffer singleplayerBuffer, multiplayerBuffer;
  private static boolean loaded = false;

  /**
   * Registers all the possible game modes and creates two static instances of GameSetupBuffer - one
   * for singleplayer and one for multiplayer, assigning the appropriate game modes to both.
   */
  private static void load() {
    // Register all the game modes via the IDGenerator.
    IDGenerator.registerGameMode(Classic.class);
    IDGenerator.registerGameMode(Speedy.class);
    IDGenerator.registerGameMode(Custom.class);

    // Assign game modes to the two buffers.
    int[] singleplayerGameModes = {IDGenerator.getGameModeID(Classic.class),
                                   IDGenerator.getGameModeID(Speedy.class),
                                   IDGenerator.getGameModeID(Custom.class)};

    int[] multiplayerGameModes = {IDGenerator.getGameModeID(Classic.class),
                                  IDGenerator.getGameModeID(Speedy.class),
                                  IDGenerator.getGameModeID(Custom.class)};

    // Initialize the buffers.
    singleplayerBuffer = new GameSetupBuffer(singleplayerGameModes);
    multiplayerBuffer = new GameSetupBuffer(multiplayerGameModes);

    loaded = true;
  }

  /**
   * @return The singleplayer GameSetupBuffer singleton.
   */
  public static GameSetupBuffer getSingleplayer() {
    if (!loaded) load();
    return singleplayerBuffer;
  }

  /**
   * @return The multiplayer GameSetupBuffer singleton.
   */
  public static GameSetupBuffer getMultiplayer() {
    if (!loaded) load();
    return multiplayerBuffer;
  }


  protected String savingPrefix = "";
  protected final CornerMap cornerMap = new CornerMap();
  protected GameMode gameMode;
  protected final List<GameMode> gameModes;

  private GameSetupBuffer(int[] gameModesIDs) {
    gameModes = new LinkedList<>();

    for (int id : gameModesIDs) {
      try {
        gameModes.add(IDGenerator.getGameModeClass(id).newInstance());
      } catch (Exception exception) {
        System.err.println("Couldn't create an instance of default game mode: "
            + IDGenerator.getGameModeClass(0));
      }
    }

    gameMode = gameModes.get(0);

    loadSettings(OpenGLActivity.current);
  }

  CornerMap getCornerMap() { return cornerMap; }

  public Game createGame() {
    Game game = gameMode.generateGame(cornerMap);
    if (this == singleplayerBuffer)
      game = game.withScoreSaveKey(gameMode.toString(), gameMode.getDifficulty().index);
    return game;
  }

  public boolean setGameMode(Class<? extends GameMode> gameModeClass) {
    for (GameMode gameMode : gameModes)
      if (gameModeClass.isInstance(gameMode)) {
        this.gameMode = gameMode;
        return true;
      }
    return false;
  }

  /**
   * Turns settings saving and loading on.
   * @param prefix The prefix to use when saving settings. Make sure it doesn't interfere with
   *                any other prefixes.
   */
  public void enableSaving(String prefix) { savingPrefix = prefix; }

  public boolean savingIsEnabled() { return !savingPrefix.equals(""); }

  public String getSavingPrefix() { return savingPrefix; }

  /** Gets you the player specific saving prefix string from a given saving prefix and player. */
  public String getPlayerSavingPrefix(Player player) {
    return savingPrefix + "-" + player.getControlCorner(this).toString();
  }

  /** Performs the saving of this buffer's settings to the shared preferences. */
  public void saveSettings(Context context) {
    if (savingPrefix.equals("")) return;

    // Game mode settings.
    SharedPreferences.Editor settings
        = context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit();
    settings.putString(savingPrefix + "gamemode-last", gameMode.toString());
    gameMode.saveSettings(settings, this);

    // CornerMap settings.
    for (ControllerBuffer.Corner corner : ControllerBuffer.Corner.values()) {
      settings.putBoolean(savingPrefix + "-" + corner.toString() + "-player-enabled",
          cornerMap.getPlayer(corner) != null);
    }

    // Player settings.
    for (Player player : cornerMap.getPlayers()) {
      String playerPrefix = getPlayerSavingPrefix(player);
      settings.putString(playerPrefix + "-name", player.getName());
      settings.putInt(playerPrefix + "-skin", player.getSkinIndex());
      settings.putString(playerPrefix + "-controllerid", player.getControllerBuffer().identifier());
      player.getControllerBuffer().saveSettings(settings, this);
    }

    settings.apply();
  }

  /** Performs the loading of last saved settings for this buffer from the shared preferences. */
  public void loadSettings(Context context) {
    if (savingPrefix.equals("")) return;
    SharedPreferences settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);

    // Game mode settings.
    String loadedGameModeName = settings.getString(savingPrefix + "gamemode-last", null);
    for (GameMode gameMode : gameModes) {
      gameMode.loadSettings(this);
      if (gameMode.toString().equals(loadedGameModeName))
        this.gameMode = gameMode;
    }


    // CornerMap settings.
    for (ControllerBuffer.Corner corner : ControllerBuffer.Corner.values())
      if (settings.getBoolean(savingPrefix + "-" + corner.toString() + "-player-enabled", false)
          && cornerMap.getPlayer(corner) == null)
        cornerMap.addPlayer(new Player(((OpenGLActivity) context).getRenderer()), corner);
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

    // GameMode calls.
    calls.addAll(gameMode.getSyncCallList());

    // Player calls.
    for (ControllerBuffer.Corner corner : ControllerBuffer.Corner.values()) {
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

    for (ControllerBuffer.Corner corner : ControllerBuffer.Corner.values()) {
      Player player = cornerMap.getPlayer(corner);
      if (player == null) output[outputIndex++] = Protocol.DSL_SNAKE_OFF;
      else if (player.getControllerBuffer() instanceof BluetoothControllerBuffer
          && ((BluetoothControllerBuffer) player.getControllerBuffer()).controllerThread.equals(thread))
        output[outputIndex++] = Protocol.DSL_SNAKE_LOCAL;
      else
        output[outputIndex++] = Protocol.DSL_SNAKE_REMOTE;
    }

    return output;
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
