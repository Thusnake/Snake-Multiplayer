package thusnake.snakemultiplayer;

import com.android.texample.GLText;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 15/12/2017.
 */

// Holds a single menu item, which functions as a button.
public class MenuItem extends MenuDrawable {
  public enum Alignment {LEFT, RIGHT, CENTER}
  private String text;
  private final Alignment align;
  private double easeOutMultiplier, easeOutInertia;
  private GLText glText;
  private MenuValue value;
  private String description;
  private float desctiptionOpacity = 1;

  // Constructor.
  public MenuItem(GameRenderer renderer, String text, float x, float y, Alignment align) {
    super(renderer, x, y);
    this.text = text;
    this.align = align;

    this.glText = renderer.getGlText();
    this.setWidth(glText.getLength(text));
    this.setHeight(glText.getHeight() * 0.65f);

    this.easeOutMultiplier = 8;
    this.easeOutInertia = this.getHeight() * 2;

    if (align == Alignment.LEFT) this.setX(x);
    else if (align == Alignment.RIGHT) this.setX(x - this.getWidth());
    else this.setX(x - this.getWidth() / 2);
  }

  // Simply draws the text representation of the button. Has to be called inside a block of
  // GLText.
  @Override
  public void draw() {
    glText.end();
    glText.begin(this.getColors()[0],this.getColors()[1],this.getColors()[2],this.getColors()[3]);
    glText.draw(this.text, this.getX(), this.getY());
    if (this.description != null) {
      glText.end();
      gl.glPushMatrix();
      gl.glScalef(0.25f, 0.25f, 1f);
      glText.begin(0.66f, 0.66f, 0.66f, desctiptionOpacity);
      glText.draw(this.description, this.getX() * 4, this.getY() * 4);
      glText.end();
      gl.glPopMatrix();
      glText.begin();
    }
    if (this.value != null) this.value.draw();
  }

  public void move(double dt) {
    if (!this.getXTimer().isDone()) this.getXTimer().countEaseOut(dt, 8, this.getHeight() * 2);
    if (!this.getYTimer().isDone()) this.getYTimer().countEaseOut(dt, 8, this.getHeight() * 2);
    if (this.value != null) this.value.move(dt);
  }

  public void setColors(float r, float g, float b, float a) {
    this.setColors(r,g,b);
    this.setOpacity(a);
  }
  public void setColors(float r, float g, float b) {
    super.setColors(new float[] {r, g, b, this.getColors()[3]});
  }

  public void setText(String text) {
    if (this.align == Alignment.RIGHT)
      this.getXTimer().offsetTime(glText.getLength(this.text) - glText.getLength(text));
    else if (this.align == Alignment.CENTER)
      this.getXTimer().offsetTime((glText.getLength(this.text) - glText.getLength(text)) / 2f);
    this.setWidth(glText.getLength(text));
    this.text = text;
  }
  public void setDescription(String text) {
    // TODO maybe have a way for descriptions to also be right-aligned.
    this.description = text;
  }

  public void setDescriptionOpacity(float opacity) { this.desctiptionOpacity = opacity; }

  public void setValue(MenuValue value) { this.value = value; }

  // Returns true if the given coordinates are in the button.
  @Override
  public boolean isClicked(float x, float y) {
    return (super.isClicked(x,y) || this.value != null && this.value.isClicked(x, y));
  }

  public boolean isVisible() { return this.isDrawable() || this.text.equals(""); }
  public GameRenderer getRenderer() { return this.renderer; }
  public MenuValue getValue() { return this.value; }
  public String getText() { return this.text; }
  public String getDescription() { return this.description; }

  @Override
  public void setDestinationX(double destinationX) {
    if (this.align == Alignment.RIGHT)
      super.setDestinationX(destinationX - this.getWidth());
    else if (this.align == Alignment.CENTER)
      super.setDestinationX(destinationX - this.getWidth() / 2);
    else
      super.setDestinationX(destinationX);
  }

  @Override
  public void setDestinationXFromOrigin(double offsetX) {
    if (this.align == Alignment.RIGHT)
      super.setDestinationXFromOrigin(offsetX - this.getWidth());
    else if (this.align == Alignment.CENTER)
      super.setDestinationXFromOrigin(offsetX - this.getWidth() / 2);
    else
      super.setDestinationXFromOrigin(offsetX);
  }

  @Override
  public void setDestinationToInitial() {
    if (this.align == Alignment.RIGHT)
      this.getXTimer().setEndTimeFromNow(this.getInitialX() - this.getWidth());
    else if (this.align == Alignment.CENTER)
      this.getXTimer().setEndTimeFromNow(this.getInitialX() - this.getWidth() / 2);
    else
      this.getXTimer().setEndTimeFromNow(this.getInitialX());

    this.getYTimer().setEndTimeFromNow(this.getInitialY());
  }

  public void setEaseOutVariables(double multiplier, double inertia) {
    this.easeOutMultiplier = multiplier;
    this.easeOutInertia = inertia;
  }
}
