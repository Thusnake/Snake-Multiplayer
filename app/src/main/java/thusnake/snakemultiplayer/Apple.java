package thusnake.snakemultiplayer;

/**
 * Created by vency on 4.3.2016.
 */
public class Apple extends Entity {
  private Coordinates<Integer> nextPosition;

  public Apple(Game game, int x, int y) {
    super(game, x, y, new float[] {1f, 1f, 1f, 1f}, new int[] {0, 0, 4, 4});
  }

  @Override
  public void onHit(Player player) {
    feed(player);
    findNewPosition();
    game.onAppleEaten(this);
  }

  public void feed(Player player) {
    player.expandBody();
    player.increaseScore(1);
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
