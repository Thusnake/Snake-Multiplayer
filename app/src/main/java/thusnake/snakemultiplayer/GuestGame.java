package thusnake.snakemultiplayer;

import android.util.Pair;

import javax.microedition.khronos.opengles.GL10;

public class GuestGame extends Game {
  private OpenGLES20Activity originActivity;
  private int moveCount = 0;
  private MissedMovesList missedMovesList;
  private final Square readyFillBar;

  public GuestGame(GameRenderer renderer, GameSetupBuffer setupBuffer) {
    super(renderer, setupBuffer);

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

    readyFillBar = new Square(renderer, 0, getScreenHeight()*2/3,
        getScreenWidth(), getScreenHeight()/3) {
      private int readyDevices = -1;
      private int connectedDevices = -1;

      @Override
      public void draw(GL10 gl) {
        if (originActivity.getNumberOfReadyRemoteDevices() != readyDevices
            || originActivity.getNumberOfRemoteDevices() != connectedDevices) {
          this.setCoordinates(0,
              getScreenHeight() * 2f / 3f,
              getScreenWidth() * (float) originActivity.getNumberOfReadyRemoteDevices() /
                  originActivity.getNumberOfRemoteDevices(),
              getScreenHeight() / 3f);

          readyDevices = originActivity.getNumberOfReadyRemoteDevices();
          connectedDevices = originActivity.getNumberOfRemoteDevices();
        }

        // Flash from 0.1 opacity to 0.4 opacity.
        gl.glColor4f(1f,1f,1f,(float) Math.sin(getGameOverTimer().getTime() * 4) / 7.5f + 0.25f);
        super.draw(gl);
      }
    };
  }

  @Override
  public void run(double dt) {
    super.run(dt);

    // Check if there is something to be emptied from the missed moves list.
    while (missedMovesList != null && missedMovesList.size() > 0 && missedMovesList.firstIsReady()){
      this.executeMove(missedMovesList.extractFirst());
      // If it's been emptied - remove it.
      if (missedMovesList.size() == 0)
        missedMovesList = null;
    }

    // Draw and update the ready bar at the top.
    if (this.isOver())
      readyFillBar.draw(this.getRenderer().getGl());
  }

  @Override
  public void onStep() {
    // We're using the fact that it's invoked on a timer to request snake moves.
    if (missedMovesList != null && missedMovesList.size() > 0) {
      for (Integer index : missedMovesList.missingMovesIndices()) {
        Pair<Byte, Byte> idBytes = Protocol.encodeMoveID(index);
        this.sendBytes(new byte[]{Protocol.REQUEST_MOVE, idBytes.first, idBytes.second});
      }
    } else if (missedMovesList != null) {
      // The missed moves list has a size of 0, so make it null.
      missedMovesList = null;
    } else {
      if (!this.isOver()) {
        // There is nothing to catch up on, so try requesting the next move.
        Pair<Byte, Byte> nextId = Protocol.encodeMoveID(moveCount + 1);
        sendBytes(new byte[]{Protocol.REQUEST_MOVE, nextId.first, nextId.second});
      }
    }
  }

  private void executeMove(byte[] moveBytes) {
    switch(moveBytes[0]) {
      case Protocol.GAME_MOVEMENT_OCCURRED:
        byte encodedMove = moveBytes[3];

        Player.Direction[] directions = new Player.Direction[4];
        int index = 0;
        Protocol.decodeMovementCode(encodedMove, directions);
        for (PlayerController.Corner corner : PlayerController.Corner.values()) {
          Player player;
          if ((player = cornerMap.getPlayer(corner)) != null)
            player.changeDirection(directions[index]);

          index++;
        }

        // Update all entities and snakes as per usual.
        performMove();
        break;

      case Protocol.GAME_APPLE_EATEN_NEXT_POS:
        ((Apple) getEntities().get(moveBytes[3])).setNextPosition(moveBytes[4], moveBytes[5]);
        break;

      case Protocol.GAME_ENTITY_POS_CHANGE:
        getEntities().get(moveBytes[3]).setPosition(moveBytes[4], moveBytes[5]);
        break;

      default: break;
    }

    // Update the counter.
    moveCount++;
  }

  @Override
  public void handleInputBytes(byte[] inputBytes, ConnectedThread source) {
    switch (inputBytes[0]) {
      case Protocol.GAME_MOVEMENT_OCCURRED:
      case Protocol.GAME_APPLE_EATEN_NEXT_POS:
      case Protocol.GAME_ENTITY_POS_CHANGE:
        int moveId = Protocol.decodeMoveID(inputBytes[1], inputBytes[2]);
        if (missedMovesList == null) {
          if (moveId - moveCount == 1) {
            // The move is correct, as it follows the last we know of. Execute it.
            executeMove(inputBytes);
          } else if (moveId - moveCount > 1) {
            // We've missed some moves, so request all the missing ones.
            requestMissedMovesPriorTo(inputBytes);
          }
        } else {
          // If we're tracking missed moves this might be one of them, so try inserting it in the
          // list.
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
    originActivity.writeBytesAuto(bytes);
  }

  private void requestMissedMovesPriorTo(byte[] latestMove) {
    int moveId = Protocol.decodeMoveID(latestMove[1], latestMove[2]);

    // We've probably missed a move (or more) and so we'll send a request.
    for (int missedMoveIndex = moveCount + 1; missedMoveIndex < moveId; missedMoveIndex++) {
      Pair<Byte, Byte> idBytes = Protocol.encodeMoveID(missedMoveIndex);
      this.sendBytes(new byte[] {Protocol.REQUEST_MOVE, idBytes.first, idBytes.second});
    }

    // Create a list (or expand the current one) of missed moves.
    if (this.missedMovesList == null)
      this.missedMovesList = new MissedMovesList(moveCount, moveId, latestMove);
    else
      this.missedMovesList.expand(moveId, latestMove);
  }
}
