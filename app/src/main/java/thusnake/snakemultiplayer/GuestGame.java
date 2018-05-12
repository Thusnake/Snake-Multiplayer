package thusnake.snakemultiplayer;

import android.util.Pair;

public class GuestGame extends Game {
  private OpenGLES20Activity originActivity;
  private int moveCount = 0;
  private MissedMovesList missedMovesList;

  public GuestGame(GameRenderer renderer, int screenWidth, int screenHeight, Player[] players) {
    super(renderer, screenWidth, screenHeight, players);

    this.originActivity = (OpenGLES20Activity) renderer.getContext();
  }

  @Override
  public void run(double dt) {
    super.run(dt);

    // Check if there is something to be emptied from the missed moves list.
    if (missedMovesList.size() > 0 && missedMovesList.firstIsReady())
      this.decodeAndExecuteMove(missedMovesList.extractFirst());
  }

  // This doesn't move all the snakes at all, but we're using the fact that it's invoked on
  // a timer to request snake moves.
  @Override
  public void moveAllSnakes() {
    if (missedMovesList.size() > 0)
      for (Integer index : missedMovesList.missingMovesIndices()) {
        Pair<Byte, Byte> idBytes = Protocol.encodeMoveID(index);
        this.sendBytes(new byte[] {Protocol.REQUEST_MOVE, idBytes.first, idBytes.second});
      }
  }

  private void decodeAndExecuteMove(byte encodedMove) {
    Player.Direction[] directions = new Player.Direction[4];
    Protocol.decodeMovementCode(encodedMove, directions);
    for (int index = 0; index < 4; index++)
      if (this.getPlayers()[index] != null && this.getPlayers()[index].isAlive())
        this.getPlayers()[index].changeDirection(directions[index]);
    // Move all the snakes.
    for (Player player : this.getPlayers())
      if (player != null && player.isAlive())
        player.move();
    // Update the counter.
    moveCount++;
  }

  @Override
  public void handleInputBytes(byte[] inputBytes, ConnectedThread source) {
    switch (inputBytes[0]) {
      case Protocol.GAME_MOVEMENT_OCCURRED:
        int moveId = inputBytes[1] + (inputBytes[2] << 8);
        if (moveId - moveCount == 1) {
          // The move is correct, as it follows the last we know of. Execute it.
          this.decodeAndExecuteMove(inputBytes[3]);
        } else if (moveId - moveCount > 1) {
          // We've probably missed a move (or more) and so we'll send a request.
          int missedMoves = moveId - moveCount - 1;
          for (int missedMoveIndex = 0; missedMoveIndex < missedMoves; missedMoveIndex++) {
            Pair<Byte, Byte> idBytes = Protocol.encodeMoveID(moveCount + missedMoveIndex + 1);
            this.sendBytes(new byte[] {Protocol.REQUEST_MOVE, idBytes.first, idBytes.second});
          }
          // Create a list (or expand the current one) of missed moves.
          if (this.missedMovesList == null)
            this.missedMovesList = new MissedMovesList(moveCount, moveId, inputBytes);
          else
            this.missedMovesList.expand(moveId);
        }
        break;
      case Protocol.GAME_MOVEMENT_INFORMATION:
        moveId = Protocol.decodeMoveID(inputBytes[1], inputBytes[2]);
        if (moveId > moveCount) {
          // The move is ahead of our information.
          missedMovesList.expand(moveId, inputBytes);
        } else {
          // The move might be relevant, put it in the list.
          missedMovesList.insert(moveId, inputBytes);
        }
        break;
      case Protocol.GAME_MOVEMENT_MISSING:
        break;
      default:
        break;
    }
  }

  public void handleInputBytes(byte[] inputBytes) {
    this.handleInputBytes(inputBytes, originActivity.connectedThread);
  }

  public void sendBytes(byte[] bytes) {
    // TODO
  }
}
