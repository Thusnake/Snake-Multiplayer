package thusnake.snakemultiplayer;

import com.android.texample.GLText;

/**
 * Created by Nick on 15/12/2017.
 */

// Holds a single menu item, which functions as a button.
public class MenuItem extends MenuDrawable {
  public enum Alignment {LEFT, RIGHT, CENTER}
  private String text;
  private double easeOutMultiplier, easeOutInertia;
  private GLText glText;
  private MenuValue value;
  private String description;
  private float descriptionOpacity = 1;

  // Constructor.
  public MenuItem(GameRenderer renderer, String text, float x, float y, EdgePoint alignPoint,
                  EdgePoint originPoint) {
    super(renderer, x, y, alignPoint, originPoint);
    this.text = text;

    this.glText = renderer.getGlText();
    this.setWidth(glText.getLength(text));
    this.setHeight(glText.getHeight() * 0.65f);

    this.easeOutMultiplier = 8;
    this.easeOutInertia = 1/4.0;
  }

  public MenuItem(GameRenderer renderer, String text, float x, float y, EdgePoint alignPoint) {
    this(renderer, text, x, y, alignPoint, EdgePoint.CENTER);
  }

  // Simply draws the text representation of the button. Has to be called inside a block of
  // GLText.
  @Override
  public void draw() {
    glText.begin(this.getColors()[0],this.getColors()[1],this.getColors()[2],this.getColors()[3]);
    glText.draw(this.text, this.getLeftX(), this.getBottomY());
    glText.end();
    if (this.description != null) {
      gl.glPushMatrix();
      gl.glScalef(0.25f, 0.25f, 1f);
      glText.begin(0.66f, 0.66f, 0.66f, descriptionOpacity);
      glText.draw(this.description, this.getLeftX() * 4, this.getBottomY() * 4);
      glText.end();
      gl.glPopMatrix();
    }
    if (this.value != null) this.value.draw();
  }

  public void move(double dt) {
    super.move(dt);
    if (!this.getXTimer().isDone()) this.getXTimer().countEaseOut(dt, easeOutMultiplier, this.getXTimer().getDuration() * easeOutInertia);
    if (!this.getYTimer().isDone()) this.getYTimer().countEaseOut(dt, easeOutMultiplier, this.getYTimer().getDuration() * easeOutInertia);
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
    this.setWidth(glText.getLength(text));
    this.text = text;
  }
  public void setDescription(String text) {
    // TODO maybe have a way for descriptions to also be right-aligned.
    this.description = text;
  }

  public void setDescriptionOpacity(float opacity) { this.descriptionOpacity = opacity; }

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
    super.setDestinationX(destinationX);
  }

  @Override
  public void setDestinationXFromOrigin(double offsetX) {
    super.setDestinationXFromOrigin(offsetX);
  }

  @Override
  public void setDestinationToInitial() {
    this.getXTimer().setEndTimeFromNow(this.getInitialX());
    this.getYTimer().setEndTimeFromNow(this.getInitialY());
  }

  public void setEaseOutVariables(double multiplier, double inertia) {
    this.easeOutMultiplier = multiplier;
    this.easeOutInertia = inertia;
  }
}
