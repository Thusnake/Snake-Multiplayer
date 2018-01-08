package thusnake.snakemultiplayer;

import com.android.texample.GLText;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 15/12/2017.
 */

// Holds a single menu item, which functions as a button.
public class MenuItem {
  private final String text;
  private float width, height, initialX, initialY;
  private SimpleTimer x, y;
  private int value;
  private float[] colors = new float[4];
  private GameRenderer renderer;
  private GL10 gl;
  private GLText glText;
  private boolean visible;
  private MenuAction action;

  // Constructor.
  public MenuItem(GameRenderer renderer, String text, float x, float y) {
    this.text = text;
    this.x = new SimpleTimer(x);
    this.y = new SimpleTimer(y);
    this.initialX = x;
    this.initialY = y;

    this.renderer = renderer;
    this.gl = renderer.getGl();
    this.glText = renderer.getGlText();
    this.width = glText.getLength(text);
    this.height = glText.getHeight() * 0.65f;

    for (int i = 0; i < 4; i++) this.colors[i] = 1f;
  }

  // Simply draws the text representation of the button. Has to be called inside a block of
  // GLText.
  public void draw() {
    gl.glColor4f(this.colors[0], this.colors[1], this.colors[2], this.colors[3]);
    glText.draw(this.text, (float) this.x.getTime(), (float) this.y.getTime());
  }

  public void move(double dt) {
    if (!this.x.isDone()) this.x.countEaseOut(dt, 8, this.height * 2);
    if (!this.y.isDone()) this.y.countEaseOut(dt, 8, this.height * 2);
  }

  public void setColors(float r, float g, float b, float a) {
    this.colors[0] = r;
    this.colors[1] = g;
    this.colors[2] = b;
    this.colors[3] = a;
  }

  public void setAction(MenuAction action) {
    this.action = action;
  }

  public void performAction() {
    this.action.perform(this.renderer);
  }

  // Returns true if the given coordinates are in the button.
  public boolean isClicked(float x, float y) {
    return (x > this.x.getTime() && x < this.x.getTime() + this.width
        && renderer.getScreenHeight() - y > this.y.getTime()
        && renderer.getScreenHeight() - y < this.y.getTime() + this.height);
  }

  public boolean isVisible() {
    return !this.visible || this.text.equals("");
  }
  public float getX() { return (float) this.x.getTime(); }
  public float getY() { return (float) this.y.getTime(); }
  public float getWidth() { return this.width; }
  public float getHeight() { return this.height; }
  public void setDestinationX(double destinationX) { this.x.setEndTimeFromNow(destinationX); }
  public void setDestinationY(double destinationY) { this.y.setEndTimeFromNow(destinationY); }
  public void setDestinationXFromOrigin(double offsetX) {
    this.x.setEndTimeFromNow(this.initialX + offsetX);
  }
  public void setDestinationYFromOrigin(double offsetY) {
    this.y.setEndTimeFromNow(this.initialY + offsetY);
  }
}


// TODO Try to create different subclasses to get move functionality out of menu items.

