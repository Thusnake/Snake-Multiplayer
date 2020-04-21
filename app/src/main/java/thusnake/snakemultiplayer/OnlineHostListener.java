package thusnake.snakemultiplayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import thusnake.snakemultiplayer.netplay.ConnectedThread;
import thusnake.snakemultiplayer.netplay.Protocol;

import static thusnake.snakemultiplayer.netplay.Protocol.GAME_START_CALL;
import static thusnake.snakemultiplayer.netplay.Protocol.GAME_START_RECEIVED;
import static thusnake.snakemultiplayer.netplay.Protocol.decodeCorner;

public class OnlineHostListener extends Thread implements GameListener {
  private Game game;
  private ArrayList<byte[]> moveCodes = new ArrayList<>();
  private int moveCount = 0;
  private final Map<ConnectedThread, List<byte[]>> awaitingAggregateReceive = new HashMap<>();
  private Square readyFillBar;
  private final GameSetupBuffer setupBuffer;

  public OnlineHostListener(GameSetupBuffer gameSetupBuffer) {
    this.setupBuffer = gameSetupBuffer;
  }

  @Override
  public void onRegistered(Game game) {
    this.game = game;
  }

  @Override
  public void onGameCreated() {
    // Reset all connected threads to not ready.
    for (ConnectedThread connectedThread : OpenGLActivity.current.connectedThreads)
      if (connectedThread != null)
        connectedThread.setReady(false);

    // Set your own status to not ready as well.
    OpenGLActivity.current.setReady(false);

    // Send the initialization call and wait synchronously for a confirmation from all.
    for (ConnectedThread thread : OpenGLActivity.current.connectedThreads)
      if (thread != null) {
        List<byte[]> calls = setupBuffer.allInformationCallList(thread);
        calls.addAll(game.setupCallList(thread));
        calls.add(Protocol.encodeRandomSeed(game.getRandSeed()));
        thread.write(Protocol.encodeSeveralCalls(calls, GAME_START_CALL));
        awaitingAggregateReceive.put(thread, calls);
      }

    // Start the thread that's going to handle some of the connection-specific tasks.
    this.start();

    // Pause the game while waiting for confirmations.
    game.setPaused(true);

    // Change the top game over button to the ready button.
    game.getGameOverTopItem().setText(OpenGLActivity.current.isReady() ? "Cancel" : "Ready");
    game.getGameOverTopItem().setAction((action, origin) -> {
      if (OpenGLActivity.current.isReady()) {
        OpenGLActivity.current.setReady(false);
        MenuItem originItem = (MenuItem) origin;
        originItem.setText("Ready");
      } else {
        OpenGLActivity.current.setReady(true);
        MenuItem originItem = (MenuItem) origin;
        originItem.setText("Cancel");
      }
    });

    GameRenderer renderer = OpenGLActivity.current.getRenderer();
    readyFillBar = new Square(renderer, 0, renderer.getScreenHeight()*2/3,
        renderer.getScreenWidth(), renderer.getScreenHeight()/3) {
      private int readyDevices = -1;
      private int connectedDevices = -1;

      @Override
      public void draw(GL10 gl) {
        // Flash from 0.1 opacity to 0.4 opacity.
        gl.glColor4f(1f,1f,1f,(float) Math.sin(game.getGameOverTimer().getTime() * 4) / 7.5f + 0.25f);
        super.draw(gl);

        if (OpenGLActivity.current.getNumberOfReadyRemoteDevices() != readyDevices
            || OpenGLActivity.current.getNumberOfRemoteDevices() != connectedDevices) {
          this.setCoordinates(0,
              renderer.getScreenHeight() * 2f / 3f,
              renderer.getScreenWidth() * (float) OpenGLActivity.current.getNumberOfReadyRemoteDevices() /
                  OpenGLActivity.current.getNumberOfRemoteDevices(),
              renderer.getScreenHeight() / 3f);

          readyDevices = OpenGLActivity.current.getNumberOfReadyRemoteDevices();
          connectedDevices = OpenGLActivity.current.getNumberOfRemoteDevices();

          if (readyDevices == connectedDevices && readyDevices > 1) {
            // Everyone is ready - begin game.
            renderer.restartGame(game);
          }
        }

      }
    };
    // TODO Send that ready fill bar to the game to draw.
  }

  @Override
  public void run() {
    while(!awaitingAggregateReceive.isEmpty())
      try {
        sleep(250);
      }
      catch (InterruptedException e) {
        System.out.println("OnlineHostListener has been interrupted: " + e.getMessage());
      }
      finally {
        for (ConnectedThread thread : awaitingAggregateReceive.keySet())
          thread.write(Protocol.encodeSeveralCalls(awaitingAggregateReceive.get(thread),
                                                   GAME_START_CALL));
      }



  }

  @Override
  public void onGameStart() {

  }

  @Override
  public void onGameOver() {

  }

  @Override
  public void beforeMoveExecuted() {

  }

  @Override
  public void afterMoveExecuted() {

  }

  @Override
  public void onSnakeCreation(Snake snake) {

  }

  @Override
  public void onSnakeDeath(Snake snake) {

  }

  @Override
  public void onEntityCreation(Entity entity) {

  }

  @Override
  public void onEntityDestroyed(Entity entity) {

  }

  @Override
  public void onInputBytesReceived(byte[] bytes, ConnectedThread source) {
    switch (bytes[0]) {
      case Protocol.REQUEST_MOVE:
        try {
          // Send back the move number together with the information for it.
          source.write(this.moveCodes.get(Protocol.decodeMoveID(bytes[1], bytes[2])));
        } catch (IndexOutOfBoundsException e) {
          // If we don't have information about the move (e.g. it hasn't happened yet) return a
          // movement missing signal.
          source.write(new byte[] {
              Protocol.GAME_MOVEMENT_MISSING, bytes[1], bytes[2]
          });
        }
        break;

      case GAME_START_RECEIVED:
        awaitingAggregateReceive.remove(source);
        if (awaitingAggregateReceive.isEmpty())
          game.setPaused(false);
        break;

      case Protocol.SNAKE_DIRECTION_CHANGE:
        game.getSnakeAt(decodeCorner(bytes[1])).changeDirection(Protocol.decodeDirection(bytes[2]));
        break;

      default:
        break;
    }
  }
}
