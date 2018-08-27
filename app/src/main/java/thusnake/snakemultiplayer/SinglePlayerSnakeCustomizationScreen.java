package thusnake.snakemultiplayer;

public final class SinglePlayerSnakeCustomizationScreen extends SnakeCustomizationScreen {
  private final MenuButton nextButton;

  public SinglePlayerSnakeCustomizationScreen(Menu menu) {
    super(menu, menu.getSetupBuffer().players[0]);

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

        setupScreen.gameModeCarousel.noBoundaries();
        setupScreen.addGameModeItem(R.drawable.gamemode_classic, "classic",
            OptionsBuilder.justDifficulty(renderer), GameSetupBuffer.GameMode.CLASSIC);
        setupScreen.addGameModeItem(R.drawable.gamemode_speedy, "speedy",
            OptionsBuilder.justDifficulty(renderer), GameSetupBuffer.GameMode.SPEEDY);
        setupScreen.addGameModeItem(R.drawable.gamemode_placeholder, "vs ai",
            OptionsBuilder.justDifficulty(renderer), GameSetupBuffer.GameMode.VS_AI);
        setupScreen.addGameModeItem(R.drawable.gamemode_placeholder, "custom",
            OptionsBuilder.defaultOptions(renderer), GameSetupBuffer.GameMode.CUSTOM);
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
