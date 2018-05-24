package thusnake.snakemultiplayer;

import android.content.Context;
import android.view.MotionEvent;

public class CornerLayoutController extends PlayerController {
  private final GameRenderer renderer;
  private final Player player;
  
  public CornerLayoutController(GameRenderer renderer, Player player) {
    super(renderer, player);
    
    this.renderer = renderer;
    this.player = player;
  }

  @Override
  public void loadGLTexture(Context context) {
    super.loadGLTexture(context);
    this.getDrawableLayout().loadGLTexture(this.getGl(), context, R.drawable.androidcontrols);
  }

  @Override
  public void onMotionEvent(MotionEvent event) {
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
      case MotionEvent.ACTION_POINTER_DOWN:
        checkCoordinates(event.getX(), event.getY());
        break;
      case MotionEvent.ACTION_MOVE:
        for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); pointerIndex++)
          if (checkCoordinates(event.getX(pointerIndex), event.getY(pointerIndex))) break;
        break;
      default: break;
    }
  }

  private boolean checkCoordinates(float x, float y) {
    float originX = getX() + getWidth() / 2f;
    float originY = getY() + getWidth() / 2f;
    boolean success;

    // Check if the pointer is in the square layout.
    if (x > getX() && x < getX() + getWidth() && y > getY() && y < getY() + getWidth()) {
      // Check which triangle the pointer is in and change the direction accordingly.
      if (Math.abs(y - originY) > Math.abs(x - originX)) {
        if (y - originY < 0) success = player.changeDirection(Player.Direction.UP);
        else                 success =  player.changeDirection(Player.Direction.DOWN);
      } else {
        if (x - originX < 0) success = player.changeDirection(Player.Direction.LEFT);
        else                 success = player.changeDirection(Player.Direction.RIGHT);
      }

      // If there is a remote thread, send the information to it.
      if (success) this.setDirectionRemote();

      return success;
    }
    return false;
  }
}
