package thusnake.snakemultiplayer;

import android.view.MotionEvent;

public class FullscreenMessage {
  private final GameRenderer renderer;
  private final MenuDrawable message;
  private final MenuDrawable cancelButton;

  private final Square loadingSnake;
  private SimpleTimer loadingRotateTimer = new SimpleTimer(0.0, 0.25);
  private int loadingRotations = 0;
  private boolean loading = false, loadingTextureLoaded = false;

  public FullscreenMessage(GameRenderer renderer, String message) {
    this.renderer = renderer;
    this.message = new MenuItem(renderer, message,
                                renderer.getScreenWidth() / 2f,
                                renderer.getScreenHeight() / 3f,
                                MenuItem.Alignment.CENTER);
    this.cancelButton = new MenuItem(renderer, "x",
                                     renderer.getScreenWidth() - 10,
                                     renderer.getScreenHeight() - 10 - renderer.getGlText().getCharHeight() * 0.65f,
                                     MenuItem.Alignment.RIGHT);
    this.loadingSnake = new Square(renderer.getScreenWidth() / 2f,
                                   renderer.getScreenHeight() / 2f,
                                   180 / 720 * renderer.getScreenHeight(),
                                   180 / 720 * renderer.getScreenHeight());
  }

  public void run(double dt) {
    message.draw();
    cancelButton.draw();
    if (loading) {
      // TODO Have it load a texture that rotates.
      loadingSnake.draw(renderer.getGl());

      if (loadingRotateTimer.count(dt)) {
        loadingRotateTimer.reset();
        loadingRotations++;
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
