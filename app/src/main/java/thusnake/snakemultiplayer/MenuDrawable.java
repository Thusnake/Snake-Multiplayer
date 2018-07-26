package thusnake.snakemultiplayer;

import android.util.Pair;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 23/01/2018.
 */

public abstract class MenuDrawable {
  final SimpleTimer x, y, scale = new SimpleTimer(1.0);
  private float width, height, initialX, initialY;
  private float[] colors = {1f, 1f, 1f, 1f}, disabledColors = {1f, 1f, 1f, 0.5f};
  private MenuAction action;
  protected final GameRenderer renderer;
  protected final GL10 gl;
  private boolean drawable = true, enabled = true;
  public enum EdgePoint {    TOP_LEFT,    TOP_CENTER,    TOP_RIGHT,
                            LEFT_CENTER,    CENTER,     RIGHT_CENTER,
                            BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT}
  final EdgePoint originPoint, alignPoint;

  public MenuDrawable(GameRenderer renderer, float x, float y, EdgePoint alignPoint,
                      EdgePoint originPoint) {
    this.renderer = renderer;
    this.gl = renderer.getGl();

    this.alignPoint = alignPoint;
    this.originPoint = originPoint;

    this.initialX = x;
    this.initialY = y;
    this.x = new SimpleTimer(initialX);
    this.y = new SimpleTimer(initialY);
  }

  public MenuDrawable(GameRenderer renderer, float x, float y, EdgePoint alignPoint) {
    this(renderer, x, y, alignPoint, EdgePoint.CENTER);
  }

  // The abstract methods.
  public abstract void draw();
  public void move(double dt) {
    if (!scale.isDone())
      scale.countEaseOut(dt, 8, 5 * dt);
  }

  // The non-abstract methods.

  /**
   * Performs translation to the proper x and y coordinates and then performs the regular scaling
   * operation, so that the drawable is ready to be drawn.
   * If this is not called, the drawable will be drawn around the bottom-left corner of the screen,
   * unless other translations have been made prior to drawing.
   */
  void translateAndScale() {
    gl.glTranslatef(-getEdgePointOffset(alignPoint).first + getEdgePointOffset(originPoint).first
                        + getX(alignPoint),
                    -getEdgePointOffset(alignPoint).second + getEdgePointOffset(originPoint).second
                        + getY(alignPoint), 0);
    gl.glScalef((float) scale.getTime(), (float) scale.getTime(), 0);
  }

  /**
   * @param x The x coordinate of the click.
   * @param y The y coordinate of the click.
   * @return Whether the click was within this drawable's boundaries.
   */
  public boolean isClicked(float x, float y) {
    y = renderer.getScreenHeight() - y;
    return (this.drawable
        && x > getX(EdgePoint.BOTTOM_LEFT) && x < getX(EdgePoint.TOP_RIGHT)
        && y > getY(EdgePoint.BOTTOM_LEFT) && y < getY(EdgePoint.TOP_RIGHT));
  }

  public void setAction(MenuAction action) {
    this.action = action;
  }
  public void performAction() {
    if (this.action != null && this.isEnabled())
      this.action.perform(this.renderer, this);
  }

  public void setColors(float[] rgba) {
    if (rgba.length == 4) {
      for (int index = 0; index < 4; index++)
        this.colors[index] = rgba[index];
      for (int index = 0; index < 3; index++)
        this.disabledColors[index] = rgba[index];
      this.disabledColors[3] = rgba[3] / 2;
    }
  }

  public float[] getColors() {
    if (this.isEnabled())
      return this.colors;
    else
      return this.disabledColors;
  }

  public void setOpacity(float opacity) {
    colors[3] = opacity;
  }

  public void setX(double x) { this.x.setTime(x); }
  public void setY(double y) { this.y.setTime(y); }
  public float getX() { return (float) x.getTime(); }
  public float getY() { return (float) y.getTime(); }
  public float getX(EdgePoint edgePoint) { return getCoordinates(edgePoint).first; }
  public float getY(EdgePoint edgePoint) { return getCoordinates(edgePoint).second; }

  public float getLeftX() { return getCoordinates(EdgePoint.BOTTOM_LEFT).first; }
  public float getBottomY() { return getCoordinates(EdgePoint.BOTTOM_LEFT).second; }
  public SimpleTimer getXTimer() { return this.x; }
  public SimpleTimer getYTimer() { return this.y; }
  public float getInitialX() { return this.initialX; }
  public float getInitialY() { return this.initialY; }
  public void setWidth(float width) { this.width = width; }
  public void setHeight(float height) { this.height = height; }
  public float getWidth() { return this.width; }
  public float getHeight() { return this.height; }

  /**
   * @param edgePoint A given edge point of the drawable's rectangle.
   * @return The edge point's offset as a Pair, relative to the bottom-left edge point.
   */
  public Pair<Float, Float> getEdgePointOffset(EdgePoint edgePoint) {
    switch (edgePoint) {
      case TOP_LEFT:      return new Pair<>(0f,           getHeight());
      case TOP_CENTER:    return new Pair<>(getWidth()/2, getHeight());
      case TOP_RIGHT:     return new Pair<>(getWidth(),   getHeight());
      case LEFT_CENTER:   return new Pair<>(0f,           getHeight()/2);
      case CENTER:        return new Pair<>(getWidth()/2, getHeight()/2);
      case RIGHT_CENTER:  return new Pair<>(getWidth(),   getHeight()/2);
      case BOTTOM_LEFT:   return new Pair<>(0f,           0f);
      case BOTTOM_CENTER: return new Pair<>(getWidth()/2, 0f);
      case BOTTOM_RIGHT:  return new Pair<>(getWidth(),   0f);
      default:            return new Pair<>(0f, 0f);
    }
  }

  /**
   * @param edgePoint A given edge point of the drawable's rectangle.
   * @return The edge point's coordinates as a Pair.
   */
  public Pair<Float, Float> getCoordinates(EdgePoint edgePoint) {
    return new Pair<>(getX() - getEdgePointOffset(alignPoint).first
                              + getEdgePointOffset(edgePoint).first,
                      getY() - getEdgePointOffset(alignPoint).second
                              + getEdgePointOffset(edgePoint).second);
  }

  public void setDestinationX(double destinationX) { this.x.setEndTimeFromNow(destinationX); }
  public void setDestinationY(double destinationY) { this.y.setEndTimeFromNow(destinationY); }
  public void setDestinationToInitial() {
    this.x.setEndTimeFromNow(initialX);
    this.y.setEndTimeFromNow(initialY);
  }
  public void setDestinationXFromOrigin(double offsetX) {
    this.x.setEndTimeFromNow(this.initialX + offsetX);
  }
  public void setDestinationYFromOrigin(double offsetY) {
    this.y.setEndTimeFromNow(this.initialY + offsetY);
  }


  public void setDrawable(boolean drawable) { this.drawable = drawable; }
  public boolean isDrawable() { return this.drawable; }

  public void setEnabled(boolean enabled) { this.enabled = enabled; }
  public boolean isEnabled() { return this.enabled; }


  public static EdgePoint combineEdgeHalves(EdgePoint firstHalf, EdgePoint secondHalf) {
    int firstIndex, secondIndex;
    switch (firstHalf) {
      case TOP_LEFT:
      case TOP_CENTER:
      case TOP_RIGHT:     firstIndex = 0; break;
      case LEFT_CENTER:
      case CENTER:
      case RIGHT_CENTER:  firstIndex = 1; break;
      case BOTTOM_LEFT:
      case BOTTOM_CENTER:
      case BOTTOM_RIGHT:  firstIndex = 2; break;
      default: throw new RuntimeException("Null passed to combineEdgeHalves(EdgePoint, EdgePoint)");
    }
    switch (secondHalf) {
      case TOP_LEFT:
      case LEFT_CENTER:
      case BOTTOM_LEFT:   secondIndex = 0; break;
      case TOP_CENTER:
      case CENTER:
      case BOTTOM_CENTER: secondIndex = 1; break;
      case TOP_RIGHT:
      case RIGHT_CENTER:
      case BOTTOM_RIGHT:  secondIndex = 2; break;
      default: throw new RuntimeException("Null passed to combineEdgeHalves(EdgePoint, EdgePoint)");
    }
    switch (firstIndex * 10 + secondIndex) {
      case 0:  return EdgePoint.TOP_LEFT;
      case 1:  return EdgePoint.TOP_CENTER;
      case 2:  return EdgePoint.TOP_RIGHT;
      case 10: return EdgePoint.LEFT_CENTER;
      case 11: return EdgePoint.CENTER;
      case 12: return EdgePoint.RIGHT_CENTER;
      case 20: return EdgePoint.BOTTOM_LEFT;
      case 21: return EdgePoint.BOTTOM_CENTER;
      case 22: return EdgePoint.BOTTOM_RIGHT;
    }
    throw new RuntimeException("Fix combineEdgeHalves(EdgePoint, EdgePoint)");
  }
}
