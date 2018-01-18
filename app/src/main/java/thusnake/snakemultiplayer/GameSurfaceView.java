package thusnake.snakemultiplayer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import com.android.texample.GLText;

/**
 * Created by Nick on 22/12/2017.
 */

public class GameSurfaceView extends GLSurfaceView {
  private final GameRenderer gameRenderer;
  private Context context;
  private GLText glText;
  private double pointerDownDuration = 0;

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
      case MotionEvent.ACTION_DOWN:
        gameRenderer.setPointerDown();
        break;
      case MotionEvent.ACTION_UP:
        gameRenderer.setPointerUp();
        break;
    }

    if (this.gameRenderer.isInGame()) {
      switch (e.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
          // Handle touching down on a cornerLayout button.
          for (Player player : gameRenderer.getGame().getPlayers())
            if (player.isAlive() && player.getCornerLayout()
                                    .changeDirectionBasedOnCoordinates(pointerX[0], pointerY[0]))
          break;
        case MotionEvent.ACTION_POINTER_DOWN:
          for (Player player : gameRenderer.getGame().getPlayers())
            if (player.isAlive() && player.getCornerLayout()
                                    .changeDirectionBasedOnCoordinates(pointerX[e.getActionIndex()],
                                                                      pointerY[e.getActionIndex()]))
          break;
        case MotionEvent.ACTION_MOVE:
          // Handle moving onto a cornerLayout button.
          for (int pointerIndex = 0; pointerIndex < pointerX.length; pointerIndex++) {
            for (Player player : gameRenderer.getGame().getPlayers())
              if (player.isAlive() &&
                  player.getCornerLayout().changeDirectionBasedOnCoordinates(pointerX[pointerIndex],
                      pointerY[pointerIndex]))
                // We have found what this pointer changes so break out of the loop.
                break;
          }
          break;
        case MotionEvent.ACTION_UP:
          // Handle the game over screen inputs.
          if (gameRenderer.getGame().isOver() && e.getEventTime() - e.getDownTime() < 500
              && gameRenderer.getGame().getGameOverTimer().getTime() > 0.5) {
            // TODO Make it so that it knows which pointer did what.
            if (pointerY[0] < 1/3.0 * gameRenderer.getScreenHeight()) {
              gameRenderer.startGame();
            } else if (pointerY[0] < 2/3.0 * gameRenderer.getScreenHeight()) {
              // TODO Opens game stats
            } else {
              gameRenderer.quitGame();
            }
          }
          break;
        default:
          break;
      }
    } else {
      // Handle the plus/minus buttons.
      for (MenuItem menuItem : gameRenderer.getMenu().getCurrentMenuItems())
        if (menuItem.getValue() != null && menuItem.getValue().isExpanded())
          switch(e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
              menuItem.getValue().handleButtonsDown(pointerX[0], pointerY[0]);
              break;
            case MotionEvent.ACTION_MOVE:
              menuItem.getValue().handleButtonsMove(pointerX[0], pointerY[0]);
              break;
            case MotionEvent.ACTION_UP:
              menuItem.getValue().handleButtonsUp();
          }



      switch(e.getAction()) {
        case MotionEvent.ACTION_DOWN:
          break;
        case MotionEvent.ACTION_MOVE:
          if (e.getEventTime() - e.getDownTime() > 100
              && e.getHistorySize() > 0
              && rawX[0] - e.getHistoricalX(0) > 20) {
            // The user has swiped right - we should go back one menu (left).
            gameRenderer.getMenu().goBack();
          }
          break;
        case MotionEvent.ACTION_UP:
          // Handle pressing menu items depending on which menu we're currently on.
          // Only the first pointer can click on menu items!
          for (MenuItem menuItem : gameRenderer.getMenu().getCurrentMenuItems()) {
            if (menuItem.isClicked(pointerX[0], pointerY[0]))
              menuItem.performAction();
          }
          break;
        default:
          break;
      }
    }
    return true;
  }

  public GameRenderer getGameRenderer() { return this.gameRenderer; }
}
