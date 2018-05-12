package thusnake.snakemultiplayer;

import android.content.Context;
import android.util.Pair;

import com.android.texample.GLText;
import javax.microedition.khronos.opengles.GL10;

public class GuestGame extends Game {
  private OpenGLES20Activity originActivity;
  private int moveCount = 0;
  private MissedMovesList missedMovesList;

  public GuestGame(GameRenderer renderer, int screenWidth, int screenHeight, Player[] players) {
    super(renderer, screenWidth, screenHeight, players);

    this.originActivity = (OpenGLES20Activity) renderer.getContext();
  }

  // This doesn't move all the snakes at all, but we're using the fact that it's invoked on
  // a timer to request snake moves.
  @Override
  public void moveAllSnakes() {
    if (missedMovesList.size() > 0)
      for (Integer index : missedMovesList.missingMovesIndeces()) {
        Pair<Byte, Byte> idBytes = Protocol.encodeMoveID(index);
        this.sendBytes(new byte[] {Protocol.REQUEST_MOVE, idBytes.first, idBytes.second});
      }
  }

  @Override
  public void handleInputBytes(byte[] inputBytes, ConnectedThread source) {
    switch (inputBytes[0]) {
      case Protocol.GAME_MOVEMENT_OCCURRED:
        int moveId = inputBytes[1] + (inputBytes[2] << 8);
        if (moveId - moveCount == 1) {
          // Load the directions in an array and apply them to each player.
          Player.Direction[] directions = new Player.Direction[4];
          Protocol.decodeMovementCode(inputBytes[3], directions);
          for (int index = 0; index < 4; index++)
            if (this.getPlayers()[index] != null && this.getPlayers()[index].isAlive())
              this.getPlayers()[index].changeDirection(directions[index]);
          // Move all the snakes.
          for (Player player : this.getPlayers())
            if (player != null && player.isAlive())
              player.move();
          // Update the counter.
          moveCount++;
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
