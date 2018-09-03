package thusnake.snakemultiplayer;

public class ConnectScreen extends MenuScreen {
  private final MenuButton hostButton, joinButton;

  public ConnectScreen(Menu menu) {
    super(menu);

    float buttonSize = renderer.getScreenHeight()
        - (renderer.getScreenHeight() - backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER)
        + renderer.smallDistance() * 2) * 2;
    hostButton = new MenuButton(renderer,
                                renderer.getScreenWidth() / 2f - renderer.smallDistance() * 2,
                                renderer.getScreenHeight() / 2f,
                                buttonSize, buttonSize, MenuDrawable.EdgePoint.RIGHT_CENTER) {
      @Override
      public void performAction() {
        super.performAction();
        menu.beginHost();
        goBack();
      }
    }.withBackgroundImage(R.drawable.host_icon);

    joinButton = new MenuButton(renderer,
                                renderer.getScreenWidth() / 2f + renderer.smallDistance() * 2,
                                renderer.getScreenHeight() / 2f,
                                buttonSize, buttonSize, MenuDrawable.EdgePoint.LEFT_CENTER) {
      @Override
      public void performAction() {
        super.performAction();
//        menu.setScreen(new JoinScreen(menu));
      }
    }.withBackgroundImage(R.drawable.join_icon);

    drawables.add(hostButton);
    drawables.add(joinButton);
    drawables.add(new MenuItem(renderer, "Connect", renderer.getScreenWidth() / 2,
        backButton.getY(MenuDrawable.EdgePoint.TOP_CENTER), MenuDrawable.EdgePoint.TOP_CENTER));
  }

  @Override
  public void goBack() {
    menu.setScreen(new MultiplayerSnakeOverviewScreen(menu));
  }
}
