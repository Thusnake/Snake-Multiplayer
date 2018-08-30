package thusnake.snakemultiplayer;

import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

final class GameSetupBuffer {
  protected enum GameMode {CLASSIC, SPEEDY, VS_AI, CUSTOM}
  protected GameMode gameMode;
  protected int horizontalSquares, verticalSquares, speed;
  protected int difficulty;
  private Map<PlayerController.Corner, Player> cornerMap = new HashMap<>();
  protected boolean stageBorders;

  GameSetupBuffer() {
    cornerMap.put(PlayerController.Corner.LOWER_LEFT, null);
    cornerMap.put(PlayerController.Corner.UPPER_LEFT, null);
    cornerMap.put(PlayerController.Corner.UPPER_RIGHT, null);
    cornerMap.put(PlayerController.Corner.LOWER_RIGHT, null);
  }

  /**
   * Adds a player to the setup buffer.
   * @param player The player to be added.
   * @param corner The control corner to be occupied by that player.
   * @return Whether the player was successfully added. This will return false only if the corner
   * had been previously occupied or the player is already in the game.
   */
  boolean addPlayer(Player player, PlayerController.Corner corner) {
    if (cornerMap.get(corner) == null && !cornerMap.containsValue(player)) {
      cornerMap.put(corner, player);
      player.setSetupBuffer(this);
      return true;
    }
    return false;
  }

  /**
   * Sets the corner of an already entered player to a new one. If the new one is occupied with
   * another snake it will switch both snakes' corners.
   * @param player The player whose corner will be moved.
   * @param corner The new corner.
   * @return Whether that player was present in the buffer. If it's not present then the function
   * will do nothing.
   */
  boolean setPlayerCorner(Player player, PlayerController.Corner corner) {
    if (cornerMap.containsValue(player)) {
      Player playerToBeMoved = cornerMap.get(corner);
      PlayerController.Corner previousCorner = player.getControlCorner();
      cornerMap.put(corner, player);
      cornerMap.put(previousCorner, playerToBeMoved);
      return true;
    }
    return false;
  }

  @Nullable
  PlayerController.Corner getPlayerCorner(Player player) {
    for (PlayerController.Corner corner : cornerMap.keySet())
      if (cornerMap.get(corner) != null && cornerMap.get(corner).equals(player))
        return corner;
    return null;
  }

  Player getPlayer(PlayerController.Corner corner) {
    return cornerMap.get(corner);
  }

  int getNumberOfPlayers() {
    int count = 0;
    for (PlayerController.Corner corner : cornerMap.keySet())
      if (cornerMap.get(corner) != null)
        count++;
    return count;
  }

  List<Player> getPlayers() {
    List<Player> players = new LinkedList<>();
    for (PlayerController.Corner corner : cornerMap.keySet())
      if (cornerMap.get(corner) != null)
        players.add(cornerMap.get(corner));
    return players;
  }

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
