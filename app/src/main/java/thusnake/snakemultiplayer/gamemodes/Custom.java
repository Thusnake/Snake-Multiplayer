package thusnake.snakemultiplayer.gamemodes;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import thusnake.snakemultiplayer.Apple;
import thusnake.snakemultiplayer.CornerMap;
import thusnake.snakemultiplayer.Entity;
import thusnake.snakemultiplayer.Game;
import thusnake.snakemultiplayer.GameRenderer;
import thusnake.snakemultiplayer.GameSetupBuffer;
import thusnake.snakemultiplayer.IDGenerator;
import thusnake.snakemultiplayer.MenuDrawable;
import thusnake.snakemultiplayer.MenuNumericalValue;
import thusnake.snakemultiplayer.OpenGLActivity;
import thusnake.snakemultiplayer.OptionsBuilder;
import thusnake.snakemultiplayer.Protocol;
import thusnake.snakemultiplayer.R;

public class Custom extends GameMode {
  protected final AtomicInteger numberOfApples = new AtomicInteger(1);

  @Override
  public Game generateGame(CornerMap cornerMap) {
    List<Class<? extends Entity>> entities = new LinkedList<>();
    for (int n = 0; n < numberOfApples.get(); n++) entities.add(Apple.class);
    return new Game(cornerMap, horizontalSquares.get(), verticalSquares.get(), speed.get(),
                         stageBorders.get(), entities);
  }

  @Override
  public List<MenuDrawable> menuOptions() {
    GameRenderer renderer = OpenGLActivity.current.getRenderer();
    List<MenuDrawable> options = OptionsBuilder.defaultOptions(this);

    MenuNumericalValue numberOfApplesValue = new MenuNumericalValue(renderer, numberOfApples,
        renderer.getScreenWidth() - renderer.smallDistance(), 0,
        MenuDrawable.EdgePoint.TOP_RIGHT) {
      @Override
      public void move(double dt) {
        super.move(dt);
        setValueBoundaries(0, horizontalSquares.get() * verticalSquares.get() - 1);
        setValue(getValue());
      }
    };
    OptionsBuilder.addDescriptionItem(numberOfApplesValue, "Apples");

    options.add(numberOfApplesValue);
    return options;
  }

  @Override
  public int getThumbnailResourceID() { return R.drawable.gamemode_custom; }

  @Override
  public void saveSettings(SharedPreferences.Editor settings, GameSetupBuffer setupBuffer) {
    String prefix = getSavingPrefix(setupBuffer);

    settings.putInt(prefix + "horsquares", horizontalSquares.get());
    settings.putInt(prefix + "versquares", verticalSquares.get());
    settings.putInt(prefix + "speed", speed.get());
    settings.putBoolean(prefix + "stageborders", stageBorders.get());
    settings.putInt(prefix + "apples", numberOfApples.get());
  }

  @Override
  public void loadSettings(GameSetupBuffer setupBuffer) {
    String prefix = getSavingPrefix(setupBuffer);
    SharedPreferences settings = OpenGLActivity.current.getSharedPreferences("settings",
                                                                             Context.MODE_PRIVATE);
    horizontalSquares.set(settings.getInt(prefix + "horsquares", horizontalSquares.get()));
    verticalSquares.set(settings.getInt(prefix + "versquares", verticalSquares.get()));
    speed.set(settings.getInt(prefix + "speed", speed.get()));
    stageBorders.set(settings.getBoolean(prefix + "stageborders", stageBorders.get()));
    numberOfApples.set(settings.getInt(prefix + "apples", numberOfApples.get()));
  }

  @Override
  public List<byte[]> gameModeSpecificSyncCalls() {
    List<byte[]> calls = new LinkedList<>();
    calls.add(new byte[] {Protocol.NUMBER_OF_APPLES_CHANGED, (byte) numberOfApples.get()});
    return calls;
  }

  @Override
  public String toString() { return "custom"; }
}
