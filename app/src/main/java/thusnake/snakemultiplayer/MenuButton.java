package thusnake.snakemultiplayer;

import android.view.MotionEvent;
import java.util.LinkedHashSet;

/**
 * A wrapper for other MenuDrawables with the functionality of a button.
 */
public abstract class MenuButton extends MenuDrawable implements TextureReloadable {
  private GameRenderer renderer;
  private MenuItem.Alignment alignment;
  private SimpleTimer scaleTimer = new SimpleTimer(1.0);
  private boolean isHeld = false;
  private SimpleTimer holdDuration = new SimpleTimer(0.0, 1.0);
  private MenuImage backgroundImage;
  final LinkedHashSet<MenuDrawable> drawables = new LinkedHashSet<>();

  public MenuButton(GameRenderer renderer, float x, float y, float width, float height,
                    MenuItem.Alignment alignment) {
    super(renderer, x, y);

    this.renderer = renderer;
    this.alignment = alignment;
    setWidth(width);
    setHeight(height);

    if (alignment.equals(MenuItem.Alignment.LEFT))        setX(x);
    else if (alignment.equals(MenuItem.Alignment.CENTER)) setX(x - width/2f);
    else                                                  setX(x - width);

    onButtonCreated();
  }

  /**
   * Called just before constructor returns.
   */
  public void onButtonCreated() {}

  public void draw() {
    gl.glPushMatrix();
    gl.glScalef((float) scaleTimer.getTime(), (float) scaleTimer.getTime(), 1);
    gl.glTranslatef((getX() + getWidth()/2f) * (1/(float) scaleTimer.getTime() - 1),
                    (getY() + getHeight()/2f) * (1/(float) scaleTimer.getTime() - 1),
                    0);
    gl.glColor4f(getColors()[0], getColors()[1], getColors()[2], getColors()[3]);

    // Draw the background image if there is one.
    if (backgroundImage != null)
      backgroundImage.draw();

    // Draw the rest of the elements.
    drawInside();

    gl.glPopMatrix();
  }

  /**
   * Specifies what should be drawn inside of the button.
   */
  public void drawInside() {
    for (MenuDrawable drawable : drawables)
      drawable.draw();
  }

  public void move(double dt) {
    if (!scaleTimer.isDone()) {
      if (isHeld) scaleTimer.countEaseOut(dt, 2, 120 * dt);
      else        scaleTimer.countEaseOut(dt, 8, 5 * dt);
    }

    if (isHeld) if (holdDuration.count(dt)) onHeld();

    for (MenuDrawable drawable : drawables)
      drawable.move(dt);
  }

  /**
   * Is called whenever the button has been held for more than a second.
   */
  public void onHeld() {}

  public void onMotionEvent(MotionEvent event) {
    float x = event.getX() - renderer.getMenu().getScreenTransformX();
    float y = event.getY() - renderer.getMenu().getScreenTransformY();

    switch(event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        if (isClicked(x, y)) {
          isHeld = true;
          scaleTimer.setEndTimeFromNow(1 - event.getPressure()/3f);
        }

        break;

      case MotionEvent.ACTION_MOVE:
        if (isHeld) {
          if (isClicked(x, y))
            scaleTimer.setEndTimeFromNow(1 - event.getPressure()/3f);
          else {
            isHeld = false;
            scaleTimer.setEndTimeFromNow(1);
          }
        }

        break;

      case MotionEvent.ACTION_UP:
        if (isClicked(x, y) && isHeld) {
          isHeld = false;
          scaleTimer.setEndTimeFromNow(1);
          performAction();
        }

        break;

      default:
        break;
    }
  }

  /**
   * Sets a custom background image.
   * @param id The Android resource identifier for the background image.
   * @return A reference to this MenuButton, for convenience.
   */
  public MenuButton withBackgroundImage(int id) {
    backgroundImage = new MenuImage(renderer, getX(), getY(), getWidth(), getHeight());
    backgroundImage.setTexture(id);
    return this;
  }

  @Override
  public void reloadTexture() {
    for (MenuDrawable drawable : drawables)
      if (drawable instanceof TextureReloadable)
        ((TextureReloadable) drawable).reloadTexture();

    if (backgroundImage != null) backgroundImage.reloadTexture();
  }

  public double getScale() { return scaleTimer.getTime(); }
}
