package thusnake.snakemultiplayer;

import android.util.Log;
import android.util.SparseArray;
import android.view.InputDevice;

import java.util.LinkedList;
import java.util.List;

/**
 * Handles everything to do with physical gamepad controllers.
 * Will automatically check for devices and list them.
 * Devices can be associated with a player.
 */
public class GamepadManager {
  private static final String TAG = "GamepadManager";
  private static final GamepadManager ourInstance = new GamepadManager();

  public static GamepadManager getInstance() {
    return ourInstance;
  }

  private SparseArray<Player> gamepadToPlayerMap = new SparseArray<>();
  private List<Integer> gamepadIds = new LinkedList<>();

  private GamepadManager() {
    Thread gamepadScannerThread = new Thread(() -> {
      while(true) {
        List<Integer> disconnectedControllers = new LinkedList<>(gamepadIds);
        for (int id : InputDevice.getDeviceIds()) {
          InputDevice dev = InputDevice.getDevice(id);
          if (dev == null) continue;
          int sources = dev.getSources();

          // Verify that the device has gamepad buttons, control sticks, or both.
          if (((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
              || ((sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)) {
            // This device is a game controller. Store its device ID.
            if (!gamepadIds.contains(id)) {
              gamepadIds.add(id);
            } else {
              disconnectedControllers.remove(Integer.valueOf(id));
            }
          }
        }

        for (int id : disconnectedControllers) {
          gamepadIds.remove(Integer.valueOf(id));
          onControllerDisconnected(id);
        }

        try { Thread.sleep(500); }
        catch(InterruptedException e) { Log.d(TAG, "Scanner thread sleep interrupted."); }
      }
    });
    gamepadScannerThread.setDaemon(true);
    gamepadScannerThread.start();
  }

  private void onControllerDisconnected(int controllerId) {
    gamepadToPlayerMap.delete(controllerId);
  }

  /**
   * @return A list of the IDs of every connected gamepad to the device.
   */
  public List<Integer> getGamepadIds() { return gamepadIds; }

  /**
   * Register an association between a gamepad and a Player instance.
   *
   * The gamepad must be identified by a valid ID from the list of gamepad IDs.
   * @param player Player instance
   * @param gamepadId Identifier for the gamepad
   */
  public void registerPlayerWithGamepad(Player player, int gamepadId) {
    if (gamepadIds.contains(gamepadId)) {
      gamepadToPlayerMap.put(gamepadId, player);
    } else {
      Log.e(TAG, String.format("Tried to register a player with an unknown gamepad ID (%d).", gamepadId));
    }
  }

  /**
   * @param gamepadID Identifier for the gamepad.
   * @return The player registered with this gamepad. Returns null if no player is associated
   * with this gamepad.
   */
  public Player getAssociatedPlayer(int gamepadID) {
    return gamepadToPlayerMap.get(gamepadID);
  }
}
