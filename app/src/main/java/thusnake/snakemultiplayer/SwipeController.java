package thusnake.snakemultiplayer;

import android.content.Context;
import android.view.MotionEvent;

import javax.microedition.khronos.opengles.GL10;

public class SwipeController extends PlayerController {
  private final Player player;
  private boolean holding;
  private float originX, originY;

  public SwipeController(GameRenderer renderer, Player player) {
    super(renderer, player);
    this.player = player;
  }

  @Override
  public void onMotionEvent(MotionEvent event) {
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
                success = player.changeDirection(Player.Direction.RIGHT);
              else
                success = player.changeDirection(Player.Direction.LEFT);
            } else {
              if (deltaY > 0)
                success = player.changeDirection(Player.Direction.DOWN);
              else
                success = player.changeDirection(Player.Direction.UP);
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
  public void draw(GL10 gl) {}

  @Override
  public void setTexture(Context context) {}
}
