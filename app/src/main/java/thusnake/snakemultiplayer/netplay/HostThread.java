package thusnake.snakemultiplayer.netplay;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import thusnake.snakemultiplayer.IDGenerator;
import thusnake.snakemultiplayer.OpenGLActivity;
import thusnake.snakemultiplayer.Player;
import thusnake.snakemultiplayer.controllers.BluetoothControllerBuffer;
import thusnake.snakemultiplayer.controllers.ControllerBuffer;

/**
 * Manages a Bluetooth connection as a game host.
 */
public class HostThread extends ConnectedThread {
  private static String TAG = "Host Thread";
  private static final double PING_WORRY_INTERVAL_SECONDS = 5;
  private static final double DISCONNECT_TIMEOUT = 5;

  private final Thread pingThread;
  private final Thread autoDisconnectThread;

  public HostThread(OpenGLActivity activity, BluetoothSocket socket) {
    super(activity, socket);

    final ConnectedThread threadToDisconnect = this;
    autoDisconnectThread = new Thread(() -> {
      try {
        Thread.sleep((long) (1000 * DISCONNECT_TIMEOUT));
      }
      catch(InterruptedException ignored) {}

      originActivity.closeConnectedGuestThread(threadToDisconnect);
    });
    autoDisconnectThread.setDaemon(true);

    pingThread = new Thread(() -> {
      while(true) {
        // Update the last activity trackers and ping threads which have been inactive.
        for (ConnectedThread thread : originActivity.connectedThreads)
          if (thread != null) {
            if (System.nanoTime() - thread.lastActivityTime > 10)
              // If a thread has been inactive for too long - disconnect it.
              originActivity.closeConnectedGuestThread(thread);
            else if (System.nanoTime() - thread.lastActivityTime > 5)
              // If a thread has been inactive for a bit - ping it to force activity.
              thread.write(new byte[] {Protocol.PING});
          }

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
    // Handle the input.
    switch (inputBytes[0]) {
      case Protocol.DISCONNECT_REQUEST:
        // Approve the request.
        this.write(new byte[] {Protocol.DISCONNECT});

        // Add the thread to the awaiting disconnect list in case it doesn't answer anymore.
        this.autoDisconnectThread.start();
        break;

      case Protocol.WILL_DISCONNECT:
        // Stop and remove the thread.
        this.autoDisconnectThread.interrupt();
        break;

      case Protocol.IS_READY:
        setReady(true);
        break;

      case Protocol.IS_NOT_READY:
        setReady(false);
        break;
        
      case Protocol.HOR_SQUARES_CHANGED:
        originActivity.getRenderer().getMenu().setupBuffer.getGameMode().horizontalSquares.set(inputBytes[1]);
        break;

      case Protocol.VER_SQUARES_CHANGED:
        originActivity.getRenderer().getMenu().setupBuffer.getGameMode().verticalSquares.set(inputBytes[1]);
        break;

      case Protocol.SPEED_CHANGED:
        originActivity.getRenderer().getMenu().setupBuffer.getGameMode().speed.set(inputBytes[1]);
        break;

      case Protocol.STAGE_BORDERS_CHANGED:
        originActivity.getRenderer().getMenu().setupBuffer.getGameMode().stageBorders.set(inputBytes[1] == 1);
        break;

      case Protocol.GAME_MODE:
        originActivity.getRenderer().getMenu().setupBuffer.setGameMode(IDGenerator.getGameModeClass(inputBytes[1]));
        break;

      case Protocol.REQUEST_ADD_SNAKE:
        ControllerBuffer.Corner requestedCorner = Protocol.decodeCorner(inputBytes[1]);
        if (originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().getPlayer(requestedCorner) == null) {
          Player addedSnake = new Player(originActivity.getRenderer());
          addedSnake.setControllerForced(new BluetoothControllerBuffer(addedSnake, this));
          addedSnake.setName(this.device.getName());
          originActivity.getRenderer().getMenu().setupBuffer.getCornerMap().addPlayer(addedSnake, requestedCorner);
        }
        break;
        
      default:
        Log.e(TAG, String.format("Received unexpected request: %d", inputBytes[0]));
    }

    // Pass to the game.
    if (originActivity.getRenderer().getGame() != null)
      originActivity.getRenderer().getGame().handleInputBytes(inputBytes, this);
  }
}
