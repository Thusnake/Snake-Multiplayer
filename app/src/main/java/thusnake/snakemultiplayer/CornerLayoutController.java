package thusnake.snakemultiplayer;

import android.content.Context;
import android.view.MotionEvent;

import java.util.LinkedList;
import java.util.List;

public class CornerLayoutController extends PlayerController {
  private final Player player;
  private boolean horizontalMirror = false;
  
  public CornerLayoutController(GameRenderer renderer, Player player) {
    super(renderer, player);

    this.player = player;
  }

  @Override
  public void setTexture(Context context) {
    this.getDrawableLayout().setTexture(R.drawable.androidcontrols);
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

  @Override
  public List<MenuDrawable> optionsList(GameRenderer renderer) {
    List<MenuDrawable> list = new LinkedList<>();
    list.add(
        OptionsBuilder.addDescriptionItem(
            new MenuBooleanValue(renderer, horizontalMirror, renderer.getScreenWidth() - 10, 0,
                                 MenuDrawable.EdgePoint.TOP_RIGHT) {
              @Override
              public void move(double dt) {
                super.move(dt);
                setValue(horizontalMirror);
              }

              @Override
              public void onValueChange(boolean newValue) {
                super.onValueChange(newValue);
                horizontalMirror = newValue;
              }
            }, "Mirror"));

    return list;
  }

  @Override
  public String toString() { return "Virtual Controller"; }

  @Override
  public String identifier() { return "Corner"; }

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
