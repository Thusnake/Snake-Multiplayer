package thusnake.snakemultiplayer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static thusnake.snakemultiplayer.Player.Direction.DOWN;
import static thusnake.snakemultiplayer.Player.Direction.LEFT;
import static thusnake.snakemultiplayer.Player.Direction.RIGHT;
import static thusnake.snakemultiplayer.Player.Direction.UP;

/**
 * Created by Nick on 12/12/2017.
 */

public class Player implements Iterable<BodyPart> {
  public enum Direction {UP, DOWN, LEFT, RIGHT}
  private Direction direction, previousDirection;
  private boolean alive = false, drawable = false, flashing;
  private GameRenderer renderer;
  private int score;
  private String name = "Snake";
  private PlayerController playerController;
  private BodyPart head, tail;
  private SnakeSkin skin = SnakeSkin.white;
  private BoardDrawer game;
  private Mesh boardSquares;
  private final List<PlayerController> controllersCache = new LinkedList<>();

  public Player(GameRenderer renderer) {
    this.renderer = renderer;
    this.setSkin(SnakeSkin.white);
  }

  public Player defaultPreset(GameSetupBuffer setupBuffer) {
    // Find a suitable unique name of the format "Player N".
    int playerIndex = 1;
    boolean indexChanged = true;
    while (indexChanged) {
      indexChanged = false;
      for (PlayerController.Corner corner : PlayerController.Corner.values())
        if (setupBuffer.getCornerMap().getPlayer(corner) != null &&
            setupBuffer.getCornerMap().getPlayer(corner).getName().equals("Player "+ playerIndex)) {
          playerIndex++;
          indexChanged = true;
        }
    }
    setName("Player " + playerIndex);

    // Set the default controller.
    setController(new CornerLayoutController(renderer, this));

    return this;
  }

  /**
   * Loads the last settings saved for this player's controller and assigns that controller.
   * To be called before loading any controllers as it caches a bunch of them.
   * @param setupBuffer The buffer to use for loading.
   */
  public void loadSavedController(GameSetupBuffer setupBuffer) {
    // Cache the default controllers.
    setController(new SwipeController(renderer, this).loadSettings(renderer.getOriginActivity(), setupBuffer));
    setController(new GamepadController(renderer, this).loadSettings(renderer.getOriginActivity(), setupBuffer));
    setController(new CornerLayoutController(renderer, this).loadSettings(renderer.getOriginActivity(), setupBuffer));

    // Select the one that has been saved to the shared preferences last.
    for (PlayerController controller : controllersCache)
      if (controller.identifier().equals(renderer.getOriginActivity()
          .getSharedPreferences("settings", Context.MODE_PRIVATE)
          .getString(setupBuffer.savingPrefix+"-"+getControlCorner().toString()+"-controllerid",
                     null))) {
        controller.loadSettings(renderer.getOriginActivity(), setupBuffer);
        setController(controller);
      }
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
      tail = new BodyPart(this, tail, getOppositeDirection(direction));

    playerController.prepareForGame();
  }

  public boolean move() {
    // First remove the color from the furthest body part.
    if (!tail.isOutOfBounds()) {
      boardSquares.updateColors(tail.getX(), tail.getY(), game.getBoardSquareColors());
      boardSquares.updateTextures(tail.getX(), tail.getY(), 31, 0, 31, 0);
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

  public void drawToMesh() {
    for (BodyPart bodyPart : this)
      bodyPart.updateColors();

    // Update the texture as well.
    // First the body.
    for (BodyPart bodyPart : this) {
      if (!bodyPart.isOutOfBounds() && bodyPart != head && bodyPart != tail) {
        boolean turning = !(bodyPart.getForwardPart().getX() == bodyPart.getBackwardPart().getX()
            || bodyPart.getForwardPart().getY() == bodyPart.getBackwardPart().getY());
        Direction direction;

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

        boardSquares.updateTextures(bodyPart.getX(), bodyPart.getY(),
                                    skin.texture(turning ? SnakeSkin.TextureType.TURN :
                                                           SnakeSkin.TextureType.BODY, direction));
      }
    }

    // Then the tail.
    BodyPart nextPart = tail.getForwardPart();
    Direction tailDirection;
    if (getBodyLength() > 1 && !tail.isOutOfBounds()) {
      do {
        tailDirection = tail.adjacentDirection(nextPart);
        nextPart = nextPart.getForwardPart();
      } while (tailDirection == null);

      boardSquares.updateTextures(tail.getX(), tail.getY(),
                                  skin.texture(SnakeSkin.TextureType.TAIL, tailDirection));
    }

    // And finally the head.
    if (head != null && !head.isOutOfBounds())
      boardSquares.updateTextures(getX(), getY(), skin.texture(SnakeSkin.TextureType.HEAD,
          previousDirection));
  }

  public void draw(double dt) {
    // TODO
  }

  public void expandBody() {
    tail = new BodyPart(this, tail);
  }

  public void checkDeath() {
    // Check if you've hit a wall. Warp if stageBorders is off, die otherwise.
    if (head.isOutOfBounds() && this.game.stageBorders)
      this.die();

    // Check if you've hit anybody's body.
    for (Player player : game.getPlayers()) {
      if (player.isAlive())
        for (BodyPart bodyPart : player)
          if (bodyPart != head && head.getX() == bodyPart.getX() && head.getY() == bodyPart.getY())
            die();
    }
  }

  public void die() {
    renderer.getOriginActivity().vibrator.vibrate(100);
    this.alive = false;
    // Check if anybody else should die from you.
    for (Player player : game.getPlayers())
      if (player != this && player.isAlive())
        for (BodyPart bodyPart : this)
          if (player.getX() == bodyPart.getX() && player.getY() == bodyPart.getY())
            player.die();
  }

  public void increaseScore(int amount) {
    this.score += amount;
  }

  public boolean isAlive() { return this.alive; }
  public boolean isDrawable() { return this.drawable; }
  public boolean isFlashing() { return this.flashing; }
  public boolean isOutOfBounds() { return head.isOutOfBounds(); }
  public int getX() { return head.getX(); }
  public int getY() { return head.getY(); }
  public Direction getDirection() { return this.direction; }
  public Direction getPreviousDirection() { return this.previousDirection; }

  public String getName() { return this.name; }
  public BoardDrawer getGame() { return this.game; }

  public int getScore() { return this.score; }
  public PlayerController getPlayerController() { return this.playerController; }
  public PlayerController.Corner getControlCorner() {
    return renderer.getMenu().getSetupBuffer().getCornerMap().getPlayerCorner(this);
  }
  public PlayerController.Corner getControlCorner(GameSetupBuffer setupBuffer) {
    return setupBuffer.getCornerMap().getPlayerCorner(this);
  }

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
    int length = 0;
    for (BodyPart bodyPart : this) length++;
    return length;
  }

  public void setSkin(SnakeSkin skin) { this.skin = skin; }
  public SnakeSkin getSkin() { return skin; }
  public int getSkinIndex() { return SnakeSkin.allSkins.indexOf(skin); }

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

  // Static methods.
  private static Direction getOppositeDirection(Direction direction) {
    switch (direction) {
      case UP: return DOWN;
      case DOWN: return UP;
      case LEFT: return RIGHT;
      case RIGHT: return LEFT;
      default: throw new RuntimeException("Passed null to Player.getOppositeDirection()");
    }
  }
}
