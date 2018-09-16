package thusnake.snakemultiplayer;

import android.content.Context;
import android.os.Vibrator;
import android.view.MotionEvent;

import java.util.LinkedList;
import java.util.List;

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
  private boolean alive = false, drawable = false, flashing;
  private GameRenderer renderer;
  private int number, score;
  private String name = "Snake";
  private ControlType controlType;
  public enum ControlType {OFF, CORNER, SWIPE, KEYBOARD, GAMEPAD, BLUETOOTH, WIFI}
  private PlayerController playerController;
  private BodyPart[] bodyParts = new BodyPart[0];
  private int bodyLength = 0;
  private SnakeSkin skin = SnakeSkin.white;
  private BoardDrawer game;
  private Mesh boardSquares;
  private final List<PlayerController> controllersCache = new LinkedList<>();

  // Constructor for a corner layout player.
  public Player(GameRenderer renderer, int number) {
    this.renderer = renderer;
    this.controlType = ControlType.OFF;
    this.number = number;
    this.setSkin(SnakeSkin.white);
  }

  public Player defaultPreset() {
    setName("Player " + (number + 1));
    setController(new CornerLayoutController(renderer, this));
    return this;
  }

  // Gets called upon game start.
  public void prepareForGame(BoardDrawer game) {
    this.game = game;
    this.boardSquares = game.getBoardSquares();
    if (this.getControlCorner() == PlayerController.Corner.UPPER_LEFT
        || this.getControlCorner() == PlayerController.Corner.UPPER_RIGHT) this.direction = DOWN;
    else                                                               this.direction = UP;
    this.previousDirection = this.direction;
    this.alive = true;
    this.drawable = true;
    this.flashing = false;
    this.score = 0;

    this.bodyParts = new BodyPart[0];
    this.bodyLength = 0;
    switch (this.getControlCorner()) {
      case LOWER_LEFT:
        this.expandBody(0, 0); break;
      case LOWER_RIGHT:
        this.expandBody(game.getHorizontalSquares() - 1, 0); break;
      case UPPER_LEFT:
        this.expandBody(0, game.getVerticalSquares() - 1); break;
      case UPPER_RIGHT:
        this.expandBody(game.getHorizontalSquares() - 1, game.getVerticalSquares() - 1); break;
    }
    for (int index = 1; index < 4; index++) {
      this.expandBody(Player.getOppositeDirection(this.direction));
    }

    playerController.prepareForGame();
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

  public void onMotionEvent(MotionEvent event) { this.playerController.onMotionEvent(event); }

  public boolean changeDirection(Direction pressedDirection) {
    // Check if the direction inputted is useless (will not change the snake's next direction).
    if (pressedDirection.equals(this.direction)) return false;

    // Check if the direction is invalid (opposite of the direction of the previous move).
    Direction oppositeDirection = getOppositeDirection(previousDirection);
    if (pressedDirection.equals(oppositeDirection)) return false;

    this.direction = pressedDirection;

    // If you're a guest try to inform the host of the direction change as well.
    if (renderer.getOriginActivity().isGuest()) {
      renderer.getOriginActivity().connectedThread.write(new byte[] {
          Protocol.SNAKE_DIRECTION_CHANGE,
          Protocol.encodeCorner(getControlCorner()),
          Protocol.encodeDirection(getDirection())
      });
    }

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
    System.arraycopy(holder, 0, this.bodyParts, 0, holder.length);

    this.bodyParts[this.bodyLength] = new BodyPart(this);
    this.bodyLength++;
  }

  public void expandBody(int x, int y) {
    // Expand the bodyParts array and increase bodyLength.
    BodyPart[] holder = this.bodyParts;
    this.bodyParts = new BodyPart[this.bodyLength + 1];
    System.arraycopy(holder, 0, this.bodyParts, 0, holder.length);

    this.bodyParts[this.bodyLength] = new BodyPart(this, x, y);
    this.bodyLength++;
  }

  public void expandBody(Direction direction) {
    // Expand the bodyParts array and increase bodyLength.
    BodyPart[] holder = this.bodyParts;
    this.bodyParts = new BodyPart[this.bodyLength + 1];
    System.arraycopy(holder, 0, this.bodyParts, 0, holder.length);

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
    renderer.getOriginActivity().vibrator.vibrate(100);
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
  public Direction getPreviousDirection() { return this.previousDirection; }

  public int getNumber() { return this.number; }
  public String getName() { return this.name; }
  public BoardDrawer getGame() { return this.game; }
  public Mesh getBoardSquares() { return this.boardSquares; }

  public int getScore() { return this.score; }
  public ControlType getControlType() { return this.controlType; }
  public PlayerController getPlayerController() { return this.playerController; }
  public PlayerController.Corner getControlCorner() {
    return renderer.getMenu().getSetupBuffer().getCornerMap().getPlayerCorner(this);
  }

  public BodyPart getBodyPart(int bodyPartIndex) {
    if (bodyPartIndex >= 0)
      return this.bodyParts[bodyPartIndex];
    else
      return this.bodyParts[this.bodyLength + bodyPartIndex];
  }

  public BodyPart[] getBodyParts() { return this.bodyParts; }
  public int getBodyLength() { return this.bodyLength; }

  public void setSkin(SnakeSkin skin) { this.skin = skin; }
  public SnakeSkin getSkin() { return skin; }
  public int getSkinIndex() { return SnakeSkin.allSkins.indexOf(skin); }

  // Setters
  public void setControlType(ControlType type) { this.controlType = type; }

  /**
   * Sets the controller of this player to a controller of a given type.
   * @param controller An example controller of the wanted type. If a controller of this type
   *                   exists already in the cache then the cached controller will be used.
   *                   Otherwise the passed controller will be assigned to that player.
   */
  public void setController(PlayerController controller) {
    for (PlayerController cachedController : controllersCache)
      if (cachedController.getClass().equals(controller.getClass())) {
        playerController = cachedController;
        return;
      }

    playerController = controller;
    controllersCache.add(controller);
  }

  /**
   * Sets the player's controller to a passed PlayerController. It is recommended that you instead
   * use setController() as this method will not check the cache and will therefore constantly
   * create new controllers with default settings if implemented in the menu as an option.
   * @param controller The controller to be used.
   */
  public void setControllerForced(PlayerController controller) {
    playerController = controller;
  }

  public void setName(String name) { this.name = name; }

  // Static methods
  private static Direction getOppositeDirection(Direction direction) {
    switch (direction) {
      case UP: return DOWN;
      case DOWN: return UP;
      case LEFT: return RIGHT;
      case RIGHT: return LEFT;
      default: return null;
    }
  }
}
