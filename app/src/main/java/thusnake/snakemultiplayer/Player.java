package thusnake.snakemultiplayer;

import android.content.Context;

import java.util.LinkedList;
import java.util.List;

import thusnake.snakemultiplayer.controllers.ControllerBuffer;
import thusnake.snakemultiplayer.controllers.CornerLayoutControllerBuffer;
import thusnake.snakemultiplayer.controllers.GamepadControllerBuffer;
import thusnake.snakemultiplayer.controllers.SwipeControllerBuffer;

/**
 * Created by Nick on 12/12/2017.
 */

public class Player{
  private GameRenderer renderer;
  private String name = "Snake";
  private ControllerBuffer controllerBuffer;
  private SnakeSkin skin = SnakeSkin.white;
  private final List<ControllerBuffer> controllersCache = new LinkedList<>();

  public Player(GameRenderer renderer) {
    this.renderer = renderer;
    this.setSkin(SnakeSkin.white);
  }

  public Player defaultPreset(GameSetupBuffer setupBuffer) {
    // Find a suitable unique name of the format "Player N".
    int playerIndex = 1;
    boolean indexChanged = true;
    while (indexChanged) {
      indexChanged = false;
      for (ControllerBuffer.Corner corner : ControllerBuffer.Corner.values())
        if (setupBuffer.getCornerMap().getPlayer(corner) != null &&
            setupBuffer.getCornerMap().getPlayer(corner).getName().equals("Player "+ playerIndex)) {
          playerIndex++;
          indexChanged = true;
        }
    }
    setName("Player " + playerIndex);

    // Set the default controller.
    setController(new CornerLayoutControllerBuffer(this));

    return this;
  }

  /**
   * Loads the last settings saved for this player's controller and assigns that controller.
   * To be called before loading any controllers as it caches a bunch of them.
   * @param setupBuffer The buffer to use for loading.
   */
  public void loadSavedController(GameSetupBuffer setupBuffer) {
    // Cache the default controllers.
    setController(new SwipeControllerBuffer(this).loadSettings(renderer.getOriginActivity(), setupBuffer));
    setController(new GamepadControllerBuffer(this).loadSettings(renderer.getOriginActivity(), setupBuffer));
    setController(new CornerLayoutControllerBuffer(this).loadSettings(renderer.getOriginActivity(), setupBuffer));

    // Select the one that has been saved to the shared preferences last.
    for (ControllerBuffer controller : controllersCache)
      if (controller.identifier().equals(renderer.getOriginActivity()
          .getSharedPreferences("settings", Context.MODE_PRIVATE)
          .getString(setupBuffer.savingPrefix+"-"+getControlCorner().toString()+"-controllerid",
                     null))) {
        controller.loadSettings(renderer.getOriginActivity(), setupBuffer);
        setController(controller);
      }
  }

  public String getName() { return this.name; }
  public ControllerBuffer getControllerBuffer() { return this.controllerBuffer; }
  public ControllerBuffer.Corner getControlCorner() {
    return renderer.getMenu().getSetupBuffer().getCornerMap().getPlayerCorner(this);
  }
  public ControllerBuffer.Corner getControlCorner(GameSetupBuffer setupBuffer) {
    return setupBuffer.getCornerMap().getPlayerCorner(this);
  }

  public void setSkin(SnakeSkin skin) { this.skin = skin; }
  public SnakeSkin getSkin() { return skin; }
  public int getSkinIndex() { return SnakeSkin.allSkins.indexOf(skin); }

  /**
   * Sets the controller of this player to a controller of a given type.
   * @param controller An example controller of the wanted type. If a controller of this type
   *                   exists already in the cache then the cached controller will be used.
   *                   Otherwise the passed controller will be assigned to that player.
   */
  public void setController(ControllerBuffer controller) {
    for (ControllerBuffer cachedController : controllersCache)
      if (cachedController.getClass().equals(controller.getClass())) {
        controllerBuffer = cachedController;
        return;
      }

    controllerBuffer = controller;
    controllersCache.add(controller);
  }

  /**
   * Sets the player's controller to a passed ControllerBuffer. It is recommended that you instead
   * use setController() as this method will not check the cache and will therefore constantly
   * create new controllers with default settings if implemented in the menu as an option.
   * @param controller The controller to be used.
   */
  public void setControllerForced(ControllerBuffer controller) {
    controllerBuffer = controller;
  }

  public void setName(String name) { this.name = name; }
}
