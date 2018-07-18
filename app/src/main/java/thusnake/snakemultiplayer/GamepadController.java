package thusnake.snakemultiplayer;

import android.content.Context;
import android.view.MotionEvent;

public class GamepadController extends CornerLayoutController {
  public GamepadController(GameRenderer renderer, Player player) {
    super(renderer, player);
  }

  @Override
  public void setTexture(Context context) {
    this.getDrawableLayout().setTexture(R.drawable.gamepad);
  }

  @Override
  public void onMotionEvent(MotionEvent event) {

  }
}
