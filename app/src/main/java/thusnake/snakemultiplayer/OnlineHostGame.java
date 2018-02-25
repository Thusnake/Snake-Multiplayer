package thusnake.snakemultiplayer;

/**
 * Created by Nick on 24/02/2018.
 */

public class OnlineHostGame extends Game {

  // Constructor.
  public OnlineHostGame(GameRenderer renderer, int screenWidth, int screenHeight, Player[] players){
    super(renderer, screenWidth, screenHeight, players);
    this.sendBytes(new byte[] {Protocol.START_GAME});
  }

  @Override
  protected boolean checkGameOver() {
    if (super.checkGameOver()) {
      this.sendBytes(new byte[] {Protocol.END_GAME, (byte) this.assessWinner()});
      return true;
    }
    return false;
  }

  @Override
  protected void moveAllSnakes() {
    super.moveAllSnakes();
    Player[] players = this.getPlayers();
    this.sendBytes(new byte[] {Protocol.GAME_MOVEMENT_OCCURED, Protocol.getMovementCode(
        (players[0] != null) ? players[0].getDirection() : Player.Direction.UP,
        (players[1] != null) ? players[1].getDirection() : Player.Direction.UP,
        (players[2] != null) ? players[2].getDirection() : Player.Direction.UP,
        (players[3] != null) ? players[3].getDirection() : Player.Direction.UP
    )});
  }

  @Override
  public void handleInputBytes(byte[] bytes) {

  }
}
