package thusnake.snakemultiplayer.controllers;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedList;
import java.util.List;

import thusnake.snakemultiplayer.Game;
import thusnake.snakemultiplayer.GameRenderer;
import thusnake.snakemultiplayer.GameSetupBuffer;
import thusnake.snakemultiplayer.MenuBooleanValue;
import thusnake.snakemultiplayer.MenuDrawable;
import thusnake.snakemultiplayer.OpenGLActivity;
import thusnake.snakemultiplayer.OptionsBuilder;
import thusnake.snakemultiplayer.Player;
import thusnake.snakemultiplayer.Snake;

public class CornerLayoutControllerBuffer extends ControllerBuffer {
  private final Player player;
  private boolean horizontalMirror = false;
  private Boolean horizontalMirrorPossible = null;
  
  public CornerLayoutControllerBuffer(Player player) {
    super(player);
    this.player = player;
  }

  @Override
  public List<MenuDrawable> optionsList() {
    GameRenderer renderer = OpenGLActivity.current.getRenderer();
    List<MenuDrawable> list = new LinkedList<>();
    list.add(
        OptionsBuilder.addDescriptionItem(
            new MenuBooleanValue(renderer, horizontalMirror, renderer.getScreenWidth() - 10, 0,
                                 MenuDrawable.EdgePoint.TOP_RIGHT) {
              @Override
              public void move(double dt) {
                super.move(dt);
                setValue(horizontalMirror);
              }

              @Override
              public void onValueChange(boolean newValue) {
                super.onValueChange(newValue);
                horizontalMirror = newValue;
              }
            }, "Mirror"));

    return list;
  }

  @Override
  public void saveSettings(SharedPreferences.Editor settings, GameSetupBuffer setupBuffer) {
    String playerPrefix = setupBuffer.getPlayerSavingPrefix(player);
    settings.putBoolean(playerPrefix + "-cornerlayout-mirror", horizontalMirror);
  }

  @Override
  public ControllerBuffer loadSettings(Context context, GameSetupBuffer setupBuffer) {
    SharedPreferences settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    String playerPrefix = setupBuffer.getPlayerSavingPrefix(player);
    horizontalMirror = settings.getBoolean(playerPrefix + "-cornerlayout-mirror", horizontalMirror);
    return this;
  }

  @Override
  public String toString() { return "Virtual Controller"; }

  @Override
  public String identifier() { return "Corner"; }

  @Override
  public Controller constructController(Game game, Snake snake) {
    return new CornerLayoutController(game, snake, horizontalMirror);
  }
}
