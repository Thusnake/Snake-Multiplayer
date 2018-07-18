package thusnake.snakemultiplayer;

public class TronGame extends Game {
  public TronGame(GameRenderer renderer, GameSetupBuffer setupBuffer) {
    super(renderer, setupBuffer);
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
