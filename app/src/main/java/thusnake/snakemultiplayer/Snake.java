package thusnake.snakemultiplayer;

import android.support.annotation.NonNull;
import android.view.MotionEvent;

import java.util.Iterator;
import java.util.LinkedList;

import thusnake.snakemultiplayer.SnakeSkin.TextureType;
import thusnake.snakemultiplayer.controllers.Controller;
import thusnake.snakemultiplayer.controllers.ControllerBuffer;
import thusnake.snakemultiplayer.textures.TextureMapCoordinates;

import static thusnake.snakemultiplayer.controllers.ControllerBuffer.Corner.UPPER_LEFT;
import static thusnake.snakemultiplayer.controllers.ControllerBuffer.Corner.UPPER_RIGHT;
import static thusnake.snakemultiplayer.Snake.Direction.DOWN;
import static thusnake.snakemultiplayer.Snake.Direction.LEFT;
import static thusnake.snakemultiplayer.Snake.Direction.RIGHT;
import static thusnake.snakemultiplayer.Snake.Direction.UP;

public class Snake implements Iterable<BodyPart> {
  public enum Direction {UP, DOWN, LEFT, RIGHT}
  private Snake.Direction direction, previousDirection;
  private boolean alive = true, drawable = true, flashing = false;
  private int score;
  private BodyPart head, tail;
  private final GameRenderer renderer = OpenGLActivity.current.getRenderer();

  public final Game game;
  public final Player player;
  public final Controller controller;
  public final ControllerBuffer.Corner corner;
  public final SnakeSkin skin;

  public Snake(Game game, Player player) {
    this.game = game;
    this.player = player;

    skin = player.getSkin();
    corner = player.getControlCorner();
    if (corner == UPPER_LEFT || corner == UPPER_RIGHT) this.direction = DOWN;
    else                                               this.direction = UP;
    controller = player.getControllerBuffer().constructController(game, this);
    previousDirection = this.direction;
    score = 0;

    switch (this.getControlCorner()) {
      case LOWER_LEFT:
        head = new BodyPart(this, 0, 0); break;
      case LOWER_RIGHT:
        head = new BodyPart(this, game.getHorizontalSquares() - 1, 0); break;
      case UPPER_LEFT:
        head = new BodyPart(this, 0, game.getVerticalSquares() - 1); break;
      case UPPER_RIGHT:
        head = new BodyPart(this, game.getHorizontalSquares()-1, game.getVerticalSquares()-1);break;
    }

    tail = head;

    for (int index = 1; index < 4; index++)
      tail = new BodyPart(this, tail, Snake.getOppositeDirection(direction));
  }

  /** Performs a move in its current direction. */
  public void move() {
    // First remove the color from the furthest body part.
    if (!tail.isOutOfBounds()) {
      game.getBoardSquares().updateColors(tail.getX(), tail.getY(), game.getBoardSquareColors());
      game.getBoardSquares().updateTextures(tail.getX(), tail.getY(), (TextureMapCoordinates) null);
    }

    // Then move all the body parts. The head's move() will make the rest of the body move as well.
    head.move();

    // Warp if the stage borders are disabled.
    if (head.isOutOfBounds() && !game.stageBorders)
      if (head.getX() < 0)
        head.warp(game.getHorizontalSquares() - 1, head.getY());
      else if (head.getX() > game.getHorizontalSquares() - 1)
        head.warp(0, head.getY());
      else if (head.getY() < 0)
        head.warp(head.getX(), game.getVerticalSquares() - 1);
      else if (head.getY() > game.getVerticalSquares() - 1)
        head.warp(head.getX(), 0);

    previousDirection = direction;
  }

  /**
   * Changes the direction of the snake.
   * @return True if the direction was changed, False if it stayed the same.
   */
  public boolean changeDirection(Snake.Direction pressedDirection) {
    // Check if the direction inputted is useless (will not change the snake's next direction).
    if (pressedDirection.equals(direction)) return false;

    // Check if the direction is invalid (opposite of the direction of the previous move).
    Snake.Direction oppositeDirection = Snake.getOppositeDirection(previousDirection);
    if (pressedDirection.equals(oppositeDirection)) return false;

    direction = pressedDirection;

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

  /**
   * Draws the snake to the game's mesh.
   * Note: This currently draws the whole snake, which is not the most efficient way to do it, but
   * it's safe.
   */
  public void drawToMesh() {
    for (BodyPart bodyPart : this)
      bodyPart.updateColors();

    // Update the texture as well.
    // First the body.
    for (BodyPart bodyPart : this) {
      if (!bodyPart.isOutOfBounds() && bodyPart != head && bodyPart != tail) {
        boolean turning = !(bodyPart.getForwardPart().getX() == bodyPart.getBackwardPart().getX()
            || bodyPart.getForwardPart().getY() == bodyPart.getBackwardPart().getY());
        Snake.Direction direction;

        // Find the directional texture to be used.
        if (turning) {
          LinkedList<Direction> directions = new LinkedList<>();
          directions.add(bodyPart.adjacentDirection(bodyPart.getForwardPart()));
          directions.add(bodyPart.adjacentDirection(bodyPart.getBackwardPart()));
          if (directions.contains(UP) && directions.contains(RIGHT)) direction = DOWN;
          else if (directions.contains(LEFT) && directions.contains(UP)) direction = RIGHT;
          else if (directions.contains(LEFT) && directions.contains(DOWN)) direction = UP;
          else if (directions.contains(DOWN) && directions.contains(RIGHT)) direction = LEFT;
          else throw new RuntimeException("Snake calculated to be turning when in fact it isn't.");
        } else direction = bodyPart.adjacentDirection(bodyPart.getForwardPart());

        game.getBoardSquares().updateTextures(bodyPart.getX(), bodyPart.getY(),
                                    game.getBoardSquares().textureMap
                                        .getTexture(skin,
                                                    turning ? TextureType.TURN : TextureType.BODY,
                                                    direction));
      }
    }

    // Then the tail.
    BodyPart nextPart = tail.getForwardPart();
    Snake.Direction tailDirection;
    if (getBodyLength() > 1 && !tail.isOutOfBounds()) {
      do {
        tailDirection = tail.adjacentDirection(nextPart);
        nextPart = nextPart.getForwardPart();
      } while (tailDirection == null);

      game.getBoardSquares().updateTextures(tail.getX(), tail.getY(),
                                            game.getBoardSquares()
                                                .textureMap.getTexture(skin, TextureType.TAIL,
                                                                       tailDirection));
    }

    // And finally the head.
    if (head != null && !head.isOutOfBounds())
      game.getBoardSquares().updateTextures(getX(), getY(),
                                            game.getBoardSquares()
                                                .textureMap.getTexture(skin, TextureType.HEAD,
                                                                       previousDirection));
  }

  public void expandBody() {
    tail = new BodyPart(this, tail);
  }

  /**
   * Performs the checks for whether this snake is supposed to die and calls the die() method if it
   * is.
   */
  public boolean checkDeath() {
    // Check if you've hit a wall. Warp if stageBorders is off, die otherwise.
    if (head.isOutOfBounds() && this.game.stageBorders) {
      this.die();
      return true;
    }

    // Check if you've hit anybody's body.
    for (Snake snake : game.getAliveSnakes())
      for (BodyPart bodyPart : snake)
        if (bodyPart != head && head.getX() == bodyPart.getX() && head.getY() == bodyPart.getY()) {
          die();
          return true;
        }

    return false;
  }

  public void die() {
    renderer.getOriginActivity().vibrator.vibrate(100);
    alive = false;
    // Check if anybody else should die from you.
    for (Snake snake : game.getAliveSnakes())
      for (BodyPart bodyPart : this)
        if (snake.getX() == bodyPart.getX() && snake.getY() == bodyPart.getY())
          snake.die();
  }

  public void increaseScore(int amount) {
    this.score += amount;
  }

  public void onMotionEvent(MotionEvent event) { this.controller.onMotionEvent(event); }

  public boolean isAlive() { return this.alive; }
  public boolean isDrawable() { return this.drawable; }
  public boolean isFlashing() { return this.flashing; }
  public boolean isOutOfBounds() { return head.isOutOfBounds(); }
  public int getX() { return head.getX(); }
  public int getY() { return head.getY(); }
  public Snake.Direction getDirection() { return this.direction; }
  public Snake.Direction getPreviousDirection() { return this.previousDirection; }
  public BoardDrawer getGame() { return this.game; }
  public int getScore() { return this.score; }
  public String getName() { return player.getName(); }
  public SnakeSkin getSkin() { return skin; }

  public ControllerBuffer.Corner getControlCorner() { return corner; }

  @NonNull
  @Override
  public Iterator<BodyPart> iterator() {
    return new Iterator<BodyPart>() {
      BodyPart lastPart = null;

      @Override
      public boolean hasNext() {
        return lastPart == null ? head != null : lastPart.getBackwardPart() != null;
      }

      @Override
      public BodyPart next() {
        return lastPart = (lastPart == null ? head : lastPart.getBackwardPart());
      }
    };
  }

  public int getBodyLength() {
    BodyPart current = head;
    int count = 1;
    while(current != tail) {
      current = current.getBackwardPart();
      count++;
    }
    return count;
  }

  // Static methods.
  static Direction getOppositeDirection(Direction direction) {
    switch (direction) {
      case UP: return DOWN;
      case DOWN: return UP;
      case LEFT: return RIGHT;
      case RIGHT: return LEFT;
      default: throw new RuntimeException("Passed null to Player.getOppositeDirection()");
    }
  }
}
