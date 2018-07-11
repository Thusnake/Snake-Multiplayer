package thusnake.snakemultiplayer;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import static thusnake.snakemultiplayer.Protocol.*;

/**
 * Created by Nick on 24/02/2018.
 */

public class OnlineHostGame extends Game {
  private ArrayList<byte[]> moveCodes = new ArrayList<>();
  private int moveCount = 0;
  private final List<ConnectedThread> awaitingAggregateReceive = new ArrayList<>();
  private final SimpleTimer awaitingAggregateReceiveTimer = new SimpleTimer(0.0, 0.25);
  private boolean running = false;
  private final Square readyFillBar;

  // Constructor.
  public OnlineHostGame(GameRenderer renderer, int screenWidth, int screenHeight, Player[] players){
    super(renderer, screenWidth, screenHeight, players);

    // The first move is null, as it's the game's initial state.
    moveCodes.add(null);
    prepare();

    OpenGLES20Activity originActivity = (OpenGLES20Activity) renderer.getContext();

    // Make everyone not ready for the next game.
    for (ConnectedThread thread : originActivity.connectedThreads)
      if (thread != null)
        thread.setReady(false);
    // Set your status to not ready as well.
    originActivity.setReady(false);

    // Send the initialization call and wait asynchronously for a confirmation from all.
    for (ConnectedThread thread : originActivity.connectedThreads)
      if (thread != null) {
        thread.write(Protocol.encodeSeveralCalls(createInitializationCalls(thread)));
        awaitingAggregateReceive.add(thread);
      }

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

  public void prepare() {
    moveCount++;
    Pair<Byte, Byte> moveId = Protocol.encodeMoveID(moveCount);
    moveCodes.add(new byte[] {GAME_APPLE_POS_CHANGED,
                              moveId.first, moveId.second,
                              (byte) getApples().get(0).getX(), (byte) getApples().get(0).getY()});
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
    allInformation.add(getRenderer().getMenu().getDetailedSnakesList(thread));
    allInformation.add(new byte[] {READY_NUMBER_AND_STATUS, 0, 0});
    allInformation.add(new byte[] {HOR_SQUARES_CHANGED, (byte) horizontalSquares});
    allInformation.add(new byte[] {VER_SQUARES_CHANGED, (byte) verticalSquares});
    allInformation.add(new byte[] {SPEED_CHANGED, (byte) getSpeed()});
    allInformation.add(new byte[] {STAGE_BORDERS_CHANGED, (byte) (stageBorders ? 1 : 0)});
    allInformation.add(moveCodes.get(1));

    return allInformation;
  }

  @Override
  public void run(double dt) {
    if (running)
      super.run(dt);
    else if (awaitingAggregateReceive != null && !awaitingAggregateReceive.isEmpty())
      if (awaitingAggregateReceiveTimer.count(dt)) {
        awaitingAggregateReceiveTimer.reset();
        for (ConnectedThread thread : awaitingAggregateReceive)
          thread.write(Protocol.encodeSeveralCalls(createInitializationCalls(thread)));
      }


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
    moveCount++;

    Player[] players = this.getPlayers();
    this.moveCodes.add(new byte[] {
        Protocol.GAME_MOVEMENT_OCCURRED,
        Protocol.encodeMoveID(moveCount).first,  // The rest of the bytes are not handled
        Protocol.encodeMoveID(moveCount).second, // and so everything goes wrong if the
        Protocol.getMovementCode(                // game becomes longer than 32768 moves.
          (players[0].getDirection() != null) ? players[0].getDirection() : Player.Direction.UP,
          (players[1].getDirection() != null) ? players[1].getDirection() : Player.Direction.UP,
          (players[2].getDirection() != null) ? players[2].getDirection() : Player.Direction.UP,
          (players[3].getDirection() != null) ? players[3].getDirection() : Player.Direction.UP
        )
    });

    // Get the movement code we just added and send it.
    this.sendBytes(this.moveCodes.get(this.moveCodes.size() - 1));
  }

  @Override
  public void onAppleEaten(Apple apple) {
    super.onAppleEaten(apple);
    moveCount++;

    this.moveCodes.add(new byte[] {
        Protocol.GAME_APPLE_EATEN_NEXT_POS,
        Protocol.encodeMoveID(moveCount).first,
        Protocol.encodeMoveID(moveCount).second,
        (byte) apple.getX(), (byte) apple.getY()
    });

    this.sendBytes(moveCodes.get(moveCodes.size() - 1));
  }

  @Override
  public void handleInputBytes(byte[] bytes, ConnectedThread sourceThread) {
    switch (bytes[0]) {
      case Protocol.REQUEST_MOVE:
        try {
          // Send back the move number together with the information for it.
          sourceThread.write(this.moveCodes.get(Protocol.decodeMoveID(bytes[1], bytes[2])));
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
