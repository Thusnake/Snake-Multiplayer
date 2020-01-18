package thusnake.snakemultiplayer.controllers;

import android.content.Context;
import android.view.MotionEvent;

import thusnake.snakemultiplayer.Game;
import thusnake.snakemultiplayer.Snake;

public class SwipeController extends Controller {
  private final int sensitivity;
  private boolean holding;

  SwipeController(Game game, Snake snake, int sensitivity) {
    super(game, snake);
    this.sensitivity = sensitivity;
  }

  @Override
  public void onMotionEvent(MotionEvent event) {
    float originX, originY;
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        holding = true;
        originX = event.getX();
        originY = event.getY();
        break;
      case MotionEvent.ACTION_MOVE:
        if (event.getHistorySize() > 0) {
          float deltaX = event.getX() - event.getHistoricalX(0);
          float deltaY = event.getY() - event.getHistoricalY(0);

          boolean success = false;
          // Check if the pointer has moved quick enough.
          if (Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2)) > 20)
            // Find the direction.
            if (Math.abs(deltaX) > Math.abs(deltaY)) {
              if (deltaX > 0)
                success = changeDirection(Snake.Direction.RIGHT);
              else
                success = changeDirection(Snake.Direction.LEFT);
            } else {
              if (deltaY > 0)
                success = changeDirection(Snake.Direction.DOWN);
              else
                success = changeDirection(Snake.Direction.UP);
            }

          // Set the information to the remote thread if there is one.
          if (success) this.setDirectionRemote();
        }

        break;
      case MotionEvent.ACTION_UP:
        holding = false;
        break;
    }
  }

  @Override
  public boolean changeDirection(Snake.Direction direction) {
    boolean success = super.changeDirection(direction);
    if (success) vibrate(60);
    return success;
  }

  @Override
  public void draw() {}

  @Override
  public void setTexture(Context context) {}
}
