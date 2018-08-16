package thusnake.snakemultiplayer;

public final class MenuMainScreen extends MenuScreen {
  private final MenuButton singleplayerButton, multiplayerButton, videoAdButton, optionsButton;

  public MenuMainScreen(Menu menu) {
    super(menu);

    singleplayerButton = new MenuButton(renderer,
                                        renderer.getScreenWidth() / 2f - renderer.getScreenWidth() / 40f,
                                        renderer.getScreenHeight() * 0.2f,
                                        renderer.getScreenHeight() * 0.7f,
                                        renderer.getScreenHeight() * 0.7f,
                                        MenuDrawable.EdgePoint.BOTTOM_RIGHT) {
      @Override
      public void performAction() {
        menu.getSetupBuffer().players = new Player[1];
        menu.getSetupBuffer().players[0] = new Player(0).defaultPreset();
        menu.setScreen(new SinglePlayerSnakeCustomizationScreen(menu));
      }
    }.withBackgroundImage(R.drawable.singleplayer_icon);

    multiplayerButton = new MenuButton(renderer,
                                       renderer.getScreenWidth() / 2f + renderer.getScreenWidth() / 40f,
                                       renderer.getScreenHeight() * 0.2f,
                                       renderer.getScreenHeight() * 0.7f,
                                       renderer.getScreenHeight() * 0.7f,
                                       MenuDrawable.EdgePoint.BOTTOM_LEFT) {
      @Override
      public void performAction() {
//        menu.setScreen(new MultiPlayerSnakeSetupScreen(menu));
      }
    }.withBackgroundImage(R.drawable.multiplayer_icon);

    this.optionsButton = new MenuButton(renderer,
                                        renderer.getScreenWidth() - 10,
                                        10,
                                        renderer.getScreenHeight() * 0.2f - 30,
                                        renderer.getScreenHeight() * 0.2f - 30,
                                        MenuDrawable.EdgePoint.BOTTOM_RIGHT) {
      @Override
      public void performAction() {
//        menu.setScreen(new OptionsScreen(menu));
      }
    }.withBackgroundImage(R.drawable.options_icon);

    this.videoAdButton = new MenuButton(renderer,
                                        10,
                                        10,
                                        (renderer.getScreenHeight() * 0.2f - 30) * 53/21f,
                                        renderer.getScreenHeight() * 0.2f - 30,
                                        MenuDrawable.EdgePoint.BOTTOM_LEFT) {
      @Override
      public void performAction() { originActivity.showAd(); }
    }.withBackgroundImage(R.drawable.ad_icon);

    drawables.add(singleplayerButton);
    drawables.add(multiplayerButton);
    drawables.add(optionsButton);
    drawables.add(videoAdButton);

    drawables.remove(backButton);
  }

  @Override
  public void goBack() {}
}
