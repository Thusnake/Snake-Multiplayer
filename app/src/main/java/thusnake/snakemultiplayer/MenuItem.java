package thusnake.snakemultiplayer;

import com.android.texample.GLText;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 15/12/2017.
 */

// Holds a single menu item, which functions as a button.
public class MenuItem implements MenuButton {
  public enum Alignment {LEFT, RIGHT, CENTER}
  private String text;
  private final Alignment align;
  private float width, height, initialX, initialY;
  private SimpleTimer x, y;
  private double easeOutMultiplier, easeOutInertia;
  private float[] colors = new float[4];
  private GameRenderer renderer;
  private GL10 gl;
  private GLText glText;
  private boolean visible;
  private MenuAction action;
  private MenuValue value;
  private String description;

  // Constructor.
  public MenuItem(GameRenderer renderer, String text, float x, float y, Alignment align) {
    this.text = text;
    this.align = align;
    this.initialX = x;
    this.initialY = y;

    this.renderer = renderer;
    this.gl = renderer.getGl();
    this.glText = renderer.getGlText();
    this.width = glText.getLength(text);
    this.height = glText.getHeight() * 0.65f;
    this.easeOutMultiplier = 8;
    this.easeOutInertia = this.height * 2;
    if (align == Alignment.LEFT) this.x = new SimpleTimer(x);
    else this.x = new SimpleTimer(x - this.width);
    this.y = new SimpleTimer(y);

    for (int i = 0; i < 4; i++) this.colors[i] = 1f;
  }

  // Simply draws the text representation of the button. Has to be called inside a block of
  // GLText.
  public void draw() {
    glText.end();
    glText.begin(this.colors[0], this.colors[1], this.colors[2], this.colors[3]);
    glText.draw(this.text, (float) this.x.getTime(), (float) this.y.getTime());
    if (this.description != null) {
      glText.end();
      gl.glPushMatrix();
      gl.glScalef(0.25f, 0.25f, 1f);
      glText.begin(0.66f, 0.66f, 0.66f, 1f);
      glText.draw(this.description, (float) this.x.getTime() * 4,
          (float) this.y.getTime() * 4);
      glText.end();
      gl.glPopMatrix();
      glText.begin();
    }
    if (this.value != null) this.value.draw();
  }

  public void move(double dt) {
    if (!this.x.isDone()) this.x.countEaseOut(dt, 8, this.height * 2);
    if (!this.y.isDone()) this.y.countEaseOut(dt, 8, this.height * 2);
    if (this.value != null) this.value.move(dt);
  }

  public void setColors(float r, float g, float b, float a) {
    this.setColors(r,g,b);
    this.setOpacity(a);
  }
  public void setColors(float r, float g, float b) {
    this.colors[0] = r;
    this.colors[1] = g;
    this.colors[2] = b;
  }
  public void setOpacity(float a) {
    this.colors[3] = a;
  }

  public void setText(String text) {
    if (this.align == Alignment.RIGHT)
      this.x.countDown(glText.getLength(text) - glText.getLength(this.text));
    this.width = glText.getLength(text);
    this.text = text;
  }
  public void setDescription(String text) {
    // TODO maybe have a way for descriptions to also be right-aligned.
    this.description = text;
  }

  public void setAction(MenuAction action) {
    this.action = action;
  }
  public void setValue(MenuValue value) { this.value = value; }

  public void performAction() { if (this.action != null) this.action.perform(this.renderer, this); }

  // Returns true if the given coordinates are in the button.
  public boolean isClicked(float x, float y) {
    return (x > this.x.getTime() && x < this.x.getTime() + this.width
        && renderer.getScreenHeight() - y > this.y.getTime()
        && renderer.getScreenHeight() - y < this.y.getTime() + this.height
        || this.value != null && this.value.isClicked(x, y));
  }

  public boolean isVisible() {
    return !this.visible || this.text.equals("");
  }
  public float getX() { return (float) this.x.getTime(); }
  public float getY() { return (float) this.y.getTime(); }
  public float getWidth() { return this.width; }
  public float getHeight() { return this.height; }
  public GameRenderer getRenderer() { return this.renderer; }
  public MenuValue getValue() { return this.value; }

  public void setX(double x) { this.x.setTime(x); }
  public void setY(double y) { this.y.setTime(y); }
  public void setDestinationX(double destinationX) {
    if (this.align == Alignment.RIGHT) this.x.setEndTimeFromNow(destinationX - this.width);
    else this.x.setEndTimeFromNow(destinationX);
  }
  public void setDestinationY(double destinationY) { this.y.setEndTimeFromNow(destinationY); }
  public void setDestinationXFromOrigin(double offsetX) {
    this.x.setEndTimeFromNow(this.initialX + offsetX);
  }
  public void setDestinationYFromOrigin(double offsetY) {
    this.y.setEndTimeFromNow(this.initialY + offsetY);
  }
  public void setEaseOutVariables(double multiplier, double inertia) {
    this.easeOutMultiplier = multiplier;
    this.easeOutInertia = inertia;
  }
}


// TODO Try to create different subclasses to get move functionality out of menu items.

