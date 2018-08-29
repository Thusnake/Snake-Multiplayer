package thusnake.snakemultiplayer;

public class MultiplayerSnakeOverviewScreen extends MenuScreen {
  private MenuButton nextButton;

  public MultiplayerSnakeOverviewScreen(Menu menu) {
    super(menu);

    nextButton
        = new MenuButton(renderer,
                         renderer.getScreenWidth() - 10,
                         renderer.getScreenHeight() - 10,
                         (renderer.getScreenHeight() * 0.2f - 30) * 2,
                         renderer.getScreenHeight() * 0.2f - 30,
                         MenuDrawable.EdgePoint.TOP_RIGHT) {
      @Override
      public void performAction() {
        super.performAction();
        GameSetupScreen setupScreen = new GameSetupScreen(menu) {
          @Override
          public void goBack() {
            menu.setScreen(new MultiplayerSnakeOverviewScreen(menu));
          }
        };

        setupScreen.addGameModeItem(R.drawable.gamemode_classic, "Classic", null,
                                    GameSetupBuffer.GameMode.CLASSIC);
        setupScreen.addGameModeItem(R.drawable.gamemode_placeholder, "Custom",
                                    OptionsBuilder.defaultOptions(renderer),
                                    GameSetupBuffer.GameMode.CUSTOM);
        setupScreen.gameModeCarousel.noBoundaries();
        setupScreen.gameModeCarousel.confirmChoices();

        menu.setScreen(setupScreen);
      }
    }.withBackgroundImage(R.drawable.next_button);

    drawables.add(nextButton);
    drawables.add(new SnakeOverviewButton(this, renderer.getScreenWidth() / 2f,
        backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f,
        backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f - 20,
        MenuDrawable.EdgePoint.TOP_RIGHT, menu.getSetupBuffer().players[0]));
  }

  @Override
  public void goBack() {
    menu.setScreen(new MenuMainScreen(menu));
  }
}

class SnakeOverviewButton extends MenuButton {
  private MenuScreen parentScreen;
  private Player player;

  SnakeOverviewButton(MenuScreen parentScreen, float x, float y, float height, EdgePoint alignPoint,
                      Player player) {
    super(parentScreen.renderer, x, y, height * 1.2f, height, alignPoint);
    this.parentScreen = parentScreen;
    this.player = player;

    withBackgroundImage(R.drawable.loadingsnake);
  }

  @Override
  public void performAction() {
    super.performAction();

    if (player != null) {
      parentScreen.menu.setScreen(new SnakeCustomizationScreen(parentScreen.menu, player) {
        @Override
        public void goBack() {
          parentScreen.menu.setScreen(parentScreen);
        }
      });
    } else {
      GameSetupBuffer setupBuffer = parentScreen.menu.getSetupBuffer();
    }
  }
}
