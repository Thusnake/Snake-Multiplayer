package thusnake.snakemultiplayer.controllers;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

import thusnake.snakemultiplayer.Game;
import thusnake.snakemultiplayer.GameSetupBuffer;
import thusnake.snakemultiplayer.MenuDrawable;
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
    return null;
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
}
