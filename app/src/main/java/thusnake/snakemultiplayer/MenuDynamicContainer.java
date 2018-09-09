package thusnake.snakemultiplayer;

/**
 * A MenuContainer which upon being moved also moves all its contents by the same relative amount.
 */
public abstract class MenuDynamicContainer extends MenuContainer {
  public MenuDynamicContainer(GameRenderer renderer, float x, float y, EdgePoint alignPoint,
                              EdgePoint originPoint) {
    super(renderer, x, y, alignPoint, originPoint);
  }

  public MenuDynamicContainer(GameRenderer renderer, float x, float y, EdgePoint alignPoint) {
    this(renderer, x, y, alignPoint, EdgePoint.CENTER);
  }

  // These methods will also offset the contents.
  @Override
  public void setX(double x) {
    for (MenuDrawable drawable : contents)
      drawable.setX(drawable.getX() + x - this.getX());
    super.setX(x);
  }

  @Override
  public void setY(double y) {
    for (MenuDrawable drawable : contents)
      drawable.setY(drawable.getY() + y - this.getY());
    super.setY(y);
  }

  @Override
  public void setDestinationX(double destinationX) {
    for (MenuDrawable drawable : contents)
      drawable.setDestinationX(drawable.getX() + destinationX - this.getX());
    super.setDestinationX(destinationX);
  }

  @Override
  public void setDestinationY(double destinationY) {
    for (MenuDrawable drawable : contents)
      drawable.setDestinationY(drawable.getY() + destinationY - this.getY());
    super.setDestinationY(destinationY);
  }

  @Override
  public void setDestinationXFromOrigin(double offsetX) {
    for (MenuDrawable drawable : contents)
      drawable.setDestinationXFromOrigin(offsetX);
    super.setDestinationXFromOrigin(offsetX);
  }

  @Override
  public void setDestinationYFromOrigin(double offsetY) {
    for (MenuDrawable drawable : contents)
      drawable.setDestinationYFromOrigin(offsetY);
    super.setDestinationYFromOrigin(offsetY);
  }

  @Override
  public void setDestinationToInitial() {
    for (MenuDrawable drawable : contents)
      drawable.setDestinationToInitial();
    super.setDestinationToInitial();
  }
}
