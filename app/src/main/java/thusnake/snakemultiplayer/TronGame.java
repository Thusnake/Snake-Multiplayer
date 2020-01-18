package thusnake.snakemultiplayer;

public class TronGame extends Game {
  public TronGame(CornerMap cornerMap, int horizontalSquares, int verticalSquares, int speed,
                  boolean stageBorders) {
    super(cornerMap, horizontalSquares, verticalSquares, speed, stageBorders, null);
  }

  @Override
  protected void performMove() {
    for (Snake snake: getAliveSnakes())
      snake.expandBody();
    super.performMove();
  }
}
