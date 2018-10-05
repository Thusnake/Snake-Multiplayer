package thusnake.snakemultiplayer;

import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import static thusnake.snakemultiplayer.Protocol.*;

/**
 * Created by Nick on 24/02/2018.
 */

public class OnlineHostGame extends Game {
  private ArrayList<byte[]> moveCodes = new ArrayList<>();
  private int moveCount = 0;
  private final Map<ConnectedThread, List<byte[]>> awaitingAggregateReceive = new HashMap<>();
  private final SimpleTimer awaitingAggregateReceiveTimer = new SimpleTimer(0.0, 0.25);
  private boolean running = false;
  private final Square readyFillBar;
  private final Game selfReference = this;

  // Constructor.
  public OnlineHostGame(GameRenderer renderer, GameSetupBuffer setupBuffer){
    super(renderer, setupBuffer);

    // The first move is null, as it's the game's initial state.
    moveCodes.add(null);
    prepare();

    OpenGLES20Activity originActivity = (OpenGLES20Activity) getRenderer().getContext();

    // Make everyone not ready for the next game.
    for (ConnectedThread thread : originActivity.connectedThreads)
      if (thread != null)
        thread.setReady(false);
    // Set your status to not ready as well.
    originActivity.setReady(false);

    // Send the initialization call and wait asynchronously for a confirmation from all.
    for (ConnectedThread thread : originActivity.connectedThreads)
      if (thread != null) {
        List<byte[]> calls = createInitializationCalls(setupBuffer, thread);
        thread.write(Protocol.encodeSeveralCalls(calls, GAME_START_CALL));
        awaitingAggregateReceive.put(thread, calls);
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

    readyFillBar = new Square(getRenderer(), 0, getScreenHeight()*2/3,
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
              getScreenHeight() * 2f / 3f,
              getScreenWidth() * (float) originActivity.getNumberOfReadyRemoteDevices() /
                                    originActivity.getNumberOfRemoteDevices(),
              getScreenHeight() / 3f);

          readyDevices = originActivity.getNumberOfReadyRemoteDevices();
          connectedDevices = originActivity.getNumberOfRemoteDevices();

          if (readyDevices == connectedDevices && readyDevices > 1) {
            // Everyone is ready - begin game.
            getRenderer().restartGame(selfReference);
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

  public List<byte[]> createInitializationCalls(GameSetupBuffer setupBuffer,
                                                ConnectedThread thread) {
    List<byte[]> allInformation = new ArrayList<>(setupBuffer.allInformationCallList(thread));
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
        for (Map.Entry<ConnectedThread, List<byte[]>> entry : awaitingAggregateReceive.entrySet())
          entry.getKey().write(Protocol.encodeSeveralCalls(entry.getValue(), GAME_START_CALL));
      }


    // Draw and update the ready bar at the top.
    if (this.isOver())
      readyFillBar.draw(this.getRenderer().getGl());
  }

  @Override
  protected boolean checkGameOver() {
    if (super.checkGameOver()) {
      Player winner = assessWinner();
      this.sendBytes(new byte[] {Protocol.END_GAME, winner == null ? 0 : Protocol.encodeCorner(winner.getControlCorner())});
      return true;
    }
    return false;
  }

  @Override
  protected void moveAllSnakes() {
    super.moveAllSnakes();
    moveCount++;

    List<Player.Direction> directions = new LinkedList<>();
    for (PlayerController.Corner corner : PlayerController.Corner.values()) {
      if (cornerMap.getPlayer(corner) == null) directions.add(Player.Direction.UP);
      else directions.add(cornerMap.getPlayer(corner).getDirection());
    }

    this.moveCodes.add(new byte[] {
        Protocol.GAME_MOVEMENT_OCCURRED,
        Protocol.encodeMoveID(moveCount).first,  // The rest of the bytes are not handled and so
        Protocol.encodeMoveID(moveCount).second, // everything goes wrong if the game becomes longer
        Protocol.getMovementCode(                // than 32768 moves.
          directions.get(0), directions.get(1), directions.get(2), directions.get(3))
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

      case GAME_START_RECEIVED:
        awaitingAggregateReceive.remove(sourceThread);
        if (awaitingAggregateReceive.isEmpty())
          running = true;
        break;

      case Protocol.SNAKE_DIRECTION_CHANGE:
        cornerMap.getPlayer(decodeCorner(bytes[1]))
            .changeDirection(Protocol.decodeDirection(bytes[2]));
        break;

      default:
        break;
    }
  }
}
