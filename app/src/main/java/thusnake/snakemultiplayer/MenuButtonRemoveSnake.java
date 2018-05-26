package thusnake.snakemultiplayer;

/**
 *  A button which removes a snake on press.
 */
public class MenuButtonRemoveSnake extends MenuItem {
  private final GameRenderer renderer;
  private final MenuItem playerButton;
  private final float hideDistance;

  public MenuButtonRemoveSnake(GameRenderer renderer, MenuItem playerButton) {
    super(renderer, "-", (playerButton.getScreenNumber() + 1) * renderer.getScreenWidth()
            - renderer.getScreenWidth() * 0.0425f, playerButton.getY(), Alignment.CENTER);
    this.hideDistance = renderer.getScreenWidth() * 0.0425f * 2;
    this.hide();
    this.renderer = renderer;
    this.playerButton = playerButton;

  }

  public void hide() {
    this.setDestinationXFromOrigin(this.getWidth() + hideDistance);
    this.setDrawableOutsideOfScreen(false);
  }

  public void show() {
    this.setDestinationXFromOrigin(0);
    this.setDrawableOutsideOfScreen(true);
  }
}
