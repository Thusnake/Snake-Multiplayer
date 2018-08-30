package thusnake.snakemultiplayer;

/**
 * Created by Nick on 22/01/2018.
 */

public class MenuImage extends MenuDrawable implements TextureReloadable {
  private Square drawable;

  /**
   * Creates a rectangle drawable with given coordinates, which can be textured.
   * @param renderer The renderer to handle the drawing.
   * @param x The x coordinate.
   * @param y The y coordinate.
   * @param width The width.
   * @param height The height.
   * @param alignPoint The edge which (x,y) represents.
   * @param originPoint The edge from which scaling and rotation is done.
   */
  public MenuImage(GameRenderer renderer, float x, float y, float width, float height,
                   EdgePoint alignPoint, EdgePoint originPoint) {
    super(renderer, x, y, alignPoint, originPoint);

    setWidth(width);
    setHeight(height);

    drawable = new Square(renderer,
                          -getEdgePointOffset(originPoint).first,
                          -getEdgePointOffset(originPoint).second,
                          width, height);
  }

  /**
   * {@link MenuImage#MenuImage(GameRenderer, float, float, float, float, EdgePoint, EdgePoint)}
   * <br>
   * The scaling and rotation point is assumed to be the center.
   */
  public MenuImage(GameRenderer renderer, float x, float y, float width, float height,
                   EdgePoint alignPoint) {
    this(renderer, x, y, width, height, alignPoint, EdgePoint.CENTER);
  }

  /**
   * {@link MenuImage#MenuImage(GameRenderer, float, float, float, float, EdgePoint, EdgePoint)}
   * <br>
   * This constructor also textures the image immediately.
   * @param resourceId The Android resource ID of the image.
   */
  public MenuImage(GameRenderer renderer, float x, float y, float width, float height,
                   EdgePoint alignPoint, EdgePoint originPoint, int resourceId) {
    this(renderer, x, y, width, height, alignPoint, originPoint);
    setTexture(resourceId);
  }

  /**
   * {@link MenuImage#MenuImage(GameRenderer, float, float, float, float, EdgePoint, EdgePoint,
   * int)}
   * <br>
   * Assumes the scaling and rotation point to be the center.
   */
  public MenuImage(GameRenderer renderer, float x, float y, float width, float height,
                   EdgePoint alignPoint, int resourceId) {
    this(renderer, x, y, width, height, alignPoint, EdgePoint.CENTER, resourceId);
  }

  // Drawing methods.
  public void draw(float[] parentColors) {
    if (isDrawable()) {
      gl.glPushMatrix();

      // Translate to the bottom-left corner and add the origin offset, so that the image fits.
      gl.glTranslatef(getX(originPoint), getY(originPoint), 0);
      gl.glScalef((float) scale.getTime(), (float) scale.getTime(), 0); // Scale it.
      glColor4array(gl, combineColorArrays(getColors(), parentColors));
      drawable.draw(gl);

      gl.glPopMatrix();
    }
  }

  public void setTexture(int id) {
    this.drawable.setTexture(id);
  }

  @Override
  public void reloadTexture() {
    drawable.reloadTexture();
  }

  public void move(double dt) {
    super.move(dt);
  }
}
