package thusnake.snakemultiplayer.netplay;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import thusnake.snakemultiplayer.FullscreenMessage;
import thusnake.snakemultiplayer.GuestGame;
import thusnake.snakemultiplayer.OpenGLActivity;
import thusnake.snakemultiplayer.Player;
import thusnake.snakemultiplayer.controllers.BluetoothControllerBuffer;
import thusnake.snakemultiplayer.controllers.ControllerBuffer;
import thusnake.snakemultiplayer.controllers.CornerLayoutControllerBuffer;

public class GuestThread extends ConnectedThread {
  private static final String TAG = "Guest Thread";
  private static final double PING_WORRY_INTERVAL_SECONDS = 5;

  private final Thread pingThread;

  public GuestThread(OpenGLActivity activity, BluetoothSocket socket) {
    super(activity, socket);

    pingThread = new Thread(() -> {
      // Update the last activity tracker and ping the host if it's been inactive.
      while(true) {
        if (System.nanoTime() - originActivity.connectedThread.lastActivityTime > PING_WORRY_INTERVAL_SECONDS * 2) {
          originActivity.connectedThread.cancel();
          originActivity.connectedThread = null;
          originActivity.getRenderer().getMenu().endGuest();
        } else if (System.nanoTime() - originActivity.connectedThread.lastActivityTime > PING_WORRY_INTERVAL_SECONDS)
          originActivity.connectedThread.write(new byte[]{Protocol.PING});

        try {
          Thread.sleep((long) (1000 * PING_WORRY_INTERVAL_SECONDS));
        }
        catch(InterruptedException e) {
          Log.d(TAG, "Ping thread interrupted.");
        }
      }
    });
    pingThread.setDaemon(true);
    pingThread.start();
  }

  @Override
  void handleRoleSpecificInput(byte[] inputBytes) {
    switch (inputBytes[0]) {
      case Protocol.DISCONNECT:
        // Answer the call first.
        originActivity.writeBytesAuto(new byte[] {Protocol.WILL_DISCONNECT});

        // Disconnect afterwards.
        originActivity.connectedThread.cancel();
        originActivity.connectedThread = null;
        originActivity.getRenderer().quitGame();
        originActivity.getRenderer().getMenu().endGuest();
        break;

      case Protocol.NUMBER_OF_READY:
        originActivity.numberOfReadyRemoteDevices = inputBytes[1];
        break;

      case Protocol.NUMBER_OF_DEVICES:
        originActivity.numberOfRemoteDevices = inputBytes[1];
        break;

      case Protocol.READY_STATUS:
        boolean receivedReady = inputBytes[1] == (byte) 1;

        // Set the ready status without requesting anything further.
        originActivity.forceSetReady(receivedReady);
        break;

      case Protocol.NUM_DEVICES_AND_READY_WITH_STATUS:
          originActivity.numberOfRemoteDevices = inputBytes[1];
          originActivity.numberOfReadyRemoteDevices = inputBytes[2];
          originActivity.forceSetReady(inputBytes[3] == 1);

          if (inputBytes[3] == 1 && originActivity.getRenderer().getGame() == null) {
            originActivity.getRenderer().setInterruptingMessage(new FullscreenMessage(
                    originActivity.getRenderer(),
                    String.format(
                            "Waiting for %s to start the game...",
                            originActivity.connectedThread.device.getName()
                    )
            ) {
              @Override
              public void onCancel() {
                originActivity.writeBytesAuto(new byte[] {Protocol.IS_NOT_READY});
              }

              @Override
              public void run(double dt) {
                super.run(dt);
                if (!originActivity.isReady() || !originActivity.isGuest())
                  originActivity.getRenderer().cancelActivity(this);
              }
            });
          }
        break;

      case Protocol.DETAILED_SNAKES_LIST:
        int index = 0;
        for (ControllerBuffer.Corner corner : ControllerBuffer.Corner.values()) {
          index++;
          Player player = originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(corner);

          switch (inputBytes[index]) {
            case Protocol.DSL_SNAKE_OFF:
              originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().emptyCorner(corner);
              break;
            case Protocol.DSL_SNAKE_LOCAL:
              if (player == null) {
                Player playerToBeAdded = new Player(originActivity.getRenderer()).defaultPreset(originActivity.getRenderer().getMenu().setupBuffer);
                originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().addPlayer(playerToBeAdded, corner);
              } else {
                player.setController(new CornerLayoutControllerBuffer(player));
              }
              break;
            case Protocol.DSL_SNAKE_REMOTE:
              if (player == null) {
                Player playerToBeAdded = new Player(originActivity.getRenderer());
                playerToBeAdded.setControllerForced(new BluetoothControllerBuffer(
                        playerToBeAdded,
                        this
                ));
                originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().addPlayer(playerToBeAdded, corner);
              } else {
                player.setControllerForced(new BluetoothControllerBuffer(player, this));
              }
              break;
            default:
              break;
          }
        }
        break;

      case Protocol.BASIC_AGGREGATE_CALL:
        for (byte[] call : Protocol.decodeSeveralCalls(inputBytes))
          if (call.length > 0)
            originActivity.getRenderer().handleInputBytes(call, this);
        break;

      case Protocol.GAME_START_CALL:
          // Tell the host you've received the aggregate call.
          write(new byte[] {Protocol.GAME_START_RECEIVED});

          // Decode all calls and execute them.
          inputBytes[0] = Protocol.BASIC_AGGREGATE_CALL;
          originActivity.getRenderer().handleInputBytes(inputBytes, this);

          // Start the game.
          originActivity.getRenderer().startGame(new GuestGame(
                  originActivity.getRenderer().getMenu().setupBuffer.getCornerMap(),
                  originActivity.getRenderer().getMenu().setupBuffer.getGameMode().horizontalSquares.get(),
                  originActivity.getRenderer().getMenu().setupBuffer.getGameMode().verticalSquares.get(),
                  originActivity.getRenderer().getMenu().setupBuffer.getGameMode().speed.get(),
                  originActivity.getRenderer().getMenu().setupBuffer.getGameMode().stageBorders.get(),
                  null // TODO get the entity blueprints somehow
          ));
        break;

      default:
        Log.e(TAG, String.format("Received unexpected request: %d", inputBytes[0]));
    }

    // Pass to the game.
    if (originActivity.getRenderer().getGame() != null)
      originActivity.getRenderer().getGame().handleInputBytes(inputBytes, this);
  }
}
