package thusnake.snakemultiplayer.gamemodes;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import thusnake.snakemultiplayer.CornerMap;
import thusnake.snakemultiplayer.Entity;
import thusnake.snakemultiplayer.Game;
import thusnake.snakemultiplayer.GameSetupBuffer;
import thusnake.snakemultiplayer.IDGenerator;
import thusnake.snakemultiplayer.MenuDrawable;
import thusnake.snakemultiplayer.Protocol;

public abstract class GameMode {
  public enum Difficulty {
    MILD(0), FAIR(1), TOUGH(2), BONKERS(3), ULTIMATE(4);

    public final int index;
    Difficulty(int index) {
      this.index = index;
    }
    static Difficulty getFromIndex(int index) {
      for (Difficulty difficulty : Difficulty.values())
        if (difficulty.index == index)
          return difficulty;
      return null;
    }
  }

  public final AtomicInteger horizontalSquares = new AtomicInteger(20),
                             verticalSquares = new AtomicInteger(20),
                             speed = new AtomicInteger(8);
  public final AtomicBoolean stageBorders = new AtomicBoolean(true);
  private Difficulty difficulty = Difficulty.MILD;

  /**
   * @return A game that follows this GameMode's configurations and rules.
   */
  public abstract Game generateGame(CornerMap cornerMap);

  /**
   * @return A list of MenuDrawables to be used as a list of options for this game mode.
   */
  public abstract List<MenuDrawable> menuOptions();

  /**
   * @return The Android resource ID of this GameMode's thumbnail.
   */
  public abstract int getThumbnailResourceID();

  /**
   * Changes the settings of this game mode to match a desired difficulty.
   * Does not have to be overridden.
   */
  public void setDifficulty(Difficulty difficulty) {
    this.difficulty = difficulty;
  }

  public Difficulty getDifficulty() { return difficulty; }

  /**
   * Saves the settings for this particular game mode (in a particular GameSetupBuffer).
   * To be called after SharedPreferences.Editor.edit() and before a commit().
   * @param settings The editor received from the SharedPreferences.Editor.edit() call.
   * @param setupBuffer The setup buffer from which the save is being called.
   */
  public abstract void saveSettings(SharedPreferences.Editor settings, GameSetupBuffer setupBuffer);

  public abstract void loadSettings(GameSetupBuffer setupBuffer);

  /**
   * @return A list of calls that would synchronize the guests with the current menu configurations.
   */
  public final List<byte[]> getSyncCallList() {
    List<byte[]> calls = new LinkedList<>();
    calls.add(new byte[] {Protocol.HOR_SQUARES_CHANGED, (byte) horizontalSquares.get()});
    calls.add(new byte[] {Protocol.VER_SQUARES_CHANGED, (byte) verticalSquares.get()});
    calls.add(new byte[] {Protocol.SPEED_CHANGED, (byte) speed.get()});
    calls.add(new byte[] {Protocol.STAGE_BORDERS_CHANGED, (byte) (stageBorders.get() ? 1 : 0)});
    calls.add(new byte[] {Protocol.GAME_MODE, (byte) IDGenerator.getGameModeID(getClass())});
    calls.addAll(gameModeSpecificSyncCalls());
    return calls;
  }

  public abstract List<byte[]> gameModeSpecificSyncCalls();

  @Override
  public abstract String toString();

  /** Returns a prefix for saving in a particular GameSetupBuffer */
  String getSavingPrefix(GameSetupBuffer setupBuffer) {
    return setupBuffer.getSavingPrefix() + "-" + toString() + "-";
  }
}
