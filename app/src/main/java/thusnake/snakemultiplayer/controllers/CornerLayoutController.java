package thusnake.snakemultiplayer.controllers;

import android.content.Context;
import android.view.MotionEvent;

import thusnake.snakemultiplayer.Game;
import thusnake.snakemultiplayer.OpenGLActivity;
import thusnake.snakemultiplayer.Player;
import thusnake.snakemultiplayer.R;
import thusnake.snakemultiplayer.Snake;

import static thusnake.snakemultiplayer.controllers.ControllerBuffer.Corner.LOWER_LEFT;
import static thusnake.snakemultiplayer.controllers.ControllerBuffer.Corner.LOWER_RIGHT;
import static thusnake.snakemultiplayer.controllers.ControllerBuffer.Corner.UPPER_LEFT;
import static thusnake.snakemultiplayer.controllers.ControllerBuffer.Corner.UPPER_RIGHT;

public class CornerLayoutController extends Controller {
  private final boolean mirrored;

  CornerLayoutController(Game game, Snake snake, boolean mirrored) {
    super(game, snake);

    // If the controller is configured as mirrored, we must check if that would be possible.
    this.mirrored = mirrored && horizontalMirrorPossible();
  }

  @Override
  public void setTexture(Context context) {
    drawableLayout.setTexture(R.drawable.androidcontrols);
  }

  @Override
  public void draw() {
    super.draw();

    if (mirrored) {
      // Calculate the mirrored position and draw it there too.
      gl.glPushMatrix();
      float offset;
      switch(snake.getControlCorner()) {
        case LOWER_LEFT:
        case UPPER_LEFT:
          offset = OpenGLActivity.current.getRenderer().getScreenWidth() - 10 - getWidth() - 10;
          break;
        case LOWER_RIGHT:
        case UPPER_RIGHT:
          offset = -OpenGLActivity.current.getRenderer().getScreenWidth() + 10 + getWidth() + 10;
          break;
        default:
          throw new RuntimeException(snake + "'s corner is null");
      }
      gl.glTranslatef(offset, 0, 0);
      super.draw();
      gl.glPopMatrix();
    }
  }

  private boolean horizontalMirrorPossible() {
    // Check if there isn't a player currently controlling a snake via the mirror corner.
    for (Player otherPlayer : game.getPlayers()) {
      if (otherPlayer.getControllerBuffer() instanceof CornerLayoutControllerBuffer) {
        ControllerBuffer.Corner mirrorCorner;
        switch (snake.getControlCorner()) {
          case LOWER_LEFT: mirrorCorner = LOWER_RIGHT;break;
          case LOWER_RIGHT: mirrorCorner = LOWER_LEFT;break;
          case UPPER_LEFT: mirrorCorner = UPPER_RIGHT;break;
          case UPPER_RIGHT: mirrorCorner = UPPER_LEFT;break;
          default: throw new RuntimeException(snake + "'s corner is null");
        }
        if (otherPlayer.getControlCorner().equals(mirrorCorner)) {
          // It's not possible and so we label it as such and return.
          return false;
        }
      }
    }

    return true;
  }

  @Override
  public void onMotionEvent(MotionEvent event) {
    float centerX = getX() + getWidth() / 2f, centerY = getY() + getWidth() / 2f;
    float inverseCenterX = OpenGLActivity.current.getRenderer().getScreenWidth() - centerX;
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
      case MotionEvent.ACTION_POINTER_DOWN:
      case MotionEvent.ACTION_MOVE:
        for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); pointerIndex++)
          if (!checkCoordinates(centerX, centerY, event.getX(pointerIndex),
              event.getY(pointerIndex)))
            if (mirrored)
              checkCoordinates(inverseCenterX, centerY, event.getX(pointerIndex),
                               event.getY(pointerIndex));
        break;
      default: break;
    }
  }

  @Override
  public boolean changeDirection(Snake.Direction direction) {
    boolean success = super.changeDirection(direction);
    if (success) vibrate(40);
    return success;
  }

  private boolean checkCoordinates(float originX, float originY, float x, float y) {
    boolean success;
    float halfWidth = getWidth() / 2f;

    // Check if the pointer is in the square layout.
    if (   x > originX - halfWidth && x < originX + halfWidth
        && y > originY - halfWidth && y < originY + halfWidth) {
      // Check which triangle the pointer is in and change the direction accordingly.
      if (Math.abs(y - originY) > Math.abs(x - originX)) {
        if (y - originY < 0) success = changeDirection(Snake.Direction.UP);
        else                 success = changeDirection(Snake.Direction.DOWN);
      } else {
        if (x - originX < 0) success = changeDirection(Snake.Direction.LEFT);
        else                 success = changeDirection(Snake.Direction.RIGHT);
      }

      return success;
    }
    return false;
  }


}
