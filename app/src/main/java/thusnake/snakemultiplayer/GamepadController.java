package thusnake.snakemultiplayer;

import android.content.Context;
import android.view.MotionEvent;

public class GamepadController extends CornerLayoutController {
  public GamepadController(GameRenderer renderer, Player player) {
    super(renderer, player);
    System.out.println("Created");
  }

  @Override
  public void loadGLTexture(Context context) {
    super.loadGLTexture(context);
    System.out.println("Loaded");
    this.getDrawableLayout().loadGLTexture(this.getGl(), context, R.drawable.gamepad);
  }

  @Override
  public void onMotionEvent(MotionEvent event) {

  }
}
