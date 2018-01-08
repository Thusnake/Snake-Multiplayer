package thusnake.snakemultiplayer;

/**
 * Created by Nick on 13/12/2017.
 */

public class BodyPart {
  private int x, y, index;
  private final boolean isHead;
  private float[] colors;
  private final Player player;
  private final Game game;
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
    if (this.isHead) this.colors = player.getColors();
    else this.colors = player.getBodyColors();
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
    if (this.isHead) this.colors = player.getColors();
    else this.colors = player.getBodyColors();
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
    if (this.isHead) this.colors = player.getColors();
    else this.colors = player.getBodyColors();
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

  public void updateColors() {
    if (!this.isOutOfBounds()) this.boardSquares.updateColors(this.x, this.y, this.colors);
  }

  public int getX() { return this.x; }
  public int getY() { return this.y; }

  public boolean isOutOfBounds() {
    return this.x < 0 || this.x >= game.getHorizontalSquares()
        || this.y < 0 || this.y >= game.getVerticalSquares();
  }

  public float[] getColors() { return this.colors; }
}
