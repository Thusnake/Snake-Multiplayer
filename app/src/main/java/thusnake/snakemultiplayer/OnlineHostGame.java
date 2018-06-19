package thusnake.snakemultiplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.microedition.khronos.opengles.GL10;

import static thusnake.snakemultiplayer.Protocol.*;

/**
 * Created by Nick on 24/02/2018.
 */

public class OnlineHostGame extends Game {
  private ArrayList<Byte> moveCodes = new ArrayList<>();
  private final List<ConnectedThread> awaitingAggregateReceive = new ArrayList<>();
  private boolean running = false;
  private final Square readyFillBar;

  // Constructor.
  public OnlineHostGame(GameRenderer renderer, int screenWidth, int screenHeight, Player[] players){
    super(renderer, screenWidth, screenHeight, players);

    // Send the initialization call and wait asynchronously for a confirmation from all.
    OpenGLES20Activity originActivity = (OpenGLES20Activity) renderer.getContext();
    for (ConnectedThread thread : originActivity.connectedThreads)
      if (thread != null) {
        thread.write(Protocol.encodeSeveralCalls(createInitializationCalls(thread)));
        awaitingAggregateReceive.add(thread);

        // Also make everyone not ready for the next game.
        thread.setReady(false);
      }

    // Set your status to not ready as well.
    originActivity.setReady(false);

    // Change the top game over button to the ready button.
    this.getGameOverTopItem().setText(originActivity.isReady() ? "Cancel" : "Ready");
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

          if (readyDevices == connectedDevices && readyDevices > 1) {
            // Everyone is ready - begin game.
            renderer.startGame(players);
          }
        }

      }
    };
  }

  public List<byte[]> createInitializationCalls(ConnectedThread thread) {
    List<byte[]> allInformation = new ArrayList<>();
    allInformation.add(new byte[] {SNAKE1_COLOR_CHANGED, (byte) getPlayers()[0].getColorIndex()});
    allInformation.add(new byte[] {SNAKE2_COLOR_CHANGED, (byte) getPlayers()[1].getColorIndex()});
    allInformation.add(new byte[] {SNAKE3_COLOR_CHANGED, (byte) getPlayers()[2].getColorIndex()});
    allInformation.add(new byte[] {SNAKE4_COLOR_CHANGED, (byte) getPlayers()[3].getColorIndex()});
    allInformation.add(new byte[] {SNAKE1_CORNER_CHANGED, Protocol.encodeCorner(getPlayers()[0].getControlCorner())});
    allInformation.add(new byte[] {SNAKE2_CORNER_CHANGED, Protocol.encodeCorner(getPlayers()[1].getControlCorner())});
    allInformation.add(new byte[] {SNAKE3_CORNER_CHANGED, Protocol.encodeCorner(getPlayers()[2].getControlCorner())});
    allInformation.add(new byte[] {SNAKE4_CORNER_CHANGED, Protocol.encodeCorner(getPlayers()[3].getControlCorner())});
    allInformation.add(getRenderer().getMenu().getAvailableSnakesList());
    allInformation.add(getRenderer().getMenu().getControlledSnakesList(thread));
    allInformation.add(new byte[] {READY_NUMBER_AND_STATUS, 0, 0});
    allInformation.add(new byte[] {HOR_SQUARES_CHANGED, (byte) horizontalSquares});
    allInformation.add(new byte[] {VER_SQUARES_CHANGED, (byte) verticalSquares});
    allInformation.add(new byte[] {SPEED_CHANGED, (byte) getSpeed()});
    allInformation.add(new byte[] {STAGE_BORDERS_CHANGED, (byte) (stageBorders ? 1 : 0)});

    return allInformation;
  }

  @Override
  public void run(double dt) {
    if (running)
      super.run(dt);

    // Draw and update the ready bar at the top.
    if (this.isOver())
      readyFillBar.draw(this.getRenderer().getGl());
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
    this.moveCodes.add(Protocol.getMovementCode(
        (players[0].getDirection() != null) ? players[0].getDirection() : Player.Direction.UP,
        (players[1].getDirection() != null) ? players[1].getDirection() : Player.Direction.UP,
        (players[2].getDirection() != null) ? players[2].getDirection() : Player.Direction.UP,
        (players[3].getDirection() != null) ? players[3].getDirection() : Player.Direction.UP
    ));

    this.sendBytes(new byte[] {
        Protocol.GAME_MOVEMENT_OCCURRED,
        (byte) (this.getMoveCount() & 0xFF),        // First byte of the moveCount integer.
        (byte) ((this.getMoveCount() >> 8) & 0xFF), // Second byte of the moveCount integer.
        // The rest of the bytes are not handled and so everything goes wrong if the game becomes
        // longer than 32768 moves.

        this.moveCodes.get(this.moveCodes.size() - 1) // Get the last movement code and send it.
    });
  }

  @Override
  public void handleInputBytes(byte[] bytes, ConnectedThread sourceThread) {
    switch (bytes[0]) {
      case Protocol.REQUEST_MOVE:
        try {
          // Send back the move number together with the information for it.
          sourceThread.write(new byte[]{
              Protocol.GAME_MOVEMENT_INFORMATION,
              bytes[1],
              bytes[2],
              this.moveCodes.get(bytes[1] + (bytes[2] << 8))
          });
        } catch (IndexOutOfBoundsException e) {
          // If we don't have information about the move (e.g. it hasn't happened yet) return a
          // movement missing signal.
          sourceThread.write(new byte[] {
              Protocol.GAME_MOVEMENT_MISSING, bytes[1], bytes[2]
          });
        }
        break;

      case AGGREGATE_CALL_RECEIVED:
        awaitingAggregateReceive.remove(sourceThread);
        if (awaitingAggregateReceive.isEmpty())
          running = true;
        break;

      case Protocol.SNAKE_DIRECTION_CHANGE:
        getPlayers()[bytes[1]].changeDirection(Protocol.decodeDirection(bytes[2]));
        break;

      default:
        break;
    }
  }
}
