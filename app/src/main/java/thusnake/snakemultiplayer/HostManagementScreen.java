package thusnake.snakemultiplayer;

public class HostManagementScreen extends MenuScreen {
  public HostManagementScreen(Menu menu) {
    super(menu);

    MenuListOfItems listOfOverviewItems = new MenuListOfItems(renderer, renderer.smallDistance(),
        renderer.getScreenHeight() / 2f, MenuDrawable.EdgePoint.LEFT_CENTER);
    for (ConnectedThread thread : originActivity.connectedThreads) {
      listOfOverviewItems.addItem(new DeviceOverviewItem(renderer, 0, 0,
          renderer.getScreenWidth() - renderer.smallDistance() * 2, renderer.getScreenHeight() / 5f,
          MenuDrawable.EdgePoint.TOP_LEFT, thread));
    }
    drawables.add(listOfOverviewItems);

    drawables.add(new MenuButton(renderer, renderer.getScreenWidth() / 2f, renderer.smallDistance(),
        renderer.getScreenWidth() / 2, renderer.getScreenHeight() / 4,
        MenuDrawable.EdgePoint.BOTTOM_CENTER) {
      @Override
      public void onButtonCreated() {
        super.onButtonCreated();
        addItem(new MenuItem(renderer, "Stop Server", getX(EdgePoint.CENTER),
            getY(EdgePoint.CENTER), EdgePoint.CENTER));
        setColors(new float[] {1f, 0.5f, 0.5f, 1f});
      }

      @Override
      public void performAction() {
        super.performAction();
        menu.stopHost();
        goBack();
      }
    });
  }

  private class DeviceOverviewItem extends MenuFlexContainer {
    private ConnectedThread thread;
    private MenuImage deviceIcon;
    private MenuItem name;

    private DeviceOverviewItem(GameRenderer renderer, float x, float y, float width, float height,
                               EdgePoint alignPoint, ConnectedThread thread) {
      super(renderer, alignPoint);

      setX(x);
      setY(y);
      setWidth(width);
      setHeight(height);

      deviceIcon = new MenuImage(renderer, getX(EdgePoint.LEFT_CENTER), getY(EdgePoint.LEFT_CENTER),
          getHeight() * 81f / 168f, getHeight(), EdgePoint.LEFT_CENTER);

      name = new MenuItem(renderer, "",
          deviceIcon.getX(EdgePoint.RIGHT_CENTER) + renderer.smallDistance(),
          getY(EdgePoint.CENTER), EdgePoint.LEFT_CENTER);

      this.thread = thread;
      update();

      addItem(deviceIcon);
      addItem(name);
    }

    private void update() {
      if (thread == null) {
        deviceIcon.setTexture(R.drawable.phone_icon_free);
        name.setText("Open");
        name.setDescription("");
      } else {
        deviceIcon.setTexture
            (thread.isReady() ? R.drawable.phone_icon_ready : R.drawable.phone_icon_joined);
        name.setText(thread.device.getName());
        name.setDescription(thread.device.getAddress());
      }
    }

    @Override
    public void move(double dt) {
      super.move(dt);
      update();
    }
  }


  @Override
  public void goBack() {
    menu.setScreen(new MultiplayerSnakeOverviewScreen(menu));
  }
}