package thusnake.snakemultiplayer;

import thusnake.snakemultiplayer.netplay.ConnectedThread;
import thusnake.snakemultiplayer.netplay.Protocol;

public class HostManagementScreen extends MenuScreen {
  private final DeviceOverviewItem[] deviceOverviewItems;
  public HostManagementScreen(Menu menu) {
    super(menu);

    MenuButton disconnectButton = new MenuButton(renderer, renderer.getScreenWidth() / 2f,
        renderer.smallDistance(), renderer.getScreenWidth() / 2, renderer.getScreenHeight() / 4,
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
    };

    MenuListOfItems listOfOverviewItems = new MenuListOfItems(renderer, renderer.smallDistance(),
        renderer.getScreenHeight() / 2f, MenuDrawable.EdgePoint.LEFT_CENTER);
    deviceOverviewItems = new DeviceOverviewItem[originActivity.connectedThreads.length];

    float deviceOverviewItemHeight = (backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER)
                                     - disconnectButton.getY(MenuDrawable.EdgePoint.TOP_CENTER))
                                     / originActivity.connectedThreads.length;

    for (int index = 0; index < originActivity.connectedThreads.length; index++) {
      deviceOverviewItems[index] = new DeviceOverviewItem(renderer, 0, 0,
          renderer.getScreenWidth() - renderer.smallDistance() * 2, deviceOverviewItemHeight,
          MenuDrawable.EdgePoint.TOP_LEFT, originActivity.connectedThreads[index]);
    }

    for (DeviceOverviewItem item : deviceOverviewItems)
      listOfOverviewItems.addItem(item);

    drawables.add(new MenuItem(renderer, "Host", renderer.getScreenWidth() / 2,
        renderer.getScreenHeight() - renderer.smallDistance(), MenuDrawable.EdgePoint.TOP_CENTER));
    drawables.add(disconnectButton);
    drawables.add(listOfOverviewItems);
  }

  @Override
  public void moveAll(double dt) {
    for (int index = 0; index < originActivity.connectedThreads.length; index++)
      deviceOverviewItems[index].update(originActivity.connectedThreads[index]);
    super.moveAll(dt);
  }

  private class DeviceOverviewItem extends MenuFlexContainer {
    private ConnectedThread connectedThread;
    private MenuImage deviceIcon;
    private MenuItem name;
    private MenuButton disconnectButton;

    private DeviceOverviewItem(GameRenderer renderer, float x, float y, float width, float height,
                               EdgePoint alignPoint, ConnectedThread thread) {
      super(renderer, alignPoint);

      setX(x);
      setY(y);
      setWidth(width);
      setHeight(height);

      connectedThread = thread;

      deviceIcon = new MenuImage(renderer, getX(EdgePoint.LEFT_CENTER), getY(EdgePoint.LEFT_CENTER),
          getHeight() * 81f / 168f, getHeight(), EdgePoint.LEFT_CENTER);

      name = new MenuItem(renderer, "",
          deviceIcon.getX(EdgePoint.RIGHT_CENTER) + renderer.smallDistance(),
          getY(EdgePoint.CENTER), EdgePoint.LEFT_CENTER);

      disconnectButton = new MenuButton(renderer, getX(EdgePoint.RIGHT_CENTER),
          getY(EdgePoint.RIGHT_CENTER), getHeight() * 2f/5f, getHeight() * 2f/3f,
          EdgePoint.RIGHT_CENTER) {
        @Override
        public void performAction() {
          super.performAction();
          disconnectDevice();
        }
      }.withBackgroundImage(R.drawable.trash_bin);
      disconnectButton.setColors(new float[] {1f, 0.5f, 0.5f, 1f});

      update(thread);

      addItem(deviceIcon);
      addItem(name);
      addItem(disconnectButton);
    }

    private void update(ConnectedThread thread) {
      // Update the thread pointer.
      connectedThread = thread;

      // Update the contents.
      disconnectButton.setDrawable(thread != null);
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

    private void disconnectDevice() {
      connectedThread.write(new byte[] {Protocol.DISCONNECT});
      originActivity.awaitingDisconnectThreads.add(connectedThread);
    }
  }


  @Override
  public void goBack() {
    menu.setScreen(new MultiplayerSnakeOverviewScreen(menu));
  }
}