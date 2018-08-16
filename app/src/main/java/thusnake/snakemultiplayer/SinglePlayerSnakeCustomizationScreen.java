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
        setupScreen.addGameModeItem(R.drawable.options_icon, "classic",
            OptionsBuilder.justDifficulty(renderer), GameSetupBuffer.GameMode.CLASSIC);
        setupScreen.addGameModeItem(R.drawable.options_icon, "speedy",
            OptionsBuilder.justDifficulty(renderer), GameSetupBuffer.GameMode.SPEEDY);
        setupScreen.addGameModeItem(R.drawable.options_icon, "vs ai",
            OptionsBuilder.justDifficulty(renderer), GameSetupBuffer.GameMode.VS_AI);
        setupScreen.addGameModeItem(R.drawable.singleplayer_icon, "custom",
            OptionsBuilder.defaultOptions(renderer), GameSetupBuffer.GameMode.CUSTOM);
        setupScreen.gameModeCarousel.confirmChoices();

        menu.setScreen(setupScreen);
      }
    }.withBackgroundImage(R.drawable.next_button);

    float cornerButtonSize = renderer.getScreenHeight() * 0.25f;
    MenuButton lowerLeft = new MenuButton(renderer,
                                          renderer.getScreenWidth() / 2f - cornerButtonSize - 15,
                                          10, cornerButtonSize, cornerButtonSize,
                                          MenuDrawable.EdgePoint.BOTTOM_RIGHT) {
      @Override
      public void performAction() {
        super.performAction();
        player.setCorner(PlayerController.Corner.LOWER_LEFT);
      }

      @Override
      public void move(double dt) {
        super.move(dt);
        if (player.getControlCorner().equals(PlayerController.Corner.LOWER_LEFT))
          setOpacity(1);
        else setOpacity(2/3f);
      }
    }.withBackgroundImage(R.drawable.lowerleft);

    MenuButton upperLeft = new MenuButton(renderer,
                                          renderer.getScreenWidth() / 2f - 5,
                                          10, cornerButtonSize, cornerButtonSize,
                                          MenuDrawable.EdgePoint.BOTTOM_RIGHT) {
      @Override
      public void performAction() {
        super.performAction();
        player.setCorner(PlayerController.Corner.UPPER_LEFT);
      }

      @Override
      public void move(double dt) {
        super.move(dt);
        if (player.getControlCorner().equals(PlayerController.Corner.UPPER_LEFT))
          setOpacity(1f);
        else setOpacity(2/3f);
      }
    }.withBackgroundImage(R.drawable.upperleft);

    MenuButton upperRight = new MenuButton(renderer,
                                           renderer.getScreenWidth() / 2f + 5,
                                           10, cornerButtonSize, cornerButtonSize,
                                           MenuDrawable.EdgePoint.BOTTOM_LEFT) {
      @Override
      public void performAction() {
        super.performAction();
        player.setCorner(PlayerController.Corner.UPPER_RIGHT);
      }

      @Override
      public void move(double dt) {
        super.move(dt);
        if (player.getControlCorner().equals(PlayerController.Corner.UPPER_RIGHT))
          setOpacity(1f);
        else setOpacity(2/3f);
      }
    }.withBackgroundImage(R.drawable.upperright);

    MenuButton lowerRight = new MenuButton(renderer,
                                           renderer.getScreenWidth() / 2f + cornerButtonSize + 15,
                                           10, cornerButtonSize, cornerButtonSize,
                                           MenuDrawable.EdgePoint.BOTTOM_LEFT) {
      @Override
      public void performAction() {
        super.performAction();
        player.setCorner(PlayerController.Corner.LOWER_RIGHT);
      }

      @Override
      public void move(double dt) {
        super.move(dt);
        if (player.getControlCorner().equals(PlayerController.Corner.LOWER_RIGHT))
          setOpacity(1f);
        else setOpacity(2/3f);
      }
    }.withBackgroundImage(R.drawable.lowerright);

    drawables.add(nextButton);
    drawables.add(lowerLeft);
    drawables.add(upperLeft);
    drawables.add(upperRight);
    drawables.add(lowerRight);
  }

  @Override
  public void goBack() {
    menu.setScreen(new MenuMainScreen(menu));
  }
}
