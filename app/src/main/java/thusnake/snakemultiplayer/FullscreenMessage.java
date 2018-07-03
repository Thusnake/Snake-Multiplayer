package thusnake.snakemultiplayer;

import android.opengl.GLES10;
import android.view.MotionEvent;

import javax.microedition.khronos.opengles.GL10;

public class FullscreenMessage {
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
                                screenHeight * 2f / 3f - renderer.getGlText().getCharHeight() * 0.65f,
                                MenuItem.Alignment.CENTER,
                                screenWidth);
    this.cancelButton = new MenuItem(renderer, "x",
                                     screenWidth - 10,
                                     screenHeight - 10 - renderer.getGlText().getCharHeight() * 0.65f,
                                     MenuItem.Alignment.RIGHT);
    this.loadingSnake = new Mesh(screenWidth / 2f - 60/720f * screenHeight,
                                 this.message.getY() / 2f - 60/720f * screenHeight,
                                 60f / 720f * screenHeight,
                                 2, 2);
    // Set the mesh colors.
    loadingSnake.updateColors(0, 0, 1f, 1f, 1f, 1f);
    loadingSnake.updateColors(0, 1, 0.25f, 0.25f, 0.25f, 1f);
    loadingSnake.updateColors(1, 0, 0f, 0f, 0f, 1f);
    loadingSnake.updateColors(1, 1, 0.25f, 0.25f, 0.25f, 1f);
  }

  public void run(double dt) {
    message.draw();
    cancelButton.draw();
    if (loading) {
      loadingSnake.draw(gl);

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
