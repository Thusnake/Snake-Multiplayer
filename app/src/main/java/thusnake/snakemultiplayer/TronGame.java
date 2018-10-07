package thusnake.snakemultiplayer;

public class TronGame extends Game {
  public TronGame(GameRenderer renderer, GameSetupBuffer setupBuffer) {
    super(renderer, setupBuffer);
  }

  @Override
  protected void performMove() {
    for (Player player : getPlayers())
      player.expandBody();
    super.performMove();
  }
}
