package thusnake.snakemultiplayer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 23/01/2018.
 */

public abstract class MenuDrawable {
  private SimpleTimer x, y;
  private float width, height, initialX, initialY;
  private float[] colors = {1f, 1f, 1f, 1f};
  private MenuAction action;
  public final GameRenderer renderer;
  public final GL10 gl;
  private boolean drawableOutsideOfScreen = true;

  public MenuDrawable(GameRenderer renderer, float x, float y) {
    this.renderer = renderer;
    this.gl = renderer.getGl();
    this.x = new SimpleTimer(x);
    this.y = new SimpleTimer(y);
    this.initialX = x;
    this.initialY = y;
  }

  // The abstract methods.
  public abstract void draw();
  public abstract void move(double dt);

  // The non-abstract methods.
  public boolean isClicked(float x, float y) {
    return (x > this.x.getTime() && x < this.x.getTime() + this.width
        && renderer.getScreenHeight() - y > this.y.getTime()
        && renderer.getScreenHeight() - y < this.y.getTime() + this.height);
  }

  public void setAction(MenuAction action) {
    this.action = action;
  }
  public void performAction() { if (this.action != null) this.action.perform(this.renderer, this); }

  public void setColors(float[] rgba) {
    if (rgba.length == 4)
      for (int index = 0; index < 4; index++)
        this.colors[index] = rgba[index];
  }

  public float[] getColors() { return this.colors; }

  public void setOpacity(float opacity) {
    colors[3] = opacity;
  }

  public void setX(double x) { this.x.setTime(x); }
  public void setY(double y) { this.y.setTime(y); }
  public float getX() { return (float) this.x.getTime(); }
  public float getY() { return (float) this.y.getTime(); }
  public SimpleTimer getXTimer() { return this.x; }
  public SimpleTimer getYTimer() { return this.y; }
  public float getInitialX() { return this.initialX; }
  public float getInitialY() { return this.initialY; }
  public void setWidth(float width) { this.width = width; }
  public void setHeight(float height) { this.height = height; }
  public float getWidth() { return this.width; }
  public float getHeight() { return this.height; }

  public void setDestinationX(double destinationX) { this.x.setEndTimeFromNow(destinationX); }
  public void setDestinationY(double destinationY) { this.y.setEndTimeFromNow(destinationY); }
  public void setDestinationToInitial() {
    this.x.setEndTimeFromNow(initialX);
    this.y.setEndTimeFromNow(initialY);
  }
  public void setDestinationXFromOrigin(double offsetX) {
    this.x.setEndTimeFromNow(this.initialX + offsetX);
  }
  public void setDestinationYFromOrigin(double offsetY) {
    this.y.setEndTimeFromNow(this.initialY + offsetY);
  }

  public void setDrawableOutsideOfScreen(boolean drawableOutsideOfScreen) {
    this.drawableOutsideOfScreen = drawableOutsideOfScreen;
  }
  public boolean isDrawableOutsideOfScreen() { return this.drawableOutsideOfScreen;}

  public int getScreenNumber() { return (int) (this.getX() / renderer.getScreenWidth());}
}
