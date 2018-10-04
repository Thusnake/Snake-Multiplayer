package thusnake.snakemultiplayer;

/**
 * Created by Nick on 13/12/2017.
 */

public class BodyPart {
  private int x, y, index;
  private final boolean isHead;
  private float[] colors;
  private final Player player;
  private final BoardDrawer game;
  private final Mesh boardSquares;

  // Default constructor - duplicates last body part.
  public BodyPart(Player player) {
    this.player = player;
    this.game = player.getGame();
    this.boardSquares = game.getBoardSquares();
    this.x = player.getBodyPart(-1).getX();
    this.y = player.getBodyPart(-1).getY();
    this.isHead = player.getBodyLength() == 0;
    this.index = player.getBodyLength();
    if (this.isHead) this.colors = player.getSkin().headColors();
    else this.colors = player.getSkin().tailColors();
  }

  // Fixed constructor
  public BodyPart(Player player, int x, int y) {
    this.player = player;
    this.game = player.getGame();
    this.boardSquares = game.getBoardSquares();
    this.x = x;
    this.y = y;
    this.isHead = player.getBodyLength() == 0;
    this.index = player.getBodyLength();
    if (this.isHead) this.colors = player.getSkin().headColors();
    else this.colors = player.getSkin().tailColors();
  }

  // Offset constructor
  public BodyPart(Player player, Player.Direction direction) {
    this.player = player;
    this.game = player.getGame();
    this.boardSquares = game.getBoardSquares();
    switch (direction) {
      case UP:
        this.x = player.getBodyPart(-1).getX();
        this.y = player.getBodyPart(-1).getY() + 1;
        break;
      case DOWN:
        this.x = player.getBodyPart(-1).getX();
        this.y = player.getBodyPart(-1).getY() - 1;
        break;
      case LEFT:
        this.x = player.getBodyPart(-1).getX() - 1;
        this.y = player.getBodyPart(-1).getY();
        break;
      case RIGHT:
        this.x = player.getBodyPart(-1).getX() + 1;
        this.y = player.getBodyPart(-1).getY();
        break;
      default: break;
    }
    this.isHead = player.getBodyLength() == 0;
    this.index = player.getBodyLength();
    if (this.isHead) this.colors = player.getSkin().headColors();
    else this.colors = player.getSkin().tailColors();
  }

  public void move() {
    if (this.isHead) {
      switch (this.player.getDirection()) {
        case UP: this.y += 1; break;
        case DOWN: this.y -= 1; break;
        case LEFT: this.x -= 1; break;
        case RIGHT: this.x += 1; break;
        default: break;
      }
    } else {
      this.x = player.getBodyPart(this.index - 1).getX();
      this.y = player.getBodyPart(this.index - 1).getY();
    }
  }

  public void warp(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public void updateColors() {
    if (!this.isOutOfBounds()) this.boardSquares.updateColors(this.x, this.y, this.colors);
  }

  public int getX() { return this.x; }
  public int getY() { return this.y; }

  public boolean isOutOfBounds() {
    return this.x < 0 || this.x >= game.getHorizontalSquares()
        || this.y < 0 || this.y >= game.getVerticalSquares();
  }

  /**
   * Returns the relative direction of an adjacent body part.
   * @param otherPart The other part.
   * @return The direction that other part is relative to this part. Null if the other part is not
   * actually adjacent or is equivalent.
   */
  public Player.Direction adjacentDirection(BodyPart otherPart) {
    if (otherPart.getX() == getX() && otherPart.getY() == getY())
      return null;
    else if (otherPart.getX() == getX())
      return otherPart.getY() > getY() ? Player.Direction.UP : Player.Direction.DOWN;
    else if (otherPart.getY() == getY())
      return otherPart.getX() > getX() ? Player.Direction.RIGHT : Player.Direction.LEFT;
    else
      return null;
  }

  public float[] getColors() { return this.colors; }
}
