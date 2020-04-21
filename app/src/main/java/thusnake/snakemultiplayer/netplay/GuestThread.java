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

  private final NetplayGuestModule parentModule;
  private final Thread pingThread;

  public GuestThread(NetplayGuestModule parentModule, BluetoothSocket socket) {
    super(socket);
    this.parentModule = parentModule;

    pingThread = new Thread(() -> {
      // Update the last activity tracker and ping the host if it's been inactive.
      while(true) {
        if (System.nanoTime() - this.lastActivityTime > PING_WORRY_INTERVAL_SECONDS * 2) {
          this.cancel();
        } else if (System.nanoTime() - this.lastActivityTime > PING_WORRY_INTERVAL_SECONDS)
          this.write(new byte[]{Protocol.PING});

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
        parentModule.writeBytesAuto(new byte[] {Protocol.WILL_DISCONNECT});

        // Disconnect afterwards.
        this.cancel();
        break;

      case Protocol.NUMBER_OF_READY:
        parentModule.numberOfReadyRemoteDevices= inputBytes[1];
        break;

      case Protocol.NUMBER_OF_DEVICES:
        parentModule.numberOfRemoteDevices = inputBytes[1];
        break;

      case Protocol.READY_STATUS:
        boolean receivedReady = inputBytes[1] == (byte) 1;

        // Set the ready status without requesting anything further.
        parentModule.forceSetReady(receivedReady);
        break;

      case Protocol.NUM_DEVICES_AND_READY_WITH_STATUS:
          parentModule.numberOfRemoteDevices = inputBytes[1];
          parentModule.numberOfReadyRemoteDevices = inputBytes[2];
          parentModule.forceSetReady(inputBytes[3] == 1);

          if (inputBytes[3] == 1 && OpenGLActivity.current.getRenderer().getGame() == null) {
            OpenGLActivity.current.getRenderer().setInterruptingMessage(new FullscreenMessage(
                    OpenGLActivity.current.getRenderer(),
                    String.format("Waiting for %s to start the game...", this.device.getName())
            ) {
              @Override
              public void onCancel() {
                write(new byte[] {Protocol.IS_NOT_READY});
              }

              @Override
              public void run(double dt) {
                super.run(dt);
                if (!parentModule.isReady() || OpenGLActivity.current.netplayModule == null)
                  OpenGLActivity.current.getRenderer().cancelActivity(this);
              }
            });
          }
        break;

      case Protocol.DETAILED_SNAKES_LIST:
        int index = 0;
        for (ControllerBuffer.Corner corner : ControllerBuffer.Corner.values()) {
          index++;
          Player player = OpenGLActivity.current.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(corner);

          switch (inputBytes[index]) {
            case Protocol.DSL_SNAKE_OFF:
              OpenGLActivity.current.getRenderer().getMenu().setupBuffer.getCornerMap().emptyCorner(corner);
              break;
            case Protocol.DSL_SNAKE_LOCAL:
              if (player == null) {
                Player playerToBeAdded = new Player(OpenGLActivity.current.getRenderer()).defaultPreset(OpenGLActivity.current.getRenderer().getMenu().setupBuffer);
                OpenGLActivity.current.getRenderer().getMenu().setupBuffer.getCornerMap().addPlayer(playerToBeAdded, corner);
              } else {
                player.setController(new CornerLayoutControllerBuffer(player));
              }
              break;
            case Protocol.DSL_SNAKE_REMOTE:
              if (player == null) {
                Player playerToBeAdded = new Player(OpenGLActivity.current.getRenderer());
                playerToBeAdded.setControllerForced(new BluetoothControllerBuffer(
                        playerToBeAdded,
                        this
                ));
                OpenGLActivity.current.getRenderer().getMenu().setupBuffer.getCornerMap().addPlayer(playerToBeAdded, corner);
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
            this.handleInput(call);
        break;

      case Protocol.GAME_START_CALL:
          // Tell the host you've received the aggregate call.
          write(new byte[] {Protocol.GAME_START_RECEIVED});

          // Decode all calls and execute them.
          inputBytes[0] = Protocol.BASIC_AGGREGATE_CALL;
          this.handleInput(inputBytes);

          // Start the game.
          OpenGLActivity.current.getRenderer().startGame(new GuestGame(
                  OpenGLActivity.current.getRenderer().getMenu().setupBuffer.getCornerMap(),
                  OpenGLActivity.current.getRenderer().getMenu().setupBuffer.getGameMode().horizontalSquares.get(),
                  OpenGLActivity.current.getRenderer().getMenu().setupBuffer.getGameMode().verticalSquares.get(),
                  OpenGLActivity.current.getRenderer().getMenu().setupBuffer.getGameMode().speed.get(),
                  OpenGLActivity.current.getRenderer().getMenu().setupBuffer.getGameMode().stageBorders.get(),
                  null // TODO get the entity blueprints somehow
          ));
        break;

      default:
        Log.e(TAG, String.format("Received unexpected request: %d", inputBytes[0]));
    }

    // Pass to the game.
    if (OpenGLActivity.current.getRenderer().getGame() != null)
      OpenGLActivity.current.getRenderer().getGame().handleInputBytes(inputBytes, this);
  }

  @Override
  public void cancel() {
    super.cancel();
    OpenGLActivity.current.getRenderer().quitGame();
    OpenGLActivity.current.getRenderer().getMenu().endGuest();
  }
}
