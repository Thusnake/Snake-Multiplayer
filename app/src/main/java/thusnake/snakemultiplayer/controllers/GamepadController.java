package thusnake.snakemultiplayer.controllers;

import android.content.Context;
import android.view.KeyEvent;
import android.view.MotionEvent;

import thusnake.snakemultiplayer.Game;
import thusnake.snakemultiplayer.R;
import thusnake.snakemultiplayer.Snake;
import thusnake.snakemultiplayer.Snake.Direction;

public class GamepadController extends Controller {
  static final double DEADZONE = 0.15;

  GamepadController(Game game, Snake snake) {
    super(game, snake);
  }

  @Override
  public void setTexture(Context context) {
    drawableLayout.setTexture(R.drawable.gamepad);
  }

  @Override
  public void onMotionEvent(MotionEvent event) {
    double axisX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
    double axisY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);

    if (Math.sqrt(axisX * axisX + axisY * axisY) > DEADZONE) {
      if ((this.snake.getDirection() == Direction.LEFT || this.snake.getDirection() == Direction.RIGHT)
          && Math.abs(axisX) <= Math.abs(axisY)) {
        if (axisY > 0) changeDirection(Direction.DOWN);
        else changeDirection(Direction.UP);
      } else if ((this.snake.getDirection() == Direction.UP || this.snake.getDirection() == Direction.DOWN)
          && Math.abs(axisY) <= Math.abs(axisX)) {
        if (axisX > 0) changeDirection(Direction.RIGHT);
        else changeDirection(Direction.LEFT);
      }
    }
  }

  public void onKeyEvent(KeyEvent event) {

  }
}
