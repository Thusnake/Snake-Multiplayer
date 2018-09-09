package thusnake.snakemultiplayer;

/**
 * A container which wraps around its contents for grouping sake.
 */
public class MenuFlexContainer extends MenuDynamicContainer {
  public MenuFlexContainer(GameRenderer renderer, EdgePoint alignPoint) {
    super(renderer, 0, 0, alignPoint, EdgePoint.CENTER);
  }

  @Override
  public void draw(float[] parentColors) {
    if (isDrawable()) {
      gl.glPushMatrix();

      // Scaling.
      gl.glTranslatef(getX(originPoint), getY(originPoint), 0);
      gl.glScalef((float) scale.getTime(), (float) scale.getTime(), 0);
      gl.glTranslatef(-getX(originPoint), -getY(originPoint), 0);

      // Drawing.
      super.draw(parentColors);

      gl.glPopMatrix();
    }
  }

  @Override
  public void move(double dt) {
    super.move(dt);

    // Update width and height before drawing.
    if (contents.size() > 0) {
      MenuDrawable firstDrawable = contents.get(0);
      float lowestX  = firstDrawable.getX(EdgePoint.LEFT_CENTER),
          highestX = firstDrawable.getX(EdgePoint.RIGHT_CENTER),
          lowestY  = firstDrawable.getY(EdgePoint.BOTTOM_CENTER),
          highestY = firstDrawable.getY(EdgePoint.TOP_CENTER);
      for (MenuDrawable drawable : contents) {
        if (drawable.equals(firstDrawable)) continue;

        if (drawable.getX(EdgePoint.LEFT_CENTER) < lowestX)
          lowestX = drawable.getX(EdgePoint.LEFT_CENTER);
        if (drawable.getX(EdgePoint.RIGHT_CENTER) > highestX)
          highestX = drawable.getX(EdgePoint.RIGHT_CENTER);
        if (drawable.getY(EdgePoint.BOTTOM_CENTER) < lowestY)
          lowestY = drawable.getY(EdgePoint.BOTTOM_CENTER);
        if (drawable.getY(EdgePoint.TOP_CENTER) > highestY)
          highestY = drawable.getY(EdgePoint.TOP_CENTER);
      }

      // Set the x and y directly through the timers as to not offset the contents, only wrap around
      // new ones.
      setWidth(highestX - lowestX);
      setHeight(highestY - lowestY);
      x.setCurrentTime(lowestX + getEdgePointOffset(alignPoint).first);
      y.setCurrentTime(lowestY + getEdgePointOffset(alignPoint).second);
    }
  }
}
