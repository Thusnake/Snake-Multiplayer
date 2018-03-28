package thusnake.snakemultiplayer;

/**
 *  A button which removes a snake on press.
 */
public class MenuButtonRemoveSnake extends MenuItem {
  private final GameRenderer renderer;
  private final MenuItem playerButton;

  public MenuButtonRemoveSnake(GameRenderer renderer, MenuItem playerButton) {
    super(renderer, "-", (playerButton.getScreenNumber() + 1) * renderer.getScreenWidth() - 10,
          playerButton.getY(), Alignment.RIGHT);
    this.hide();
    this.renderer = renderer;
    this.playerButton = playerButton;
  }

  public void hide() {
    this.setDestinationXFromOrigin(this.getWidth() + 10);
    this.setDrawableOutsideOfScreen(false);
  }

  public void show() {
    this.setDestinationXFromOrigin(0);
    this.setDrawableOutsideOfScreen(true);
  }
}
