package thusnake.snakemultiplayer.controllers;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import thusnake.snakemultiplayer.Game;
import thusnake.snakemultiplayer.GameRenderer;
import thusnake.snakemultiplayer.GameSetupBuffer;
import thusnake.snakemultiplayer.MenuDrawable;
import thusnake.snakemultiplayer.MenuNumericalValue;
import thusnake.snakemultiplayer.OpenGLActivity;
import thusnake.snakemultiplayer.OptionsBuilder;
import thusnake.snakemultiplayer.Player;
import thusnake.snakemultiplayer.Snake;

public class SwipeControllerBuffer extends ControllerBuffer {
  private final Player player;
  private AtomicInteger sensitivity = new AtomicInteger(10);

  public SwipeControllerBuffer(Player player) {
    super(player);
    this.player = player;
  }

  @Override
  public String toString() { return "Swipe"; }

  @Override
  public String identifier() { return "Swipe"; }

  @Override
  public List<MenuDrawable> optionsList() {
    GameRenderer renderer = OpenGLActivity.current.getRenderer();
    List<MenuDrawable> list = new LinkedList<>();
    list.add(
        OptionsBuilder.addDescriptionItem(
            new MenuNumericalValue(renderer, sensitivity, renderer.getScreenWidth() - 10,
                                   0, MenuDrawable.EdgePoint.TOP_RIGHT), "Sensitivity"));
    return list;
  }

  @Override
  public void saveSettings(SharedPreferences.Editor settings, GameSetupBuffer setupBuffer) {
    String playerPrefix = setupBuffer.getPlayerSavingPrefix(player);
    settings.putInt(playerPrefix + "-swipe-sensitivity", sensitivity.get());
  }

  @Override
  public ControllerBuffer loadSettings(Context context, GameSetupBuffer setupBuffer) {
    SharedPreferences settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    String playerPrefix = setupBuffer.getPlayerSavingPrefix(player);
    sensitivity.set(settings.getInt(playerPrefix + "-swipe-sensitivity", sensitivity.get()));
    return this;
  }

  @Override
  public Controller constructController(Game game, Snake snake) {
    return new SwipeController(game, snake, sensitivity.get());
  }
}
