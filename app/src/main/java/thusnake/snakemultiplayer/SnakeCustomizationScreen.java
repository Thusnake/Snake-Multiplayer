package thusnake.snakemultiplayer;

public abstract class SnakeCustomizationScreen extends MenuScreen {
  final Player player;
  // private final MenuCarousel snakeSelectionCarousel; TODO Create the MenuCarousel class.

  public SnakeCustomizationScreen(Menu menu, Player player) {
    super(menu);
    this.player = player;
  }
}
