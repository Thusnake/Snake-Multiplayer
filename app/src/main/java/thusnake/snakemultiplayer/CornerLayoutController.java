package thusnake.snakemultiplayer;

import android.content.Context;
import android.view.MotionEvent;

import java.util.LinkedList;
import java.util.List;

public class CornerLayoutController extends PlayerController {
  private final Player player;
  private boolean horizontalMirror = false;
  private Boolean horizontalMirrorPossible = null;
  
  public CornerLayoutController(GameRenderer renderer, Player player) {
    super(renderer, player);

    this.player = player;
  }

  @Override
  public void setTexture(Context context) {
    this.getDrawableLayout().setTexture(R.drawable.androidcontrols);
  }

  @Override
  public void draw() {
    super.draw();
    if (horizontalMirrorPossible == null && horizontalMirror) {
      // Check if there isn't a player currently controlling a snake via the mirror corner.
      for (Player otherPlayer : player.getGame().getPlayers()) {
        if (otherPlayer == player) continue;
        if (otherPlayer.getPlayerController() instanceof CornerLayoutController) {
          Corner mirrorCorner;
          switch (player.getControlCorner()) {
            case LOWER_LEFT:
              mirrorCorner = Corner.LOWER_RIGHT;
              break;
            case LOWER_RIGHT:
              mirrorCorner = Corner.LOWER_LEFT;
              break;
            case UPPER_LEFT:
              mirrorCorner = Corner.UPPER_RIGHT;
              break;
            case UPPER_RIGHT:
              mirrorCorner = Corner.UPPER_LEFT;
              break;
            default:
              throw new RuntimeException(player + "'s corner is null");
          }
          if (otherPlayer.getControlCorner().equals(player.getControlCorner())
              || otherPlayer.getControlCorner().equals(mirrorCorner)) {
            // It's not possible and so we label it as such and return.
            horizontalMirrorPossible = false;
            return;
          }
        }
      }

      // Mirroring is evidently possible so we label it as such.
      horizontalMirrorPossible = true;
    }

    if (horizontalMirrorPossible != null && horizontalMirrorPossible) {
      // Calculate the mirrored position and draw it there.
      gl.glPushMatrix();
      float offset;
      switch(player.getControlCorner()) {
        case LOWER_LEFT:
        case UPPER_LEFT:
          offset = player.getGame().getRenderer().getScreenWidth() - 10 - getWidth() - 10; break;
        case LOWER_RIGHT:
        case UPPER_RIGHT:
          offset = -player.getGame().getRenderer().getScreenWidth() + 10 + getWidth() + 10; break;
        default:
          throw new RuntimeException(player + "'s corner is null");
      }
      gl.glTranslatef(offset, 0, 0);
      super.draw();
      gl.glPopMatrix();
    }
  }

  @Override
  public void onMotionEvent(MotionEvent event) {
    float centerX = getX() + getWidth() / 2f, centerY = getY() + getWidth() / 2f;
    float inverseCenterX = player.getGame().getRenderer().getScreenWidth() - centerX;
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
      case MotionEvent.ACTION_POINTER_DOWN:
        checkCoordinates(centerX, centerY, event.getX(), event.getY());
        if (horizontalMirrorPossible != null && horizontalMirrorPossible)
          checkCoordinates(inverseCenterX, centerY, event.getX(), event.getY());
        break;
      case MotionEvent.ACTION_MOVE:
        for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); pointerIndex++)
          if (!checkCoordinates(centerX, centerY, event.getX(pointerIndex),
                                                  event.getY(pointerIndex)))
            if (horizontalMirrorPossible != null && horizontalMirrorPossible)
              checkCoordinates(inverseCenterX, centerY, event.getX(), event.getY());
        break;
      default: break;
    }
  }

  @Override
  public boolean changeDirection(Player.Direction direction) {
    boolean success = super.changeDirection(direction);
    if (success) vibrate(40);
    return success;
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

  private boolean checkCoordinates(float originX, float originY, float x, float y) {
    boolean success;
    float halfWidth = getWidth() / 2f;

    // Check if the pointer is in the square layout.
    if (   x > originX - halfWidth && x < originX + halfWidth
        && y > originY - halfWidth && y < originY + halfWidth) {
      // Check which triangle the pointer is in and change the direction accordingly.
      if (Math.abs(y - originY) > Math.abs(x - originX)) {
        if (y - originY < 0) success = changeDirection(Player.Direction.UP);
        else                 success = changeDirection(Player.Direction.DOWN);
      } else {
        if (x - originX < 0) success = changeDirection(Player.Direction.LEFT);
        else                 success = changeDirection(Player.Direction.RIGHT);
      }

      return success;
    }
    return false;
  }
}
