package thusnake.snakemultiplayer.controllers;

import android.content.Context;
import android.view.MotionEvent;

import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import thusnake.snakemultiplayer.Game;
import thusnake.snakemultiplayer.GameRenderer;
import thusnake.snakemultiplayer.MenuDrawable;
import thusnake.snakemultiplayer.MenuImage;
import thusnake.snakemultiplayer.OpenGLActivity;
import thusnake.snakemultiplayer.Snake;

/**
 * Handles the inputs for a particular snake in a particular game.
 * Also responsible for drawing a layout on screen if necessary.
 */
public abstract class Controller {

  public final GameRenderer renderer;
  public final GL10 gl;
  private float x, y, width;
  public final MenuImage drawableLayout;
  final Game game;
  final Snake snake;

  public Controller(Game game, Snake snake) {
    this.game = game;
    this.snake = snake;

    renderer = OpenGLActivity.current.getRenderer();
    gl = renderer.getGl();
    width = renderer.getScreenHeight() / 720f * 300f;

    switch (snake.getControlCorner()) {
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
      default: throw new RuntimeException("Snake " + snake + " is not part of " + game
                                          + "'s corner map.");
    }
    this.drawableLayout
        = new MenuImage(renderer, x, renderer.getScreenHeight() - y, width, width,
        MenuDrawable.EdgePoint.TOP_LEFT);

    setTexture(renderer.getOriginActivity());
  }

  /** Draws the controller's layout to the screen */
  public void draw() { drawableLayout.draw(); }

  public abstract void setTexture(Context context);
  public void reloadTexture() { drawableLayout.reloadTexture(); }

  /** Handles motion events for controlling the snake. */
  public abstract void onMotionEvent(MotionEvent event);

  public GL10 getGl() { return this.gl; }
  public float getX() { return x; }
  public float getY() { return y; }
  public float getWidth() { return this.width; }

  public void setDirectionRemote() {

  }

  public boolean changeDirection(Snake.Direction direction) {
    return snake.changeDirection(direction);
  }

  public void vibrate(long milliseconds) {
    renderer.getOriginActivity().vibrator.vibrate(milliseconds);
  }
}
