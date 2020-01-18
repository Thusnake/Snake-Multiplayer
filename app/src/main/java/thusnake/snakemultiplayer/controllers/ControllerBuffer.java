package thusnake.snakemultiplayer.controllers;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedList;
import java.util.List;

import thusnake.snakemultiplayer.Game;
import thusnake.snakemultiplayer.GameSetupBuffer;
import thusnake.snakemultiplayer.MenuDrawable;
import thusnake.snakemultiplayer.Player;
import thusnake.snakemultiplayer.Snake;

/**
 * Created by Nick on 05/01/2018.
 */

/**
 * Configures a \Controller and creates one for use in-game.
 */
public abstract class ControllerBuffer {
  public enum Corner {LOWER_LEFT, UPPER_LEFT, UPPER_RIGHT, LOWER_RIGHT}
  final Player player;

  public ControllerBuffer(Player player) {
    this.player = player;
  }

  public abstract String toString();

  public abstract String identifier();

  /**
   * @return A list of MenuDrawables, which will be used in creating the options menu when this
   * ControllerBuffer is selected.
   */
  public abstract List<MenuDrawable> optionsList();

  /**
   * Saves the settings for this particular controller.
   * To be called after SharedPreferences.Editor.edit() and before a commit().
   * @param settings The editor received from the SharedPreferences.Editor.edit() call.
   * @param setupBuffer The setup buffer from which the save is being called.
   */
  public abstract void saveSettings(SharedPreferences.Editor settings, GameSetupBuffer setupBuffer);

  public abstract ControllerBuffer loadSettings(Context context, GameSetupBuffer setupBuffer);

  /**
   * @return A Controller to be used in a game for a specific snake.
   */
  public abstract Controller constructController(Game game, Snake snake);


  public static List<ControllerBuffer> getControllerChoiceList(Player player) {
    List<ControllerBuffer> choices = new LinkedList<>();
    choices.add(new CornerLayoutControllerBuffer(player));
    choices.add(new SwipeControllerBuffer(player));
    choices.add(new GamepadControllerBuffer(player));
    return choices;
  }
}
