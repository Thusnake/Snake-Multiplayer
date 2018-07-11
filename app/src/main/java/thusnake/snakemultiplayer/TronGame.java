package thusnake.snakemultiplayer;

public class TronGame extends Game {
  public TronGame(GameRenderer renderer, int screenWidth, int screenHeight, Player[] players) {
    super(renderer, screenWidth, screenHeight, players);
  }

  @Override
  public void createApples() {
    getApples().clear();
  }

  @Override
  protected void moveAllSnakes() {
    for (Player player : getPlayers())
      player.expandBody();
    super.moveAllSnakes();
  }
}
