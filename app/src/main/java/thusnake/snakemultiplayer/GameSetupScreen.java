package thusnake.snakemultiplayer;

public abstract class GameSetupScreen extends MenuScreen implements TextureReloadable {
  private final MenuCarousel gameModeCarousel;
  private final MenuListOfItems listOfOptions;
  private final MenuButton nextButton;

  public GameSetupScreen(Menu menu) {
    super(menu);

    gameModeCarousel = makeCarousel();
    listOfOptions = new MenuListOfItems(renderer, 10, gameModeCarousel.getY() - 50);
    nextButton
        = new MenuButton(renderer,
        renderer.getScreenWidth() - 10,
        renderer.getScreenHeight() - 10 - (renderer.getScreenHeight() * 0.2f - 30),
        renderer.getScreenHeight() * 0.2f - 30,
        renderer.getScreenHeight() * 0.2f - 30,
        MenuItem.Alignment.RIGHT) {
      @Override
      public void onButtonCreated() {
        drawables.add(new MenuItem(renderer, "next", getX() + getWidth() / 2f, getY(),
                                   MenuItem.Alignment.CENTER));
      }

      @Override
      public void performAction() {
        renderer.startGame(new Game(renderer, menu.getSetupBuffer()));
      }
    };

    drawables.add(gameModeCarousel);
    drawables.add(listOfOptions);
    drawables.add(nextButton);
  }

  public abstract MenuCarousel makeCarousel();

  @Override
  public void reloadTexture() {
    for (MenuDrawable drawable : drawables)
      if (drawable instanceof TextureReloadable)
        ((TextureReloadable) drawable).reloadTexture();
  }
}
