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
    drawables.add(new SnakeOverviewButton(this,
        renderer.getScreenWidth() / 2f - renderer.smallDistance()/2,
        backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f - renderer.smallDistance()/2,
        backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f - 20,
        MenuDrawable.EdgePoint.TOP_RIGHT, PlayerController.Corner.LOWER_LEFT));
    drawables.add(new SnakeOverviewButton(this,
        renderer.getScreenWidth() / 2f - renderer.smallDistance()/2,
        backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f + renderer.smallDistance()/2,
        backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f - 20,
        MenuDrawable.EdgePoint.BOTTOM_RIGHT, PlayerController.Corner.UPPER_LEFT));
    drawables.add(new SnakeOverviewButton(this,
        renderer.getScreenWidth() / 2f + renderer.smallDistance()/2,
        backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f + renderer.smallDistance()/2,
        backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f - 20,
        MenuDrawable.EdgePoint.BOTTOM_LEFT, PlayerController.Corner.UPPER_RIGHT));
    drawables.add(new SnakeOverviewButton(this,
        renderer.getScreenWidth() / 2f + renderer.smallDistance(),
        backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f - renderer.smallDistance()/2,
        backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f - 20,
        MenuDrawable.EdgePoint.TOP_LEFT, PlayerController.Corner.LOWER_RIGHT));
  }

  @Override
  public void goBack() {
    menu.setScreen(new MenuMainScreen(menu));
  }
}

class SnakeOverviewButton extends MenuButton {
  private MenuScreen parentScreen;
  private PlayerController.Corner corner;
  private MenuItem nameItem, plusIcon;
  private Mesh skinPreview;

  SnakeOverviewButton(MenuScreen parentScreen, float x, float y, float height, EdgePoint alignPoint,
                      PlayerController.Corner corner) {
    super(parentScreen.renderer, x, y, height * 1.5f, height, alignPoint);
    this.parentScreen = parentScreen;
    this.corner = corner;

    MenuImage background = new MenuImage(renderer, 0, 0, getWidth(), height, EdgePoint.CENTER);
    nameItem = new MenuItem(renderer, "",
                            -getWidth() / 2f,
                            getHeight() / 2f,
                            EdgePoint.TOP_LEFT, EdgePoint.TOP_LEFT);
    skinPreview = new Mesh(renderer, getWidth() / 2f,
                           getHeight() / 2f,
                           EdgePoint.TOP_RIGHT, height / 3f, 1, 3);
    nameItem.scaleToFit(skinPreview.getLeftX() - nameItem.getLeftX(), 0);
    plusIcon = new MenuItem(renderer, "+", 0, 0, EdgePoint.CENTER);
    background.setOpacity(0.1f);
    addItem(background);
    addItem(nameItem);
    addItem(skinPreview);
    addItem(plusIcon);
  }

  @Override
  public void move(double dt) {
    super.move(dt);
    plusIcon.setDrawable(getPlayer() == null);
    nameItem.setDrawable(getPlayer() != null);
    skinPreview.setDrawable(getPlayer() != null);

    if (getPlayer() != null) {
      nameItem.setText(getPlayer().getName());
      nameItem.scaleToFit(skinPreview.getLeftX() - nameItem.getLeftX(), 0);
      skinPreview.updateColors(0, getPlayer().getColors());
    }
  }

  @Override
  public void performAction() {
    super.performAction();

    if (getPlayer() != null) {
      parentScreen.menu.setScreen(new SnakeCustomizationScreen(parentScreen.menu, getPlayer()) {
        @Override
        public void goBack() {
          parentScreen.menu.setScreen(parentScreen);
        }
      });
    } else {
      GameSetupBuffer setupBuffer = parentScreen.menu.getSetupBuffer();
      setupBuffer.addPlayer(new Player(renderer, setupBuffer.getNumberOfPlayers()).defaultPreset(),
                            corner);
    }
  }

  private Player getPlayer() {
    return parentScreen.menu.getSetupBuffer().getPlayer(corner);
  }
}
