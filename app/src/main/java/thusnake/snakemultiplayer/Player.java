package thusnake.snakemultiplayer;

import android.content.Context;
import android.os.Vibrator;

import static thusnake.snakemultiplayer.Player.Direction.DOWN;
import static thusnake.snakemultiplayer.Player.Direction.LEFT;
import static thusnake.snakemultiplayer.Player.Direction.RIGHT;
import static thusnake.snakemultiplayer.Player.Direction.UP;

/**
 * Created by Nick on 12/12/2017.
 */

public class Player {
  public enum Direction {UP, DOWN, LEFT, RIGHT}
  private Direction direction, previousDirection;
  private boolean alive, drawable, flashing;
  private int number, score;
  private String name;
  private ControlType controlType;
  public enum ControlType {OFF, CORNER, SWIPE, KEYBOARD, GAMEPAD, BLUETOOTH, WIFI}
  private CornerLayout cornerLayout;
  private BodyPart[] bodyParts = new BodyPart[0];
  private int bodyLength = 0;
  private float[] colors = new float[4];
  private int colorIndex = 0;
  private Game game;
  private Vibrator vibrator;
  private Mesh boardSquares;
  private int onlineIdentifier;

  // Constructor for a corner layout player.
  public Player() {

  }

  // Gets called upon game start.
  public void prepareForGame(Game game, int number) {
    this.game = game;
    this.number = number;
    this.vibrator = (Vibrator) game.getRenderer().getContext()
        .getSystemService(Context.VIBRATOR_SERVICE);
    this.boardSquares = game.getBoardSquares();
    if (this.getControlCorner() == CornerLayout.Corner.UPPER_LEFT
        || this.getControlCorner() == CornerLayout.Corner.UPPER_RIGHT) this.direction = DOWN;
    else                                                          this.direction = UP;
    this.previousDirection = this.direction;
    this.alive = true;
    this.drawable = true;
    this.flashing = false;
    this.score = 0;
    switch (this.getControlCorner()) {
      case LOWER_LEFT:
        this.expandBody(0, 0); break;
      case LOWER_RIGHT:
        this.expandBody(game.getRenderer().getMenu().horizontalSquares - 1, 0); break;
      case UPPER_LEFT:
        this.expandBody(0, game.getRenderer().getMenu().verticalSquares - 1); break;
      case UPPER_RIGHT:
        this.expandBody(game.getRenderer().getMenu().horizontalSquares - 1,
            game.getRenderer().getMenu().verticalSquares - 1); break;
    }
    for (int index = 1; index < 4; index++) {
      this.expandBody(Player.getOppositeDirection(this.direction));
    }
  }

  public boolean move() {
    // First remove the color from the furthest body part.
    if (!this.getBodyPart(-1).isOutOfBounds()) {
      boardSquares.updateColors(this.getBodyPart(-1).getX(), this.getBodyPart(-1).getY(),
          game.getBoardSquareColors());
    }

    // Then move all the body parts, starting from the furthest.
    for (int partIndex = this.bodyLength - 1; partIndex >= 0; partIndex--) {
      this.bodyParts[partIndex].move();
    }

    // Warp if the stage borders are disabled.
    if (bodyParts[0].isOutOfBounds() && !game.stageBorders)
      if (this.bodyParts[0].getX() < 0)
        this.bodyParts[0].warp(this.game.getHorizontalSquares() - 1, this.bodyParts[0].getY());
      else if (this.bodyParts[0].getX() > this.game.getHorizontalSquares() - 1)
        this.bodyParts[0].warp(0, this.bodyParts[0].getY());
      else if (this.bodyParts[0].getY() < 0)
        this.bodyParts[0].warp(this.bodyParts[0].getX(), this.game.getVerticalSquares() - 1);
      else if (this.bodyParts[0].getY() > this.game.getVerticalSquares() - 1)
        this.bodyParts[0].warp(this.bodyParts[0].getX(), 0);

    this.previousDirection = this.direction;
    return true;
  }

  public boolean changeDirection(Direction pressedDirection) {
    // Check if the direction inputted is useless (will not change the snake's next direction).
    if (pressedDirection.equals(this.direction)) return false;

    // Check if the direction is invalid (opposite of the direction of the previous move).
    Direction oppositeDirection;
    switch (this.previousDirection) {
      case UP: oppositeDirection = DOWN; break;
      case DOWN: oppositeDirection = UP; break;
      case LEFT: oppositeDirection = Direction.RIGHT; break;
      case RIGHT: oppositeDirection = Direction.LEFT; break;
      default: oppositeDirection = UP; break;
    }
    if (pressedDirection.equals(oppositeDirection)) return false;

    this.direction = pressedDirection;
    this.vibrator.vibrate(40);
    return true;
  }

  public void updateColors() {
    for (int partIndex = 0; partIndex < this.bodyLength; partIndex++)
      this.bodyParts[partIndex].updateColors();
  }

  public void draw(double dt) {
    // TODO
  }

  public void expandBody() {
    // Expand the bodyParts array and increase bodyLength.
    BodyPart[] holder = this.bodyParts;
    this.bodyParts = new BodyPart[this.bodyLength + 1];
    for (int index = 0; index < holder.length; index++) this.bodyParts[index] = holder[index];

    this.bodyParts[this.bodyLength] = new BodyPart(this);
    this.bodyLength++;
  }

  public void expandBody(int x, int y) {
    // Expand the bodyParts array and increase bodyLength.
    BodyPart[] holder = this.bodyParts;
    this.bodyParts = new BodyPart[this.bodyLength + 1];
    for (int index = 0; index < holder.length; index++) this.bodyParts[index] = holder[index];

    this.bodyParts[this.bodyLength] = new BodyPart(this, x, y);
    this.bodyLength++;
  }

  public void expandBody(Direction direction) {
    // Expand the bodyParts array and increase bodyLength.
    BodyPart[] holder = this.bodyParts;
    this.bodyParts = new BodyPart[this.bodyLength + 1];
    for (int index = 0; index < holder.length; index++) this.bodyParts[index] = holder[index];
    System.out.println(this.bodyLength + " " + this.bodyParts.length);
    this.bodyParts[this.bodyLength] = new BodyPart(this, direction);
    this.bodyLength++;
  }

  public void checkDeath() {
    // Check if you've hit a wall. Warp if stageBorders is off, die otherwise.
    if (this.bodyParts[0].isOutOfBounds() && this.game.stageBorders)
      this.die();

    // Check if you've hit anybody's body.
    for (Player player : game.getPlayers()) {
      if (player.isAlive())
        for (BodyPart bodyPart : player.getBodyParts())
          if (bodyPart != this.bodyParts[0]
              && this.bodyParts[0].getX() == bodyPart.getX()
              && this.bodyParts[0].getY() == bodyPart.getY())
            this.die();
    }
  }

  public void die() {
    this.vibrator.vibrate(100);
    this.alive = false;
    // Check if anybody else should die from you.
    for (Player player : game.getPlayers())
      if (player != this && player.isAlive())
        for (BodyPart bodyPart : this.bodyParts)
          if (player.getX() == bodyPart.getX() && player.getY() == bodyPart.getY())
            player.die();
  }

  public void increaseScore(int amount) {
    this.score += amount;
  }

  public boolean isAlive() { return this.alive; }
  public boolean isDrawable() { return this.drawable; }
  public boolean isFlashing() { return this.flashing; }
  public boolean isOutOfBounds() { return this.bodyParts[0].isOutOfBounds(); }
  public int getX() { return this.bodyParts[0].getX(); }
  public int getY() { return this.bodyParts[0].getY(); }
  public Direction getDirection() { return this.direction; }

  public int getNumber() { return this.number; }
  public String getName() { return this.name; }
  public Game getGame() { return this.game; }
  public Mesh getBoardSquares() { return this.boardSquares; }

  public int getScore() { return this.score; }
  public ControlType getControlType() { return this.controlType; }
  public CornerLayout getCornerLayout() { return this.cornerLayout; }
  public CornerLayout.Corner getControlCorner() { return this.cornerLayout.getCorner(); }

  public BodyPart getBodyPart(int bodyPartIndex) {
    if (bodyPartIndex >= 0)
      return this.bodyParts[bodyPartIndex];
    else
      return this.bodyParts[this.bodyLength + bodyPartIndex];
  }

  public BodyPart[] getBodyParts() { return this.bodyParts; }
  public int getBodyLength() { return this.bodyLength; }

  public float[] getColors() { return this.colors; }
  public int getColorIndex() { return this.colorIndex; }
  public float[] getBodyColors() {
    float[] bodyColors = {this.colors[0] / 2f, this.colors[1] / 2f,
                          this.colors[2] / 2f, this.colors[3]};
    return bodyColors;
  }

  public int getOnlineIdentifier() { return this.onlineIdentifier; }

  // Setters
  public void setControlType(ControlType type) { this.controlType = type; }
  public void setCornerLayout(CornerLayout.Corner corner) {
    this.cornerLayout = new CornerLayout(this, corner);
  }
  public void setName(String name) { this.name = name; }
  public void setColors(int colorIndex) {
    this.colors = Menu.getColorFromIndex(colorIndex);
    this.colorIndex = colorIndex;
  }
  public void setOnlineIdentifier(int id) { this.onlineIdentifier = id; }

  // Static methods
  public static Direction getOppositeDirection(Direction direction) {
    switch (direction) {
      case UP: return DOWN;
      case DOWN: return UP;
      case LEFT: return RIGHT;
      case RIGHT: return LEFT;
      default: return null;
    }
  }
}
