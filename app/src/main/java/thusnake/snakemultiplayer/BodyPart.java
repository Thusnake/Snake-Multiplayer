package thusnake.snakemultiplayer;

/**
 * Created by Nick on 13/12/2017.
 */

public class BodyPart {
  private int x, y;
  private float[] colors;
  private final Snake snake;
  private final BodyPart forwardPart;
  private BodyPart backwardPart;
  private final BoardDrawer game;

  /** Head constructor. */
  public BodyPart(Snake snake, int x, int y) {
    forwardPart = null;
    this.snake = snake;
    this.game = snake.getGame();
    this.x = x;
    this.y = y;
    colors = snake.getSkin().headColors();
  }

  /** Duplicate tail constructor. */
  public BodyPart(Snake snake, BodyPart forwardPart) {
    forwardPart.setBackwardPart(this);
    this.forwardPart = forwardPart;
    this.snake = snake;
    this.game = snake.getGame();
    this.colors = snake.getSkin().tailColors();
    this.x = forwardPart.getX();
    this.y = forwardPart.getY();
  }

  /** Offset tail constructor. */
  public BodyPart(Snake snake, BodyPart forwardPart, Snake.Direction direction) {
    this(snake, forwardPart);
    switch (direction) {
      case UP: y++; break;
      case DOWN: y--; break;
      case LEFT: x--; break;
      case RIGHT: x++; break;
      default: break;
    }
  }

  public void setBackwardPart(BodyPart backwardPart) {
    this.backwardPart = backwardPart;
  }

  public BodyPart getBackwardPart() { return backwardPart; }

  public BodyPart getForwardPart() { return forwardPart; }

  /** Moves this body part and all body parts behind it (i.e. leading up to the tail). */
  public void move() {
    // First make all previous parts move.
    if (backwardPart != null)
      backwardPart.move();

    // Then move this one.
    if (isHead()) {
      switch (this.snake.getDirection()) {
        case UP: y++; break;
        case DOWN: y--; break;
        case LEFT: x--; break;
        case RIGHT: x++; break;
        default: break;
      }
    } else {
      x = forwardPart.getX();
      y = forwardPart.getY();
    }
  }

  /**
   * Warps this body part to a different position.
   * Would not recommend using it on anything but the head.
   */
  public void warp(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public void updateColors() {
    if (!this.isOutOfBounds()) game.getBoardSquares().updateColors(this.x, this.y, this.colors);
  }

  public int getX() { return this.x; }
  public int getY() { return this.y; }

  public boolean isOutOfBounds() {
    return this.x < 0 || this.x >= game.getHorizontalSquares()
        || this.y < 0 || this.y >= game.getVerticalSquares();
  }

  public boolean isHead() { return forwardPart == null; }

  /**
   * Returns the relative direction of an adjacent body part.
   * @param otherPart The other part.
   * @return The direction that other part is relative to this part. Null if the other part is not
   * actually adjacent or is equivalent.
   */
  public Snake.Direction adjacentDirection(BodyPart otherPart) {
    if (otherPart.getX() == getX() && otherPart.getY() == getY())
      return null;
    else if (otherPart.getX() == getX())
      return otherPart.getY() > getY() ? Snake.Direction.UP : Snake.Direction.DOWN;
    else if (otherPart.getY() == getY())
      return otherPart.getX() > getX() ? Snake.Direction.RIGHT : Snake.Direction.LEFT;
    else
      return null;
  }

  public float[] getColors() { return this.colors; }
}
