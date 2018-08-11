package thusnake.snakemultiplayer;

import java.util.List;

public abstract class GameSetupScreen extends MenuScreen implements TextureReloadable {
  final MenuCarousel gameModeCarousel;
  private MenuListOfItems listOfOptions;
  private final MenuButton nextButton;

  public GameSetupScreen(Menu menu) {
    super(menu);

    gameModeCarousel = new MenuCarousel(renderer, 0,
                                        backButton.getBottomY(),
                                        renderer.getScreenWidth(),
                                        renderer.getScreenHeight() / 2f,
                                        MenuDrawable.EdgePoint.TOP_LEFT);

    listOfOptions = new MenuListOfItems(renderer, 10, gameModeCarousel.getBottomY() - 50,
                                        MenuDrawable.EdgePoint.TOP_LEFT);
    nextButton
        = new MenuButton(renderer,
        renderer.getScreenWidth() - 10,
        renderer.getScreenHeight() - 10 - (renderer.getScreenHeight() * 0.2f - 30),
        (renderer.getScreenHeight() * 0.2f - 30) * 2,
        renderer.getScreenHeight() * 0.2f - 30,
        MenuDrawable.EdgePoint.BOTTOM_RIGHT) {
      @Override
      public void performAction() {
        renderer.startGame(menu.getSetupBuffer().createGame(renderer));
      }
    }.withBackgroundImage(R.drawable.ready_button);

    drawables.add(gameModeCarousel);
    drawables.add(listOfOptions);
    drawables.add(nextButton);
  }

  @Override
  public void reloadTexture() {
    for (MenuDrawable drawable : drawables)
      if (drawable instanceof TextureReloadable)
        ((TextureReloadable) drawable).reloadTexture();
  }

  public void setListOfOptions(List<MenuDrawable> listOfDrawables) {
    MenuListOfItems listOfItems =
        new MenuListOfItems(renderer,
                            10,
                            gameModeCarousel.getY(MenuDrawable.EdgePoint.BOTTOM_LEFT),
                            MenuDrawable.EdgePoint.TOP_LEFT);

    if (listOfDrawables != null)
      for (MenuDrawable drawable : listOfDrawables)
        listOfItems.addItem(drawable);

    drawables.remove(listOfOptions);
    this.listOfOptions = listOfItems;
    drawables.add(listOfOptions);
  }

  public void addGameModeItem(int resourceId, String name, List<MenuDrawable> options,
                              GameSetupBuffer.GameMode gameMode) {
    gameModeCarousel.addChoice(
        new CarouselItem(gameModeCarousel,
                         CarouselItem.makeFittingImage(gameModeCarousel, resourceId),
                         name) {
          @Override
          public void onChosen() {
            super.onChosen();
            setListOfOptions(options);
            menu.getSetupBuffer().gameMode = gameMode;
          }
        });
  }
}
