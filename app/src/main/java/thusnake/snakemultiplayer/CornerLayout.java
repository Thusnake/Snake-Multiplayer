package thusnake.snakemultiplayer;

import android.content.Context;
import android.os.Vibrator;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 05/01/2018.
 */

public class CornerLayout {
  public enum Corner {UPPER_LEFT, UPPER_RIGHT, LOWER_LEFT, LOWER_RIGHT}
  private final Square drawableLayout;
  private final float x, y, width;
  private final Player player;
  private final GL10 gl;

  public CornerLayout(Player player, Corner corner) {
    this.player = player;
    this.gl = player.getGame().getRenderer().getGl();
    float screenWidth = player.getGame().getRenderer().getScreenWidth();
    float screenHeight = player.getGame().getRenderer().getScreenHeight();
    this.width = screenHeight / 720f * 300f;
    switch(corner) {
      case UPPER_LEFT:
        this.x = 10;
        this.y = 10;
        break;
      case UPPER_RIGHT:
        this.x = screenWidth - 10 - this.width;
        this.y = 10;
        break;
      case LOWER_LEFT:
        this.x = 10;
        this.y = screenHeight - 10 - this.width;
        break;
      case LOWER_RIGHT:
        this.x = screenWidth - 10 - this.width;
        this.y = screenHeight - 10 - this.width;
        break;
      default:
        this.x = screenWidth / 2f - this.width / 2f;
        this.y = screenHeight / 2f - this.width / 2f;
        break;
    }
    this.drawableLayout = new Square(this.x, screenHeight - this.y - this.width,
        this.width, this.width);
    this.drawableLayout.loadGLTexture(this.gl, player.getGame().getRenderer().getContext(),
        R.drawable.androidcontrols);
  }

  public void draw() {
    this.drawableLayout.draw(this.gl);
  }

  public void draw(GL10 gl) {
    this.drawableLayout.draw(gl);
  }

  public boolean changeDirectionBasedOnCoordinates (float x, float y) {
    float originX = this.x + this.width / 2f;
    float originY = this.y + this.width / 2f;

    // Check if the pointer is in the square layout.
    if (x > this.x && x < this.x + this.width && y > this.y && y < this.y + this.width) {
      // Check which triangle the pointer is in and change the direction accordingly.
      if (Math.abs(y - originY) > Math.abs(x - originX)) {
        if (y - originY < 0) return player.changeDirection(Player.Direction.UP);
        else                 return player.changeDirection(Player.Direction.DOWN);
      } else {
        if (x - originX < 0) return player.changeDirection(Player.Direction.LEFT);
        else                 return player.changeDirection(Player.Direction.RIGHT);
      }
    }
    return false;
  }
}
