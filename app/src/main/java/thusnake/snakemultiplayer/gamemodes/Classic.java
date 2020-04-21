package thusnake.snakemultiplayer.gamemodes;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedList;
import java.util.List;

import thusnake.snakemultiplayer.Apple;
import thusnake.snakemultiplayer.CornerMap;
import thusnake.snakemultiplayer.Entity;
import thusnake.snakemultiplayer.Game;
import thusnake.snakemultiplayer.GameSetupBuffer;
import thusnake.snakemultiplayer.MenuDrawable;
import thusnake.snakemultiplayer.OpenGLActivity;
import thusnake.snakemultiplayer.OptionsBuilder;
import thusnake.snakemultiplayer.R;

public class Classic extends GameMode {
  @Override
  public Game generateGame(CornerMap cornerMap) {
    List<Class<? extends Entity>> entities = new LinkedList<>();
    entities.add(Apple.class);
    return new Game(cornerMap, horizontalSquares.get(), verticalSquares.get(), speed.get(),
                         stageBorders.get(), entities);
  }

  @Override
  public List<MenuDrawable> menuOptions() {
    return OptionsBuilder.justDifficulty();
  }

  @Override
  public void setDifficulty(Difficulty difficulty) {
    super.setDifficulty(difficulty);
    horizontalSquares.set(10 + difficulty.index * 5);
    verticalSquares.set(10 + difficulty.index * 5);
    speed.set(6 + difficulty.index * 3);
  }

  @Override
  public int getThumbnailResourceID() { return R.drawable.gamemode_classic; }

  @Override
  public void saveSettings(SharedPreferences.Editor settings, GameSetupBuffer setupBuffer) {
    String prefix = getSavingPrefix(setupBuffer);

    settings.putInt(prefix + "difficulty", getDifficulty().index);
  }

  @Override
  public void loadSettings(GameSetupBuffer setupBuffer) {
    String prefix = getSavingPrefix(setupBuffer);
    SharedPreferences settings = OpenGLActivity.current.getSharedPreferences("settings",
                                                                             Context.MODE_PRIVATE);

    setDifficulty(Difficulty.getFromIndex(settings.getInt(prefix + "difficulty",getDifficulty().index)));
  }

  @Override
  public List<byte[]> gameModeSpecificSyncCalls() {
    return null;
  }

  @Override
  public String toString() { return "classic"; }
}
