package thusnake.snakemultiplayer;

import android.view.MotionEvent;

/**
 * A container which draws all of its items from top to bottom, one under another.
 */
public class MenuListOfItems extends MenuContainer {
  public MenuListOfItems(GameRenderer renderer, float x, float y, EdgePoint alignPoint) {
    super(renderer, x, y, alignPoint);
  }

  @Override
  public void draw(float[] parentColors) {
    gl.glPushMatrix();

    // Scaling.
    gl.glTranslatef(getX(originPoint), getY(originPoint), 0);
    gl.glScalef((float) scale.getTime(), (float) scale.getTime(), 0);
    gl.glTranslatef(-getX(originPoint), -getY(originPoint), 0);

    // Drawing. We're now at the top point of this drawable.
    super.draw(parentColors);

    gl.glPopMatrix();
  }

  @Override
  public void move(double dt) {
    super.move(dt);

    // Update width and height before drawing.
    if (contents.size() > 0) {
      MenuDrawable firstDrawable = contents.get(0);
      float lowestX  = firstDrawable.getX(EdgePoint.LEFT_CENTER),
            highestX = firstDrawable.getX(EdgePoint.RIGHT_CENTER),
            heightSum = firstDrawable.getHeight();

      for (MenuDrawable drawable : contents) {
        if (drawable.equals(firstDrawable)) continue;

        if (drawable.getX(EdgePoint.LEFT_CENTER) < lowestX)
          lowestX = drawable.getX(EdgePoint.LEFT_CENTER);
        if (drawable.getX(EdgePoint.RIGHT_CENTER) > highestX)
          highestX = drawable.getX(EdgePoint.RIGHT_CENTER);

        heightSum += drawable.getHeight();
      }

      // The width is equal to the distance between its leftmost and its rightmost points.
      setWidth(highestX - lowestX);
      // The height is equal to the sum of all its elements' heights.
      setHeight(heightSum);

      // Once the container's coordinates have been set, arrange the items inside vertically.
      heightSum = 0;
      for (MenuDrawable drawable : contents) {
        drawable.setX(this.getX());
        drawable.setY(this.getY(EdgePoint.TOP_CENTER) - heightSum);
        heightSum += drawable.getHeight();
      }
    }
  }

  /**
   * Adds an item to the list. The item's vertical alignment and its x and y coordinates are
   * irrelevant.
   * @param item The item to be added.
   */
  @Override
  public void addItem(MenuDrawable item) {
    item.alignPoint = MenuDrawable.combineEdgeHalves(EdgePoint.TOP_CENTER, this.alignPoint);
    item.setX(this.getX());
    item.setY(0);
    super.addItem(item);
  }
}
