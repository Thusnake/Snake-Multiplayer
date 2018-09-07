package thusnake.snakemultiplayer;

import com.android.texample.GLText;

/**
 * Created by Nick on 15/12/2017.
 */

// Holds a single menu item, which functions as a button.
public class MenuItem extends MenuDrawable {
  private String text;
  private double easeOutMultiplier, easeOutInertia;
  private GLText glText;
  private String description;
  private float descriptionOpacity = 1;

  // Constructor.
  public MenuItem(GameRenderer renderer, String text, float x, float y, EdgePoint alignPoint,
                  EdgePoint originPoint) {
    super(renderer, x, y, alignPoint, originPoint);
    this.text = text;

    this.glText = renderer.getGlText();
    this.setWidth(glText.getLength(text));
    this.setHeight(glText.getHeight());

    this.easeOutMultiplier = 8;
    this.easeOutInertia = 1/4.0;
  }

  public MenuItem(GameRenderer renderer, String text, float x, float y, EdgePoint alignPoint) {
    this(renderer, text, x, y, alignPoint, alignPoint);
  }

  @Override
  public void draw(float[] parentColors) {
    if (isDrawable()) {
      gl.glPushMatrix();

      gl.glTranslatef(getX(originPoint), getY(originPoint), 0);
      gl.glScalef((float) scale.getTime(), (float) scale.getTime(), 1);
      gl.glTranslatef(-getX(originPoint), -getY(originPoint), 0);

      glText.begin();
      glColor4array(gl, combineColorArrays(getColors(), parentColors));
      glText.draw(this.text, this.getLeftX(), this.getBottomY());
      glText.end();
      if (this.description != null) {
        gl.glPushMatrix();
        gl.glScalef(0.25f, 0.25f, 1f);
        glText.begin(0.66f, 0.66f, 0.66f, descriptionOpacity);
        glText.draw(description, getLeftX() * 4, getBottomY() * 4 - glText.getHeight() * 0.7f);
        glText.end();
        gl.glPopMatrix();
      }

      gl.glPopMatrix();
    }
  }

  public void move(double dt) {
    super.move(dt);
    if (!getXTimer().isDone())
      getXTimer().countEaseOut(dt, easeOutMultiplier, getXTimer().getDuration() * easeOutInertia);
    if (!getYTimer().isDone())
      getYTimer().countEaseOut(dt, easeOutMultiplier, getYTimer().getDuration() * easeOutInertia);
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
    this.description = text;
  }

  public void setDescriptionOpacity(float opacity) { this.descriptionOpacity = opacity; }

  public boolean isVisible() { return this.isDrawable() || this.text.equals(""); }
  public GameRenderer getRenderer() { return this.renderer; }
  public String getText() { return this.text; }
  public String getDescription() { return this.description; }

  @Override
  public float getHeight() {
    return description == null ? super.getHeight() : super.getHeight() + glText.getHeight() / 4;
  }

  public void setEaseOutVariables(double multiplier, double inertia) {
    this.easeOutMultiplier = multiplier;
    this.easeOutInertia = inertia;
  }
}
