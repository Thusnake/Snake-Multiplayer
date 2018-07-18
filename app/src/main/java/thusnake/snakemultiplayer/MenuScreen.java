package thusnake.snakemultiplayer;

import android.view.MotionEvent;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A single screen of the game's menu.
 * Is NOT reusable - once the user has changed screens, this one is deleted and a new one of the
 * same class must be created.
 */
public abstract class MenuScreen {
  final OpenGLES20Activity originActivity;
  final GameRenderer renderer;
  final Menu menu;
  final Set<MenuDrawable> drawables = new LinkedHashSet<>();
  final MenuButton backButton;

  MenuScreen(Menu menu) {
    this.originActivity = menu.getOriginActivity();
    this.renderer = menu.getRenderer();
    this.menu = menu;

    backButton
        = new MenuButton(renderer,
                         10,
                         renderer.getScreenHeight() - 10 - (renderer.getScreenHeight() * 0.2f - 30),
                         renderer.getScreenHeight() * 0.2f - 30,
                         renderer.getScreenHeight() * 0.2f - 30,
                         MenuItem.Alignment.LEFT) {
      @Override
      public void onButtonCreated() {
        this.drawables.add(new MenuItem(renderer, "<", getX() + getWidth() / 2f,
            getY() + 5, MenuItem.Alignment.CENTER));
      }

      @Override
      public void performAction() {
        goBack();
      }
    };

    drawables.add(backButton);
  }

  /**
   * Draws everything within the screen. Should be called at every frame if the screen is active.
   */
  public void drawAll() {
    for (MenuDrawable drawable : drawables)
      drawable.draw();
  }

  /**
   * Updates everything within the screen. Should be called at every frame if the screen is active.
   * @param dt The current frame's delta time.
   */
  public void moveAll(double dt) {
    for (MenuDrawable drawable : drawables)
      drawable.move(dt);
  }

  /**
   * Sets the menu screen to what should be the previous one in the screen progression flow.
   */
  public abstract void goBack();

  /**
   * Handles a motion event passed from the active view.
   * @param event The current event to be handled.
   */
  public void onMotionEvent(MotionEvent event) {
    // Pass it to all buttons.
    for (MenuDrawable drawable : drawables)
      if (drawable instanceof MenuButton)
        ((MenuButton) drawable).onMotionEvent(event);
  }

  public Set<MenuDrawable> getAllDrawables() { return drawables; }

  public void refresh() {
    for (MenuDrawable drawable : drawables)
      if (drawable instanceof TextureReloadable)
        ((TextureReloadable) drawable).reloadTexture();
  }
}

