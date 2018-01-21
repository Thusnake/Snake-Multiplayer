package thusnake.snakemultiplayer;

import android.content.Context;
import android.os.Vibrator;

/**
 * Created by Nick on 12/12/2017.
 */

public class Player {
  public enum Direction {UP, DOWN, LEFT, RIGHT}
  private Direction direction, previousDirection;
  private boolean alive, drawable, flashing;
  private int number, score;
  private final String name;
  private final ControlType controlType;
  public enum ControlType {OFF, CORNER, SWIPE, KEYBOARD, GAMEPAD, BLUETOOTH, WIFI};
  private final CornerLayout cornerLayout;
  private BodyPart[] bodyParts = new BodyPart[0];
  private int bodyLength = 0;
  private float[] colors = new float[4];
  private final Game game;
  private final Vibrator vibrator;
  private final Mesh boardSquares;

  // Constructor for a corner layout player.
  public Player(Game game, String name) {
    this.game = game;
    this.vibrator = (Vibrator) game.getRenderer().getContext()
        .getSystemService(Context.VIBRATOR_SERVICE);
    this.boardSquares = game.getBoardSquares();
    this.direction = Direction.UP;
    this.previousDirection = this.direction;
    this.alive = true;
    this.drawable = true;
    this.flashing = false;
    this.name = name;
    this.colors[0] = 1;
    this.colors[1] = 1;
    this.colors[2] = 1;
    this.colors[3] = 1;
    this.score = 0;
    this.controlType = ControlType.CORNER;
    // TODO make the cornerlayout position dependant on control corner / player number.
    this.cornerLayout = new CornerLayout(this, CornerLayout.Corner.LOWER_LEFT);
    // TODO make the first body part's position dependant on control corner / player number.
    expandBody(0, 0);
    for (int index = 1; index < 4; index++) {
      expandBody(Direction.DOWN);
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

    this.previousDirection = this.direction;
    return true;
  }

  public boolean changeDirection(Direction pressedDirection) {
    // Check if the direction inputted is useless (will not change the snake's next direction).
    if (pressedDirection.equals(this.direction)) return false;

    // Check if the direction is invalid (opposite of the direction of the previous move).
    Direction oppositeDirection;
    switch (this.previousDirection) {
      case UP: oppositeDirection = Direction.DOWN; break;
      case DOWN: oppositeDirection = Direction.UP; break;
      case LEFT: oppositeDirection = Direction.RIGHT; break;
      case RIGHT: oppositeDirection = Direction.LEFT; break;
      default: oppositeDirection = Direction.UP; break;
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
    // Check if you've hit anybody's body.
    for (Player player : game.getPlayers()) {
      if (player.isAlive())
        for (BodyPart bodyPart : player.getBodyParts())
          if (bodyPart != this.bodyParts[0]
              && this.bodyParts[0].getX() == bodyPart.getX()
              && this.bodyParts[0].getY() == bodyPart.getY())
            this.die();
    }
    // Check if you've hit a wall. TODO check if wallDeath is on from properties.
    if (this.bodyParts[0].isOutOfBounds()) this.die();
  }

  public void die() {
    // Check if anybody else should die from you before declaring death.
    for (Player player : game.getPlayers())
      if (player != this && player.isAlive())
        for (BodyPart bodyPart : this.bodyParts)
          if (player.getX() == bodyPart.getX() && player.getY() == bodyPart.getY())
            player.die();
    this.vibrator.vibrate(100);
    this.alive = false;
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

  public BodyPart getBodyPart(int bodyPartIndex) {
    if (bodyPartIndex >= 0)
      return this.bodyParts[bodyPartIndex];
    else
      return this.bodyParts[this.bodyLength + bodyPartIndex];
  }

  public BodyPart[] getBodyParts() { return this.bodyParts; }
  public int getBodyLength() { return this.bodyLength; }

  public float[] getColors() { return this.colors; }
  public float[] getBodyColors() {
    float[] bodyColors = {this.colors[0] / 2f, this.colors[1] / 2f,
                          this.colors[2] / 2f, this.colors[3]};
    return bodyColors;
  }
}
