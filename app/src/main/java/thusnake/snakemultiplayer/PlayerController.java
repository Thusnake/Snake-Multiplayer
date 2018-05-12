package thusnake.snakemultiplayer;

import android.content.Context;
import android.os.Vibrator;
import android.view.MotionEvent;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 05/01/2018.
 */

public abstract class PlayerController {
  public enum Corner {UPPER_LEFT, UPPER_RIGHT, LOWER_LEFT, LOWER_RIGHT}
  private final Corner corner;
  private Square drawableLayout;
  private final float x, y, width;
  private final Player player;
  private final GameRenderer renderer;
  private final GL10 gl;
  private boolean textureLoaded = false;
  private ConnectedThread inputTargetThread;

  public PlayerController(GameRenderer renderer, Player player) {
    this.player = player;
    this.gl = renderer.getGl();
    this.renderer = renderer;
    this.width = renderer.getScreenHeight() / 720f * 300f;
    this.corner = player.getControlCorner();
    switch(corner) {
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

    this.drawableLayout = new Square(x, renderer.getScreenHeight() - y - width, width, width);
  }

  public void loadGLTexture(Context context) {
    this.textureLoaded = true;
  }

  public boolean textureIsLoaded() { return this.textureLoaded; }

  public void draw() { this.draw(gl); }
  public void draw(GL10 gl) { if (textureLoaded) this.drawableLayout.draw(gl); }

  public abstract void onMotionEvent(MotionEvent event);

  public Corner getCorner() { return this.corner; }
  public Square getDrawableLayout() { return drawableLayout; }
  public GL10 getGl() { return this.gl; }
  public float getX() { return x; }
  public float getY() { return y; }
  public float getWidth() { return this.width; }

  public void setInputTargetThread(ConnectedThread inputTargetThread) {
    this.inputTargetThread = inputTargetThread;
  }

  public ConnectedThread getInputTargetThread() { return inputTargetThread; }
}
