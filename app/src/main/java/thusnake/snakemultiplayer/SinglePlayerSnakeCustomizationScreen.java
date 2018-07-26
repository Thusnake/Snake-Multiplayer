package thusnake.snakemultiplayer;

public final class SinglePlayerSnakeCustomizationScreen extends SnakeCustomizationScreen {
  private final MenuButton nextButton;

  public SinglePlayerSnakeCustomizationScreen(Menu menu) {
    super(menu, menu.getPlayers()[0]);

    nextButton
        = new MenuButton(renderer,
                         renderer.getScreenWidth() - 10,
                         renderer.getScreenHeight() - 10 - (renderer.getScreenHeight() * 0.2f - 30),
                         renderer.getScreenHeight() * 0.2f - 30,
                         renderer.getScreenHeight() * 0.2f - 30,
                         MenuDrawable.EdgePoint.BOTTOM_RIGHT) {

      @Override
      public void onButtonCreated() {
        this.drawables.add(new MenuItem(renderer, "next", 0, 0, EdgePoint.BOTTOM_CENTER));
      }

      @Override
      public void performAction() {
        // Go to the single player setup screen.
        menu.setScreen(new GameSetupScreen(menu) {
          @Override
          public MenuCarousel makeCarousel() {
            MenuCarousel gameModeCarousel = new MenuCarousel(renderer, 0,
                                                             backButton.getBottomY(),
                                                             renderer.getScreenWidth(),
                                                             renderer.getScreenHeight() / 2f,
                                                             EdgePoint.TOP_LEFT);
            gameModeCarousel.addImageChoice(R.drawable.ad_icon, "ad");
            gameModeCarousel.addImageChoice(R.drawable.androidcontrols, "ad");
            gameModeCarousel.addImageChoice(R.drawable.ladder_icon, "ad");
            gameModeCarousel.addImageChoice(R.drawable.singleplayer_icon, "ad");
            gameModeCarousel.confirmChoices();
            return gameModeCarousel;
          }

          @Override
          public void goBack() {
            menu.setScreen(new SinglePlayerSnakeCustomizationScreen(menu));
          }
        });
      }
    };

    drawables.add(nextButton);
  }

  @Override
  public void goBack() {
    menu.setScreen(new MenuMainScreen(menu));
  }
}
