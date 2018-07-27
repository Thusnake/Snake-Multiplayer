package thusnake.snakemultiplayer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import com.android.texample.GLText;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

/**
 * Created by Nick on 22/12/2017.
 */

public class GameSurfaceView extends GLSurfaceView {
  private final GameRenderer gameRenderer;
  private Context context;
  private GLText glText;
  private double pointerDownDuration = 0;
  private enum HoldMode {NORMAL, HOR_SCROLL, VER_SCROLL}
  private HoldMode holdMode = HoldMode.NORMAL;
  private float holdOriginX, holdOriginY;

  public GameSurfaceView(Context context) {
    super(context);
    this.context = context;
    this.gameRenderer = new GameRenderer(context);
    this.setRenderer(this.gameRenderer);
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    // Assign the pointer coordinates to arrays for optimisation.
    float[] pointerX = new float[e.getPointerCount()];
    float[] pointerY = new float[e.getPointerCount()];
    float[] rawX = new float[e.getPointerCount()];
    float[] rawY = new float[e.getPointerCount()];
    for (int i = 0; i < e.getPointerCount(); i++) {
      pointerX[i] = e.getX(i) - gameRenderer.getMenu().getScreenTransformX();
      pointerY[i] = e.getY(i) - gameRenderer.getMenu().getScreenTransformY();
      rawX[i] = e.getX(i);
      rawY[i] = e.getY(i);
    }

    switch(e.getActionMasked()) {
      case ACTION_DOWN:
        gameRenderer.setPointerDown();
        break;
      case MotionEvent.ACTION_UP:
        gameRenderer.setPointerUp();
        break;
    }
    if (this.gameRenderer.getInterruptingMessage() != null) {
      // Let the FullscreenMessage handle the input.
      gameRenderer.getInterruptingMessage().onMotionEvent(e);
    } else if (this.gameRenderer.isInGame()) {
      // Send all the alive players the motion event.
      for (Player player : gameRenderer.getGame().getPlayers())
        if (player != null && player.isAlive())
          player.onMotionEvent(e);

      // Handle the game over screen inputs.
      if (e.getActionMasked() == MotionEvent.ACTION_UP
          && gameRenderer.getGame().isOver() && e.getEventTime() - e.getDownTime() < 500
          && gameRenderer.getGame().getGameOverTimer().getTime() > 0.5) {

          for (MenuItem item : gameRenderer.getGame().getGameOverItems())
            if (item.isClicked(pointerX[0], pointerY[0]))
              item.performAction();
      }
    } else {
      // Handle the plus/minus buttons.
//      for (MenuItem menuItem : gameRenderer.getMenu().getCurrentMenuItems())
//        if (menuItem.getValue() != null && menuItem.getValue().isExpanded())
//          switch (e.getActionMasked()) {
//            case ACTION_DOWN:
//              menuItem.getValue().handleButtonsDown(pointerX[0], pointerY[0]);
//              break;
//            case ACTION_MOVE:
//              menuItem.getValue().handleButtonsMove(pointerX[0], pointerY[0]);
//              break;
//            case MotionEvent.ACTION_UP:
//              menuItem.getValue().handleButtonsUp();
//          }

      // Handle player menu color and corner squares.
//      if (gameRenderer.getMenu().getState() == Menu.MenuState.PLAYERSOPTIONS
//          || gameRenderer.getMenu().getPreviousState() == Menu.MenuState.PLAYERSOPTIONS) {
//        for (MenuButton square : gameRenderer.getMenu().getColorSelectionSquares())
//          square.onMotionEvent(e);
//
//        for (MenuButton square : gameRenderer.getMenu().getCornerSelectionSquares())
//          square.onMotionEvent(e);
//      }

      switch (e.getAction()) {
        case ACTION_DOWN:
          break;
        case ACTION_MOVE:
          // Changing the hold mode checkers.
          if (holdMode == HoldMode.NORMAL && e.getEventTime() - e.getDownTime() > 100
              && e.getHistorySize() > 0) {
            if (rawX[0] - e.getHistoricalX(0) > 20 || rawX[0] - holdOriginX > 60) {
              // The user has swiped right.
//              holdMode = HoldMode.HOR_SCROLL;
//              gameRenderer.getMenu()
//                  .peekLeftScreen(Math.max(rawX[0] - e.getHistoricalX(0), rawX[0] - holdOriginX));
            } else if (gameRenderer.getMenu().isScrollable()
                && (Math.abs(rawY[0] - e.getHistoricalY(0)) > 20
                || Math.abs(rawY[0] - holdOriginY) > 60)) {
              // The user has swiped vertically.
              holdMode = HoldMode.VER_SCROLL;
            }
          }

          // Using the hold mode.
          if (e.getHistorySize() > 0) {
            if (holdMode == HoldMode.VER_SCROLL && gameRenderer.getMenu().isScrollable()) {
              gameRenderer.getMenu().scroll(rawY[0] - e.getHistoricalY(0));
              gameRenderer.getMenu().setScrollInertia(rawY[0] - e.getHistoricalY(0));
            } else if (holdMode == HoldMode.HOR_SCROLL) {
              if (rawX[0] - e.getHistoricalX(0) > 20) {
                gameRenderer.getMenu().goBack();
                // Reset everything as if the user has released the screen and pressed it again.
                holdMode = HoldMode.NORMAL;
                holdOriginX = e.getRawX();
                holdOriginY = e.getRawY();
              } else {
//                gameRenderer.getMenu().peekLeftScreen(rawX[0] - e.getHistoricalX(0));
              }
            }
          }
          break;
        case MotionEvent.ACTION_UP:

          break;
        default:
          break;
      }

      // Nullify the hold mode if the user releases their pointer.
      if (e.getAction() == ACTION_UP) {
        holdMode = HoldMode.NORMAL;
//        gameRenderer.getMenu().snapToClosestHorizontalScreen();
      }
      // Set the origin coordinates it the user presses the screen.
      else if (e.getAction() == ACTION_DOWN) {
        holdOriginX = e.getRawX();
        holdOriginY = e.getRawY();
      }

      // Pass the event to the current screen to handle.
      if (holdMode == HoldMode.NORMAL)
        gameRenderer.getMenu().getCurrentScreen().onMotionEvent(e, pointerX, pointerY);
    }
    return true;
  }

  public GameRenderer getGameRenderer() { return this.gameRenderer; }
}
