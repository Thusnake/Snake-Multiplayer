package thusnake.snakemultiplayer;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MenuCarousel extends MenuDrawable implements TextureReloadable {
  private final List<CarouselItem> choices = new LinkedList<>();
  private CarouselItem currentChoice;
  private boolean locked = false, isHeld = false, noBoundaries = false;
  private SimpleTimer slideX = new SimpleTimer(0.0), holdTimer = new SimpleTimer(0.0, 1.0),
      inertiaX = new SimpleTimer(0.0) {
    @Override
    public void onDone() {
      super.onDone();
      snap();
    }
  };

  public MenuCarousel(GameRenderer renderer, float x, float y, float width, float height,
                      EdgePoint alignPoint) {
    super(renderer, x, y, alignPoint, EdgePoint.CENTER);

    setWidth(width);
    setHeight(height);
  }

  public MenuCarousel noBoundaries() { noBoundaries = true; return this; }

  @Override
  public void draw() {
    if (locked) {
      float xCenter = getLeftX() + getWidth() / 2f;

      gl.glPushMatrix();
      gl.glTranslatef((float) slideX.getTime(), 0, 0);

      // Draw the current choice in the middle.
      gl.glPushMatrix();
      gl.glTranslatef(xCenter,
                      getBottomY() + getHeight() / 2f, 0);
      currentChoice.drawable.draw();
      gl.glPopMatrix();

      // Create rotation of carousel items to be drawn on the right.
      List<CarouselItem> rotationRight = new LinkedList<>();
      List<CarouselItem> rotationLeft = new LinkedList<>();
      for (int index = choices.indexOf(currentChoice) + 1; index < choices.size(); index++) {
        rotationRight.add(choices.get(index));
        rotationLeft.add(choices.get(index));
      }
      for (int index = 0; index < choices.indexOf(currentChoice); index++) {
        rotationRight.add(choices.get(index));
        rotationLeft.add(choices.get(index));
      }

      // Let the left rotation be the same as the right one, only reversed.
      Collections.reverse(rotationLeft);

      // Add the current choice at the end of both.
      rotationRight.add(currentChoice);
      rotationLeft.add(currentChoice);

      // Set some values for the leftmost and rightmost point currently.
      float leftBoundary = xCenter - currentChoice.getVisualWidth() / 2f;
      float rightBoundary = xCenter + currentChoice.getVisualWidth() / 2f;

      // These are the limiting boundaries.
      float leftLimit = getX() - (float) slideX.getTime();
      float rightLimit = getX(EdgePoint.RIGHT_CENTER) - (float) slideX.getTime();

      // Loop until we run out of space for items to be drawn.
      while(leftBoundary > leftLimit) {
        // First check if there will be space left after this.
        if (leftBoundary - rotationLeft.get(0).getVisualWidth()
            - renderer.getScreenHeight() / 18f < leftLimit && !noBoundaries) {
          // If not then don't do anything and just announce no space left on this side.
          // No boundaries mode prevents this check.
          leftBoundary = leftLimit;
          continue;
        }

        // Get the drawable we'll be working with.
        CarouselItem leftChoice = rotationLeft.get(0);

        // Rotate the list for next time.
        Collections.rotate(rotationLeft, -1);

        // Update the left boundary.
        leftBoundary -= leftChoice.getVisualWidth() + renderer.getScreenHeight() / 18f;

        // Draw it.
        gl.glPushMatrix();
        gl.glTranslatef(leftBoundary + leftChoice.getVisualWidth() / 2f,
            getY(EdgePoint.CENTER), 0);
        leftChoice.drawable.draw();
        gl.glPopMatrix();

      }

      while(rightBoundary < rightLimit) {
        // First check if there will be space left after this.
        if (rightBoundary + rotationRight.get(0).getVisualWidth()
            + renderer.getScreenHeight() / 18f > rightLimit && !noBoundaries) {
          // If not then don't do anything and just announce no space left on this side.
          // No boundaries mode prevents this check.
          rightBoundary = rightLimit;
          continue;
        }

        // Get the drawable we'll be working with.
        CarouselItem rightChoice = rotationRight.get(0);
        Collections.rotate(rotationRight, -1);

        // Update the right boundary.
        rightBoundary += rightChoice.getVisualWidth() + renderer.getScreenHeight() / 18f;

        // Draw it.
        gl.glPushMatrix();
        gl.glTranslatef(rightBoundary - rightChoice.getVisualWidth() / 2f,
                        getY(EdgePoint.CENTER), 0);
        rightChoice.drawable.draw();
        gl.glPopMatrix();
      }

      gl.glPopMatrix();
    }
  }

  @Override
  public void move(double dt) {
    super.move(dt);
    if (locked) {
      for (CarouselItem item : choices)
        item.drawable.move(dt);
      if (!slideX.isDone()) slideX.countEaseOut(dt, 10, 10*dt);
      if (!inertiaX.isDone() && !isHeld) {
        slideX.setTime(slideX.getTime() + inertiaX.getTime());
        inertiaX.countEaseOut(dt, 2, renderer.getScreenHeight() * 8 * dt);
      } else if (isHeld) {
        if (holdTimer.count(dt)) {
          snap();
          isHeld = false;
        }
      }
    }
  }

  @Override
  public void reloadTexture() {
    for (CarouselItem choice : choices)
      if (choice.drawable instanceof TextureReloadable)
        ((TextureReloadable) choice.drawable).reloadTexture();
  }

  @Override
  public void onMotionEvent(MotionEvent event, float[] pointerX, float[] pointerY) {
    super.onMotionEvent(event, pointerX, pointerY);

    if (event.getActionMasked() == MotionEvent.ACTION_DOWN && isClicked(pointerX[0], pointerY[0])) {
      isHeld = true;
      holdTimer.reset();

    } else if (event.getActionMasked() == MotionEvent.ACTION_UP && isHeld) {
      isHeld = false;

      // Snap even if no inertia has been generated.
      if (inertiaX.isDone()) snap();

    } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && isHeld) {
      holdTimer.reset();
      if (event.getHistorySize() > 0) {
        // Scroll using setTime() so that the timer goal is cleared, which stops the snapping.
        slideX.setTime(slideX.getTime() + event.getX() - event.getHistoricalX(0));

        // Also set the inertia.
        inertiaX.setTime(event.getX() - event.getHistoricalX(0));
        inertiaX.setEndTimeFromNow(0.0);
      }
    }
  }

  /** Assess closest CarouselItem and snap to it. */
  private void snap() {
    int closestIndex = choices.indexOf(currentChoice), checkIndex = closestIndex;
    double closestItemDistance = slideX.getTime();
    if (slideX.getTime() < 0) {
      // Check to the right.
      while (true) {
        // Rotate the checkIndex right.
        if (++checkIndex >= choices.size()) checkIndex = 0;

        double checkItemDistance
            = closestItemDistance + choices.get(closestIndex).getVisualWidth() / 2f
            + renderer.getScreenHeight() / 18f + choices.get(checkIndex).getVisualWidth() / 2f;

        if (Math.abs(checkItemDistance) > Math.abs(closestItemDistance))
          // There is no closer item.
          break;
        else {
          // Check the next one.
          closestIndex = checkIndex;
          closestItemDistance = checkItemDistance;
        }
      }
    } else {
      // Check to the left.
      while (true) {
        // Rotate the checkIndex right.
        if (--checkIndex < 0) checkIndex = choices.size() - 1;

        double checkItemDistance
            = closestItemDistance - choices.get(closestIndex).getVisualWidth() / 2f
            - renderer.getScreenHeight() / 18f - choices.get(checkIndex).getVisualWidth() / 2f;

        if (Math.abs(checkItemDistance) > Math.abs(closestItemDistance))
          // There is no closer item.
          break;
        else {
          // Check the next one.
          closestIndex = checkIndex;
          closestItemDistance = checkItemDistance;
        }
      }
    }

    // Select that CarouselItem.
    currentChoice.unchoose();
    currentChoice = choices.get(closestIndex);
    currentChoice.choose();

    // Set the slide to a value that makes it seamless.
    slideX.setTime(closestItemDistance);
    slideX.setEndTimeFromNow(0);
  }

  /**
   * Adds a custom choice to the carousel.
   * @param choice The CarouselItem that represents the choice.
   */
  public void addChoice(CarouselItem choice) {
    if (!locked)
      choices.add(choice);
  }

  /**
   * Confirms the choices you've added, locking the list and becoming drawable.
   * @return Whether or not the lock was successful.
   */
  public boolean confirmChoices() {
    if (choices.size() == 0)
      return false;

    currentChoice = choices.get(0);
    currentChoice.choose();
    locked = true;
    return true;
  }
}

class CarouselItem {
  private final MenuCarousel carousel;
  final MenuDrawable drawable;
  final String name;
  private final float notChosenScaleMultiplier = 0.8f;

  public CarouselItem(MenuCarousel carousel, MenuDrawable drawable, String name) {
    this.carousel = carousel;
    this.drawable = drawable;
    this.name = name;

    // Set the actual scale to one of an item that hasn't been chosen yet.
    unchoose();
  }

  public void choose() {
    drawable.scale.setEndTimeFromNow(1);
    drawable.setOpacity(1);
    onChosen();
  }

  public void unchoose() {
    drawable.scale.setEndTimeFromNow(notChosenScaleMultiplier);
    drawable.setOpacity(0.75f);
  }

  public float getWidth() { return drawable.getWidth(); }
  public float getHeight() { return drawable.getHeight(); }
  public float getVisualWidth() { return drawable.getWidth() * (float) drawable.scale.getTime(); }

  /**
   * Creates an image, resized to fit a carousel.
   * @param carousel The carousel it will be part of.
   * @param resourceId The ID of the image resource.
   * @return The resulting MenuImage.
   */
  @NonNull
  public static MenuImage makeFittingImage(MenuCarousel carousel, int resourceId) {
    // Set the width and the height to fit.
    Bitmap bitmap = carousel.renderer.loadTextureBitmap(resourceId);
    float width = bitmap.getWidth() > bitmap.getHeight()
        ? carousel.getHeight()
        : carousel.getHeight() * bitmap.getWidth() / bitmap.getHeight();
    float height = bitmap.getWidth() > bitmap.getHeight()
        ? carousel.getHeight() * bitmap.getHeight() / bitmap.getWidth()
        : carousel.getHeight();

    return new MenuImage(carousel.renderer, 0, 0, width, height,
                                    MenuDrawable.EdgePoint.CENTER, resourceId);
  }

  public void onChosen() {}
}
