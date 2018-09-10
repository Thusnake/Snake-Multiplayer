package thusnake.snakemultiplayer;

import android.content.Context;
import android.view.MotionEvent;

import java.util.List;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 05/01/2018.
 */

public abstract class PlayerController {
  public enum Corner {LOWER_LEFT, UPPER_LEFT, UPPER_RIGHT, LOWER_RIGHT}
  final GameRenderer renderer;
  private MenuImage drawableLayout;
  private float x, y, width;
  final Player player;
  final GL10 gl;

  public PlayerController(GameRenderer renderer, Player player) {
    this.renderer = renderer;
    this.player = player;
    this.gl = renderer.getGl();
    this.width = renderer.getScreenHeight() / 720f * 300f;
  }

  public void prepareForGame() {
    switch(player.getControlCorner()) {
      case UPPER_LEFT:
        this.x = 10;
        this.y = 10;
        break;
      case UPPER_RIGHT:
        this.x = renderer.getScreenWidth() - 10 - this.width;
        this.y = 10;
        break;
      case LOWER_LEFT:
        this.x = 10;
        this.y = renderer.getScreenHeight() - 10 - this.width;
        break;
      case LOWER_RIGHT:
        this.x = renderer.getScreenWidth() - 10 - this.width;
        this.y = renderer.getScreenHeight() - 10 - this.width;
        break;
      default:
        this.x = renderer.getScreenWidth() / 2f - this.width / 2f;
        this.y = renderer.getScreenHeight() / 2f - this.width / 2f;
        break;
    }

    this.drawableLayout
        = new MenuImage(renderer, x, renderer.getScreenHeight() - y, width, width,
        MenuDrawable.EdgePoint.TOP_LEFT);

    setTexture(renderer.getOriginActivity());
  }

  public abstract void setTexture(Context context);
  public void reloadTexture() { drawableLayout.reloadTexture(); }

  public void draw() { drawableLayout.draw(); }

  public abstract void onMotionEvent(MotionEvent event);

  public abstract List<MenuDrawable> optionsList(GameRenderer renderer);

  public MenuImage getDrawableLayout() { return drawableLayout; }
  public GL10 getGl() { return this.gl; }
  public float getX() { return x; }
  public float getY() { return y; }
  public float getWidth() { return this.width; }

  public void setDirectionRemote() {

  }

  public boolean changeDirection(Player.Direction direction) {
    return player.changeDirection(direction);
  }

  public void vibrate(long milliseconds) {
    renderer.getOriginActivity().vibrator.vibrate(milliseconds);
  }

  public abstract String toString();

  public abstract String identifier();
}
