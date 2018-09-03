package thusnake.snakemultiplayer;

import android.view.MotionEvent;
import java.util.LinkedHashSet;

/**
 * A wrapper for other MenuDrawables with the functionality of a button.
 */
public abstract class MenuButton extends MenuContainer implements TextureReloadable {
  private boolean isHeld = false;
  private SimpleTimer holdDuration = new SimpleTimer(0.0);
  private MenuImage backgroundImage;

  public MenuButton(GameRenderer renderer, float x, float y, float width, float height,
                    EdgePoint alignPoint) {
    super(renderer, x, y, alignPoint, EdgePoint.CENTER);
    setWidth(width);
    setHeight(height);

    onButtonCreated();
  }

  /**
   * Called just before constructor returns.
   */
  public void onButtonCreated() {}

  public void draw(float[] parentColors) {
    if (isDrawable()) {
      gl.glPushMatrix();
      gl.glTranslatef(getX(originPoint), getY(originPoint), 0);
      gl.glScalef((float) scale.getTime(), (float) scale.getTime(), 0); // Scale it.
      gl.glTranslatef(-getX(originPoint), -getY(originPoint), 0);

      super.draw(parentColors);

      gl.glPopMatrix();
    }
  }

  public void move(double dt) {
    if (!getXTimer().isDone()) getXTimer().countEaseOut(dt, 8, getXTimer().getDuration() / 4.0);
    if (!getYTimer().isDone()) getYTimer().countEaseOut(dt, 8, getYTimer().getDuration() / 4.0);

    if (!scale.isDone()) {
      if (isHeld) scale.countEaseOut(dt, 2, 120 * dt);
      else        scale.countEaseOut(dt, 8, 5 * dt);
    }

    if (isHeld) {
      holdDuration.countUp(dt);
      if (getHoldDuration() - dt < 0.5 && getHoldDuration() >= 0.5)
        onHeld();
    }

    super.move(dt);
  }

  /**
   * Is called whenever the button has been held for more than a second.
   */
  public void onHeld() {}

  public double getHoldDuration() { return holdDuration.getTime(); }

  @Override
  public void onMotionEvent(MotionEvent event, float[] pointerX, float[] pointerY) {
    if (isEnabled()) {
      float x = pointerX[0];
      float y = pointerY[0];

      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
          if (isClicked(x, y)) {
            isHeld = true;
            scale.setEndTimeFromNow(1 + event.getPressure() / 6f);
          }

          break;

        case MotionEvent.ACTION_MOVE:
          if (isHeld) {
            if (isClicked(x, y))
              scale.setEndTimeFromNow(1 + event.getPressure() / 6f);
            else {
              isHeld = false;
              scale.setEndTimeFromNow(1);
              holdDuration.reset();
            }
          }

          break;

        case MotionEvent.ACTION_UP:
          if (isHeld) {
            isHeld = false;
            scale.setEndTimeFromNow(1);
            holdDuration.reset();
            if (isClicked(x, y))
              performAction();
          }

          break;

        default:
          break;
      }
    }
  }

  /**
   * Sets a custom background image.
   * @param id The Android resource identifier for the background image.
   * @return A reference to this MenuButton, for convenience.
   */
  public MenuButton withBackgroundImage(int id) {
    // If backgroundImage has not been created yet - create it and add it to the list of drawables.
    if (backgroundImage == null) {
      backgroundImage = new MenuImage(renderer, getX(), getY(), getWidth(), getHeight(),alignPoint);
      addItem(backgroundImage);
    }

    backgroundImage.setTexture(id);
    return this;
  }
}
