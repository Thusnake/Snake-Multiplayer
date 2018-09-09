package thusnake.snakemultiplayer;

public class JoinScreen extends MenuScreen {
  private final MenuButton searchButton;
  private final MenuListOfItems searchResults;

  public JoinScreen(Menu menu) {
    super(menu);

    menu.updatePairedDevices();

    searchButton = new MenuButton(renderer, renderer.getScreenWidth() - renderer.smallDistance(),
                                  renderer.getScreenHeight() - renderer.smallDistance(),
                                  backButton.getHeight(), backButton.getHeight(),
                                  MenuDrawable.EdgePoint.TOP_RIGHT) {
      @Override
      public void performAction() {
        super.performAction();
        menu.beginSearch();
      }

      @Override
      public void move(double dt) {
        super.move(dt);
        setEnabled(!originActivity.discoveryStarted());
      }
    }.withBackgroundImage(R.drawable.wrench);

    searchResults = new MenuListOfItems(renderer, renderer.smallDistance(),
                                        searchButton.getBottomY(),
                                        MenuDrawable.EdgePoint.TOP_LEFT) {
      @Override
      public void move(double dt) {
        contents.clear();
        for (MenuDrawable item : menu.pairedDevices.getItems()) {
          item.scale.setTime(0.8);
          addItem(item);
        }
        for (MenuDrawable item : menu.foundDevices.getItems()) {
          item.scale.setTime(0.8);
          addItem(item);
        }
        super.move(dt);

      }
    };

    drawables.add(new MenuItem(renderer, "Join", renderer.getScreenWidth() / 2,
        renderer.getScreenHeight() - renderer.smallDistance(), MenuDrawable.EdgePoint.TOP_CENTER));
    drawables.add(searchButton);
    drawables.add(searchResults);
  }

  @Override
  public void goBack() {
    menu.setScreen(new ConnectScreen(menu));
  }
}
