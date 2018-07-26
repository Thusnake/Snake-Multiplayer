package thusnake.snakemultiplayer;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MenuCarousel extends MenuDrawable implements TextureReloadable {
  private final List<CarouselItem> choices = new LinkedList<>();
  private CarouselItem currentChoice;
  private boolean locked = false;

  public MenuCarousel(GameRenderer renderer, float x, float y, float width, float height,
                      EdgePoint alignPoint) {
    super(renderer, x, y, alignPoint, EdgePoint.CENTER);

    setWidth(width);
    setHeight(height);
  }

  @Override
  public void draw() {
    if (locked) {
      float xCenter = getLeftX() + getWidth() / 2f;

      // Draw the current choice in the middle.
      gl.glPushMatrix();
      gl.glTranslatef(xCenter,
                      getBottomY() + getHeight() / 2f, 0);
      currentChoice.drawable.draw();
      gl.glPopMatrix();

      // Create an array of carousel items to be drawn.
      List<CarouselItem> leftToBeDrawn = new ArrayList<>();
      for (int index = choices.indexOf(currentChoice); index < choices.size(); index++)
        leftToBeDrawn.add(choices.get(index));
      for (int index = 0; index < choices.indexOf(currentChoice); index++)
        leftToBeDrawn.add(choices.get(index));

      // Set some values for the leftmost and rightmost point currently.
      float leftBoundary = xCenter - currentChoice.getVisualWidth() / 2f;
      float rightBoundary = xCenter + currentChoice.getVisualWidth() / 2f;

      boolean lastChoiceWasRight = false;

      // Loop until we either run out of items to draw or run out of space to draw them.
      while(leftToBeDrawn.size() > 0
            && leftBoundary > getLeftX() || rightBoundary < getLeftX() + getWidth()) {
        if (lastChoiceWasRight) {
          // We're drawing on the left.
          if (leftBoundary > getLeftX()) { // If there is still space left.

            // First check if there will be space left after this.
            if (leftBoundary - leftToBeDrawn.get(leftToBeDrawn.size() - 1).getVisualWidth()
                - renderer.getScreenWidth() / 18f < getLeftX()) {
              // If not then don't do anything and just announce no space left on this side.
              leftBoundary = getLeftX();
              continue;
            }

            // Get the drawable we'll be working with.
            CarouselItem leftChoice = leftToBeDrawn.remove(leftToBeDrawn.size() - 1);

            // Update the left boundary.
            leftBoundary -= leftChoice.getVisualWidth() - renderer.getScreenWidth() / 18f;

            // Draw it.
            gl.glPushMatrix();
            gl.glTranslatef(leftBoundary,
                            getBottomY() + getHeight() / 2f - leftChoice.getHeight() / 2f, 0);
            leftChoice.drawable.draw();
            gl.glPopMatrix();
          }
          lastChoiceWasRight = false;

        } else {
          if (rightBoundary < getLeftX() + getWidth()) {

            // First check if there will be space left after this.
            if (rightBoundary + leftToBeDrawn.get(leftToBeDrawn.size() - 1).getVisualWidth()
                + renderer.getScreenWidth() / 18f > getLeftX() + getWidth()) {
              // If not then don't do anything and just announce no space left on this side.
              rightBoundary = getLeftX() + getWidth();
              continue;
            }

            // Get the drawable we'll be working with.
            CarouselItem rightChoice = leftToBeDrawn.remove(0);

            // Update the right boundary.
            rightBoundary += rightChoice.getVisualWidth() + renderer.getScreenWidth() / 18f;

            // Draw it.
            gl.glPushMatrix();
            gl.glTranslatef(rightBoundary - rightChoice.getVisualWidth(),
                            getBottomY() + getHeight() / 2f - rightChoice.getHeight() / 2f, 0);
            rightChoice.drawable.draw();
            gl.glPopMatrix();
          }
          lastChoiceWasRight = true;
        }
      }
    }
  }

  @Override
  public void move(double dt) {
    super.move(dt);
    if (locked) {
      for (CarouselItem item : choices)
        item.drawable.move(dt);
    }
  }

  @Override
  public void reloadTexture() {

  }

  public void onMotionEvent() {

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
   * Adds a choice to the carousel, visually represented via an image.
   * @param resourceId The ID of the image resource.
   * @param name The name of the choice.
   */
  public void addImageChoice(int resourceId, String name) {
    if (!locked) {
      // Set the width and the height to fit.
      Bitmap bitmap = renderer.loadTextureBitmap(resourceId);
      float width = bitmap.getWidth() > bitmap.getHeight()
                    ? getHeight()
                    : getHeight() * bitmap.getWidth() / bitmap.getHeight();
      float height = bitmap.getWidth() > bitmap.getHeight()
                     ? getHeight() * bitmap.getHeight() / bitmap.getWidth()
                     : getHeight();
      MenuImage image = new MenuImage(renderer, 0, 0, width, height, EdgePoint.CENTER, resourceId);

      choices.add(new CarouselItem(this, image, name));
    }
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
  }

  public void unchoose() {
    drawable.scale.setEndTimeFromNow(notChosenScaleMultiplier);
    drawable.setOpacity(0.75f);
  }

  public float getWidth() { return drawable.getWidth(); }
  public float getHeight() { return drawable.getHeight(); }
  public float getVisualWidth() { return drawable.getWidth() * (float) drawable.scale.getTime(); }
}
