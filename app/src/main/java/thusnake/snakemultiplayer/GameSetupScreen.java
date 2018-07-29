package thusnake.snakemultiplayer;

import java.util.List;

public abstract class GameSetupScreen extends MenuScreen implements TextureReloadable {
  private final MenuCarousel gameModeCarousel;
  private MenuContainer listOfOptions;
  private final MenuButton nextButton;

  public GameSetupScreen(Menu menu, List<CarouselItem> gameModeOptions) {
    super(menu);

    gameModeCarousel = new MenuCarousel(renderer, 0,
                                        backButton.getBottomY(),
                                        renderer.getScreenWidth(),
                                        renderer.getScreenHeight() / 2f,
                                        MenuDrawable.EdgePoint.TOP_LEFT);
    for (CarouselItem carouselItem : gameModeOptions)
      gameModeCarousel.addChoice(carouselItem);
    gameModeCarousel.confirmChoices();

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
        renderer.startGame(new Game(renderer, menu.getSetupBuffer()));
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

  public void setListOfOptions(MenuContainer listOfOptions) {this.listOfOptions = listOfOptions;}
}
