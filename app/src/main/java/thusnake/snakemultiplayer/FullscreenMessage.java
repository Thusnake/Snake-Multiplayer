package thusnake.snakemultiplayer;

import android.view.MotionEvent;

import javax.microedition.khronos.opengles.GL10;

public class FullscreenMessage implements Activity {
  private final GameRenderer renderer;
  private final GL10 gl;
  private final MenuDrawable message;
  private final MenuDrawable cancelButton;

  private final Mesh loadingSnake;
  private SimpleTimer loadingRotateTimer = new SimpleTimer(0.0, 0.25);
  private boolean loading = false;

  public FullscreenMessage(GameRenderer renderer, String message) {
    this.renderer = renderer;
    this.gl = renderer.getGl();

    float screenWidth = renderer.getScreenWidth();
    float screenHeight = renderer.getScreenHeight();
    this.message
        = new MultilineMenuItem(renderer, message,
                                screenWidth / 2f,
                                screenHeight * 2f / 3f,
                                MenuDrawable.EdgePoint.TOP_CENTER,
                                screenWidth);
    this.cancelButton = new MenuItem(renderer, "x",
                                     screenWidth - 10,
                                     screenHeight - 10,
                                     MenuDrawable.EdgePoint.TOP_RIGHT);
    this.loadingSnake = new Mesh(renderer, screenWidth / 2f,
                                 this.message.getBottomY() / 2f,
                                 MenuDrawable.EdgePoint.CENTER,
                                 60f / 720f * screenHeight,
                                 2, 2);
    // Set the mesh colors.
    loadingSnake.updateColors(0, 0, 1f, 1f, 1f, 1f);
    loadingSnake.updateColors(0, 1, 0.25f, 0.25f, 0.25f, 1f);
    loadingSnake.updateColors(1, 0, 0f, 0f, 0f, 1f);
    loadingSnake.updateColors(1, 1, 0.25f, 0.25f, 0.25f, 1f);
  }

  @Override
  public void run(double dt) {
    message.draw();
    cancelButton.draw();
    if (loading) {
      loadingSnake.draw();

      if (loadingRotateTimer.count(dt)) {
        loadingRotateTimer.reset();
        float[][] previousColors = {loadingSnake.getColors(0), loadingSnake.getColors(1),
                                    loadingSnake.getColors(2), loadingSnake.getColors(3)};
        loadingSnake.updateColors(0, previousColors[2]);
        loadingSnake.updateColors(1, previousColors[0]);
        loadingSnake.updateColors(2, previousColors[3]);
        loadingSnake.updateColors(3, previousColors[1]);
      }
    }
  }

  @Override
  public void refresh() {}

  public FullscreenMessage withLoadingSnake(boolean loading) {
    this.loading = loading;
    return this;
  }

  public void onMotionEvent(MotionEvent event) {
    switch(event.getAction()) {
      case MotionEvent.ACTION_UP:
        if (cancelButton.isClicked(event.getX(), event.getY())) {
          renderer.setInterruptingMessage(null);
          this.onCancel();
        }
    }
  }

  // To be overridden.
  public void onCancel() {}
}
