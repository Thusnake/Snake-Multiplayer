package thusnake.snakemultiplayer;

import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import thusnake.snakemultiplayer.controllers.ControllerBuffer;

public class CornerMap {
  private final Map<ControllerBuffer.Corner, Player> map = new HashMap<>();
  
  CornerMap() {
    map.put(ControllerBuffer.Corner.LOWER_LEFT, null);
    map.put(ControllerBuffer.Corner.UPPER_LEFT, null);
    map.put(ControllerBuffer.Corner.UPPER_RIGHT, null);
    map.put(ControllerBuffer.Corner.LOWER_RIGHT, null);
  }

  /**
   * Adds a player to the corner map.
   * @param player The player to be added.
   * @param corner The control corner to be occupied by that player.
   * @return Whether the player was successfully added. This will return false only if the corner
   * had been previously occupied or the player is already in the game.
   */
  public boolean addPlayer(Player player, ControllerBuffer.Corner corner) {
    if (map.get(corner) == null && !map.containsValue(player)) {
      map.put(corner, player);
      return true;
    }
    return false;
  }

  /**
   * Removes a player from the corner map. Its corner is emptied.
   * @param player The player to be removed.
   */
  void removePlayer(Player player) {
    if (player != null && map.containsValue(player))
      for (ControllerBuffer.Corner corner : map.keySet())
        if (player.equals(map.get(corner)))
          map.put(corner, null);
  }

  /**
   * Empties a corner from the corner map, removing the snake in it.
   * @param corner The corner to be emptied.
   */
  public void emptyCorner(ControllerBuffer.Corner corner) {
    map.put(corner, null);
  }

  /**
   * Sets the corner of an already entered player to a new one. If the new one is occupied with
   * another snake it will switch both snakes' corners.
   * @param player The player whose corner will be moved.
   * @param corner The new corner.
   * @return Whether that player was present in the buffer. If it's not present then the function
   * will do nothing.
   */
  boolean setPlayerCorner(Player player, ControllerBuffer.Corner corner) {
    if (map.containsValue(player)) {
      Player playerToBeMoved = map.get(corner);
      ControllerBuffer.Corner previousCorner = player.getControlCorner();
      map.put(corner, player);
      map.put(previousCorner, playerToBeMoved);
      return true;
    }
    return false;
  }

  /** Simply swaps the contents of two given corners.*/
  void swapCorners(ControllerBuffer.Corner corner1, ControllerBuffer.Corner corner2) {
    Player playerToBeMoved = map.get(corner1);
    map.put(corner1, map.get(corner2));
    map.put(corner2, playerToBeMoved);
  }

  /** Returns the corner of a given player. Will return null if player is absent in the map. */
  @Nullable
  ControllerBuffer.Corner getPlayerCorner(Player player) {
    for (ControllerBuffer.Corner corner : map.keySet())
      if (map.get(corner) != null && map.get(corner).equals(player))
        return corner;
    return null;
  }

  /** Returns the player occupying a given corner or null if that corner is empty */
  public Player getPlayer(ControllerBuffer.Corner corner) {
    return map.get(corner);
  }

  /** Returns the number of players currently occupying corners. Value range is 0 - 4. */
  int getNumberOfPlayers() {
    int count = 0;
    for (ControllerBuffer.Corner corner : map.keySet())
      if (map.get(corner) != null)
        count++;
    return count;
  }

  /** Returns all the players currently occupying corners. */
  List<Player> getPlayers() {
    List<Player> players = new LinkedList<>();
    for (ControllerBuffer.Corner corner : map.keySet())
      if (map.get(corner) != null)
        players.add(map.get(corner));
    return players;
  }
}
