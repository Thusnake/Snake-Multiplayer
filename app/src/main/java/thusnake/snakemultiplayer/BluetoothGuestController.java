package thusnake.snakemultiplayer;

import android.content.Context;
import android.view.MotionEvent;

import java.util.List;

public class BluetoothGuestController extends PlayerController {
  private final PlayerController wrappedController;
  private final ConnectedThread hostThread;

  public BluetoothGuestController(PlayerController controllerToBeWrapped, ConnectedThread host) {
    super(controllerToBeWrapped.renderer, controllerToBeWrapped.player);
    wrappedController = controllerToBeWrapped;
    hostThread = host;
  }

  @Override
  public boolean changeDirection(Player.Direction direction) {
    boolean success = super.changeDirection(direction);
    if (hostThread != null)
      hostThread.write(new byte[] {
          Protocol.SNAKE_DIRECTION_CHANGE,
          Protocol.encodeCorner(player.getControlCorner()),
          Protocol.encodeDirection(player.getDirection())
      });
    return success;
  }

  @Override
  public void onMotionEvent(MotionEvent event) { wrappedController.onMotionEvent(event); }

  @Override
  public String identifier() { return wrappedController.identifier(); }

  @Override
  public String toString() { return wrappedController.toString(); }

  @Override
  public void setTexture(Context context) { wrappedController.setTexture(context); }

  @Override
  public List<MenuDrawable> optionsList(GameRenderer renderer) {
    return wrappedController.optionsList(renderer);
  }
}
