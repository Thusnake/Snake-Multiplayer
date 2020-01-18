package thusnake.snakemultiplayer.controllers;

import android.content.Context;
import android.view.MotionEvent;

import thusnake.snakemultiplayer.Game;
import thusnake.snakemultiplayer.R;
import thusnake.snakemultiplayer.Snake;

public class GamepadController extends Controller {
  GamepadController(Game game, Snake snake) {
    super(game, snake);
  }

  @Override
  public void setTexture(Context context) {
    drawableLayout.setTexture(R.drawable.gamepad);
  }

  @Override
  public void onMotionEvent(MotionEvent event) {
    // Stub!
  }
}
