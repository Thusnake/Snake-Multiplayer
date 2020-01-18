package thusnake.snakemultiplayer;

/**
 * Created by vency on 4.3.2016.
 */
public class Apple extends Entity {
  private Coordinates<Integer> nextPosition;

  public Apple(Game game, int x, int y) {
    super(game, new float[] {1f, 1f, 1f, 1f}, R.drawable.apple);
    setPosition(x, y);
  }

  public Apple(Game game, Coordinates<Integer> coordinates) {
    this(game, coordinates.x, coordinates.y);
  }

  public Apple(Game game) {
    this(game, game.getRandomEmptySpace());
  }

  @Override
  public void onHit(Snake snake) {
    feed(snake);
    findNewPosition();
    game.onAppleEaten(this);
  }

  public void feed(Snake snake) {
    snake.expandBody();
    snake.increaseScore(1);
  }

  public void findNewPosition() {
    if (nextPosition != null) {
      // If there is a predetermined next position, don't bother searching for a new one.
      this.x = nextPosition.x;
      this.y = nextPosition.y;
      // Clear out the next position coordinates.
      nextPosition = null;
    } else {
      Coordinates<Integer> randomEmptySpace = game.getRandomEmptySpace();
      this.x = randomEmptySpace.x;
      this.y = randomEmptySpace.y;
    }
  }

  public int getX() { return x; }
  public int getY() { return y; }

  public void setNextPosition(int x, int y) { nextPosition = new Coordinates<>(x, y); }
}
