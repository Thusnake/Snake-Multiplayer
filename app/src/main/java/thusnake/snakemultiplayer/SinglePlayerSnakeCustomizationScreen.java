package thusnake.snakemultiplayer;

import thusnake.snakemultiplayer.gamemodes.GameMode;

public final class SinglePlayerSnakeCustomizationScreen extends SnakeCustomizationScreen {
  private final MenuButton nextButton;

  public SinglePlayerSnakeCustomizationScreen(Menu menu) {
    super(menu, menu.getSetupBuffer().cornerMap.getPlayers().get(0));

    nextButton
        = new MenuButton(renderer,
                         renderer.getScreenWidth() - 10,
                         renderer.getScreenHeight() - 10 - (renderer.getScreenHeight() * 0.2f - 30),
                         (renderer.getScreenHeight() * 0.2f - 30) * 2,
                         renderer.getScreenHeight() * 0.2f - 30,
                         MenuDrawable.EdgePoint.BOTTOM_RIGHT) {
      @Override
      public void performAction() {
        // Go to the single player setup screen.
        GameSetupScreen setupScreen = new GameSetupScreen(menu) {
          @Override
          public void goBack() {
            menu.setScreen(new SinglePlayerSnakeCustomizationScreen(menu));
          }
        };

        for (GameMode gameMode : menu.getSetupBuffer().gameModes)
          setupScreen.addGameModeItem(gameMode);

        setupScreen.gameModeCarousel.noBoundaries();
        setupScreen.gameModeCarousel.confirmChoices();

        menu.setScreen(setupScreen);
      }
    }.withBackgroundImage(R.drawable.next_button);

    drawables.add(nextButton);
  }

  @Override
  public void goBack() {
    menu.setScreen(new MenuMainScreen(menu));
  }
}
