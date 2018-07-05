package thusnake.snakemultiplayer;

import android.util.Pair;

import javax.microedition.khronos.opengles.GL10;

public class GuestGame extends Game {
  private OpenGLES20Activity originActivity;
  private int moveCount = 0;
  private MissedMovesList missedMovesList;
  private final Square readyFillBar;

  public GuestGame(GameRenderer renderer, int screenWidth, int screenHeight, Player[] players) {
    super(renderer, screenWidth, screenHeight, players);

    this.originActivity = (OpenGLES20Activity) renderer.getContext();

    // Change the top game over button to the ready button.
    this.getGameOverTopItem().setText("Ready");
    this.getGameOverTopItem().setAction((action, origin) -> {
      if (originActivity.isReady()) {
        originActivity.setReady(false);
        MenuItem originItem = (MenuItem) origin;
        originItem.setText("Ready");
      } else {
        originActivity.setReady(true);
        MenuItem originItem = (MenuItem) origin;
        originItem.setText("Cancel");
      }
    });

    readyFillBar = new Square(0, getScreenHeight()*2/3,
        getScreenWidth(), getScreenHeight()/3) {
      private int readyDevices = -1;
      private int connectedDevices = -1;

      @Override
      public void draw(GL10 gl) {
        // Flash from 0.1 opacity to 0.4 opacity.
        gl.glColor4f(1f,1f,1f,(float) Math.sin(getGameOverTimer().getTime() * 4) / 7.5f + 0.25f);
        super.draw(gl);

        if (originActivity.getNumberOfReadyRemoteDevices() != readyDevices
            || originActivity.getNumberOfRemoteDevices() != connectedDevices) {
          this.setCoordinates(0,
              screenHeight * 2f / 3f,
              screenWidth * (float) originActivity.getNumberOfReadyRemoteDevices() /
                  originActivity.getNumberOfRemoteDevices(),
              screenHeight / 3f);

          readyDevices = originActivity.getNumberOfReadyRemoteDevices();
          connectedDevices = originActivity.getNumberOfRemoteDevices();
        }
      }
    };

    for (Player player : players)
      if (player != null && !player.getControlType().equals(Player.ControlType.OFF)
                         && !player.getControlType().equals(Player.ControlType.BLUETOOTH))
        player.setControllerThread(originActivity.connectedThread);
  }

  @Override
  public void run(double dt) {
    super.run(dt);

    // Check if there is something to be emptied from the missed moves list.
    if (missedMovesList != null && missedMovesList.size() > 0 && missedMovesList.firstIsReady()) {
      this.decodeAndExecuteMove(missedMovesList.extractFirst());
      // If it's been emptied - remove it.
      if (missedMovesList.size() == 0)
        missedMovesList = null;
    }

    // Draw and update the ready bar at the top.
    if (this.isOver())
      readyFillBar.draw(this.getRenderer().getGl());
  }

  // This doesn't move all the snakes at all, but we're using the fact that it's invoked on
  // a timer to request snake moves.
  @Override
  public void moveAllSnakes() {
    if (missedMovesList != null && missedMovesList.size() > 0)
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

    // Check for deaths.
    for (Player player : getPlayers())
      if (player != null && player.isAlive())
        player.checkDeath();
  }

  @Override
  public void handleInputBytes(byte[] inputBytes, ConnectedThread source) {
    switch (inputBytes[0]) {
      case Protocol.GAME_MOVEMENT_OCCURRED:
        int moveId = Protocol.decodeMoveID(inputBytes[1], inputBytes[2]);
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
          // TODO FIX THIS
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
