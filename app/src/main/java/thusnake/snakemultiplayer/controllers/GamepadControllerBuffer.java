package thusnake.snakemultiplayer.controllers;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.InputDevice;

import java.util.LinkedList;
import java.util.List;

import thusnake.snakemultiplayer.Game;
import thusnake.snakemultiplayer.GameRenderer;
import thusnake.snakemultiplayer.GameSetupBuffer;
import thusnake.snakemultiplayer.GamepadManager;
import thusnake.snakemultiplayer.MenuDrawable;
import thusnake.snakemultiplayer.MenuItem;
import thusnake.snakemultiplayer.MenuListOfItems;
import thusnake.snakemultiplayer.OpenGLActivity;
import thusnake.snakemultiplayer.Player;
import thusnake.snakemultiplayer.Snake;

public class GamepadControllerBuffer extends ControllerBuffer {
  public GamepadControllerBuffer(Player player) {
    super(player);
  }

  @Override
  public String toString() { return "Gamepad"; }

  @Override
  public String identifier() { return "Gamepad"; }

  @Override
  public List<MenuDrawable> optionsList() {
    GameRenderer renderer = OpenGLActivity.current.getRenderer();
    List<MenuDrawable> list = new LinkedList<>();
    list.add(
        new MenuListOfItems(renderer, renderer.smallDistance(), 0, MenuDrawable.EdgePoint.TOP_LEFT) {
          @Override
          public void move(double dt) {
            this.clearAll();
            for (int controllerId : GamepadManager.getInstance().getGamepadIds()) {
              MenuItem item = new MenuItem(renderer, InputDevice.getDevice(controllerId).getName(), 0, 0, EdgePoint.TOP_LEFT) {
                @Override
                public float[] getColors() {
                  float[] colors = super.getColors();
                  if (GamepadManager.getInstance().getAssociatedPlayer(controllerId) != player)
                    colors[3] /= 2;
                  return colors;
                }
              };
              item.setAction((action, origin) -> pairWithGamepad(controllerId));
              this.addItem(item);
            }
            super.move(dt);
          }
        }
    );
    return list;
  }

  @Override
  public void saveSettings(SharedPreferences.Editor settings, GameSetupBuffer setupBuffer) {}

  @Override
  public ControllerBuffer loadSettings(Context context, GameSetupBuffer setupBuffer) {
    return this;
  }

  @Override
  public Controller constructController(Game game, Snake snake) {
    return new GamepadController(game, snake);
  }

  private void pairWithGamepad(int deviceId) {
    GamepadManager.getInstance().registerPlayerWithGamepad(player, deviceId);
  }
}
