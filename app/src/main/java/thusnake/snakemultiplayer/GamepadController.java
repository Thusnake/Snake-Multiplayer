package thusnake.snakemultiplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.MotionEvent;

import java.util.List;

public class GamepadController extends PlayerController {
  public GamepadController(GameRenderer renderer, Player player) {
    super(renderer, player);
  }

  @Override
  public void setTexture(Context context) {
    this.getDrawableLayout().setTexture(R.drawable.gamepad);
  }

  @Override
  public void onMotionEvent(MotionEvent event) {

  }

  @Override
  public String toString() { return "Gamepad"; }

  @Override
  public String identifier() { return "Gamepad"; }

  @Override
  public List<MenuDrawable> optionsList(GameRenderer renderer) {
    return null;
  }

  @Override
  public void saveSettings(SharedPreferences.Editor settings, GameSetupBuffer setupBuffer) {

  }

  @Override
  public PlayerController loadSettings(Context context, GameSetupBuffer setupBuffer) {
    return this;
  }
}
