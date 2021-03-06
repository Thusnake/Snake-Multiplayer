package thusnake.snakemultiplayer;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.util.Pair;

import com.android.texample.GLText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.microedition.khronos.opengles.GL10;

import thusnake.snakemultiplayer.MenuDrawable.EdgePoint;
import thusnake.snakemultiplayer.controllers.BluetoothControllerBuffer;
import thusnake.snakemultiplayer.controllers.ControllerBuffer;
import thusnake.snakemultiplayer.controllers.CornerLayoutControllerBuffer;
import thusnake.snakemultiplayer.textures.GameTextureMap;

/**
 * Created by Nick on 14/12/2017.
 */

// And instance of this class would be a single menu with all menu items.
public class Menu implements Activity {
  private SimpleTimer screenTransformX, screenTransformY;
  private float screenWidth, screenHeight;
  private GameRenderer renderer;
  private final GL10 gl;
  private final GLText glText;

  private final MenuItem[] menuItemsMain, menuItemsConnect, menuItemsBoard, menuItemsPlayers,
      menuItemsPlayersOptions;
  private final List<MenuDrawable> drawablesMain, drawablesConnect, drawablesBoard,
      drawablesPlayers, drawablesPlayersOptions;

  private enum ConnectionType {BLUETOOTH, WIFI}
  private enum ConnectionRole {HOST, GUEST}
  private ConnectionType connectionType = null;
  private ConnectionRole connectionRole = null;
  final DeviceItemMap pairedDevices = new DeviceItemMap();
  final DeviceItemMap foundDevices = new DeviceItemMap();
  private final List<MenuDrawable> bluetoothHostMenu = new ArrayList<>();
  private final List<MenuDrawable> bluetoothGuestMenu = new ArrayList<>();
  private final List<MenuDrawable> guestDisabledDrawables = new ArrayList<>();
  private final MenuDrawable bluetoothStatusIcon, disconnectButton;

  private final MenuButton[] colorSelectionSquare;
  private final MenuItem addSnakeButton;
  private int playersOptionsIndex;
  private SimpleTimer backgroundSnakeTimer = new SimpleTimer(0.0, 0.5 + Math.random());
  private LinkedList<BackgroundSnake> backgroundSnakes = new LinkedList<>();
  private SimpleTimer scrollInertia = new SimpleTimer(0.0);
  private SimpleTimer hostUpdatePacketTimer = new SimpleTimer(0.0, 0.5);

  private final OpenGLActivity originActivity;
  private MenuScreen currentScreen;
  private MenuScreenAnimation transitionAnimation;

  // Menu variables
  GameSetupBuffer setupBuffer = null;

  // Constructor.
  public Menu(GameRenderer renderer, float screenWidth, float screenHeight) {
    this.renderer = renderer;
    this.gl = renderer.getGl();
    this.glText = renderer.getGlText();
    this.originActivity = (OpenGLActivity) renderer.getContext();

    this.screenTransformX = new SimpleTimer(0.0);
    this.screenTransformY = new SimpleTimer(0.0);
    this.currentScreen = new MenuMainScreen(this);

    this.screenWidth = screenWidth;
    this.screenHeight = screenHeight;

    // Create menuItem instances for each button.
    // Main screen buttons.
    String[] menuItemsMainText = {"Play", "Connect", "Board", "Players", "Watch ad"};
    this.menuItemsMain = new MenuItem[menuItemsMainText.length];
    this.menuItemsMain[0] = new MenuItem(renderer, menuItemsMainText[0], 10,
        screenHeight - glText.getCharHeight() * 0.65f
            - (screenHeight - glText.getCharHeight() * menuItemsMain.length * 0.65f) / 2,
        EdgePoint.BOTTOM_LEFT) {
      @Override
      public void move(double dt) {
        super.move(dt);
        if (originActivity.isGuest() || originActivity.isHost())
          if (originActivity.isReady()) {
            if (!this.getText().equals("Cancel")) this.setText("Cancel");
          } else {
            if (!this.getText().equals("Ready")) this.setText("Ready");
          }
        else
          if (!this.getText().equals(menuItemsMainText[0])) this.setText(menuItemsMainText[0]);
      }
    };
    for (int i = 1; i < menuItemsMainText.length - 1; i++)
      this.menuItemsMain[i] = new MenuItem(renderer, menuItemsMainText[i], 10,
          screenHeight - glText.getCharHeight() * (i + 1) * 0.65f
              - (screenHeight - glText.getCharHeight() * menuItemsMainText.length * 0.65f) / 2,
          EdgePoint.BOTTOM_LEFT);
    this.menuItemsMain[4] = new MenuItem(renderer, menuItemsMainText[4], 10,
        screenHeight - glText.getCharHeight() * (4 + 1) * 0.65f
            - (screenHeight - glText.getCharHeight() * menuItemsMainText.length * 0.65f) / 2,
        EdgePoint.BOTTOM_LEFT) {
      @Override
      public void move(double dt) {
        super.move(dt);
        this.setEnabled(originActivity.videoAdIsLoaded());
      }
    };

    // Connect screen buttons.
    this.menuItemsConnect = new MenuItem[8];
    this.menuItemsConnect[0] = new MenuItem(renderer, "Host", 10 + screenWidth,
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f, EdgePoint.BOTTOM_LEFT);
    this.menuItemsConnect[1] = new MenuItem(renderer, "Join", 10 + screenWidth,
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f * 2, EdgePoint.BOTTOM_LEFT);
    this.menuItemsConnect[2] = new MenuItem(renderer, "Bluetooth", screenWidth * 2 - 10,
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f, EdgePoint.BOTTOM_RIGHT);
    this.menuItemsConnect[3] = new MenuItem(renderer, "Wi-Fi", screenWidth * 2 - 10,
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f * 2, EdgePoint.BOTTOM_RIGHT);
    this.menuItemsConnect[4] = new MenuItem(renderer, "Search", screenWidth*2 - 10,
        screenHeight / 8, EdgePoint.BOTTOM_RIGHT);
    this.menuItemsConnect[5] = new MenuItem(renderer, "Devices:", screenWidth + 10,
        screenHeight / 8, EdgePoint.BOTTOM_LEFT);
    this.menuItemsConnect[6] = new MenuItem(renderer, "Start server", screenWidth*1.5f,
        screenHeight / 8, EdgePoint.BOTTOM_CENTER);
    this.menuItemsConnect[7] = new MenuItem(renderer, "", screenWidth*1.5f,
        screenHeight / 8, EdgePoint.BOTTOM_CENTER) {
      @Override
      public void move(double dt) {
        super.move(dt);
        if (originActivity.acceptThread != null) {
          int threads = 0;
          for (ConnectedThread thread : originActivity.connectedThreads)
            if (thread != null)
              threads++;
          if (!this.getText().equals("Connected: " + threads))
            this.setText("Connected: " + threads);
        } else {
          if (!this.getText().equals("")) this.setText("");
        }
      }
    };

    this.bluetoothStatusIcon = new MenuItem(renderer, "none", screenWidth * 2 - 10,
        menuItemsConnect[5].getBottomY(), EdgePoint.BOTTOM_RIGHT) {
      private BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

      @Override
      public void move(double dt) {
        super.move(dt);
        switch(adapter.getScanMode()) {
          case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
            if (!this.getText().equals("c")) this.setText("c"); break;
          case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
            if (!this.getText().equals("c d")) this.setText("c d"); break;
          case BluetoothAdapter.SCAN_MODE_NONE:
            if (!this.getText().equals("none")) this.setText("none"); break;
          default:
            if (!this.getText().equals("idk")) this.setText("idk"); break;
        }
      }
    };

    this.disconnectButton = new MenuItem(renderer, "disconnect", screenWidth * 1.5f,
        screenHeight / 8f, EdgePoint.BOTTOM_CENTER) {
      @Override
      public void move(double dt) {
        super.move(dt);
        this.setDrawable(originActivity.isGuest());
      }
    };
    this.disconnectButton.setAction((action, origin)
        -> originActivity.writeBytesAuto(new byte[] {Protocol.DISCONNECT_REQUEST}));

    bluetoothGuestMenu.add(menuItemsConnect[4]);
    bluetoothGuestMenu.add(menuItemsConnect[5]);
    bluetoothHostMenu.add(bluetoothStatusIcon);
    bluetoothHostMenu.add(menuItemsConnect[6]);
    bluetoothHostMenu.add(menuItemsConnect[7]);
    updateConnectMenuContents();

    // Board screen buttons.
    String[] menuItemsBoardText = {"Hor Squares", "Ver Squares", "Speed", "Stage Borders"};
    this.menuItemsBoard = new MenuItem[menuItemsBoardText.length];
    for (int i = 0; i < menuItemsBoardText.length; i++)
      this.menuItemsBoard[i] = new MenuItem(renderer, menuItemsBoardText[i], 10 + screenWidth,
          screenHeight - glText.getCharHeight() * (i * 5/4 + 1) * 0.65f - screenHeight / 5,
          EdgePoint.BOTTOM_LEFT);

    // Players screen buttons.
    this.menuItemsPlayers = new MenuItem[4];
    for (int i = 0; i < 4; i++)
      this.menuItemsPlayers[i] = new MenuItem(renderer, "Player " + (i + 1),
          10 + screenWidth, screenHeight * 4/5f - glText.getCharHeight() * (i * 5/4f + 1) * 0.65f,
          EdgePoint.BOTTOM_LEFT);

    this.addSnakeButton = new MenuItem(renderer, "+", screenWidth*2 - screenWidth*0.0425f,
        menuItemsPlayers[1].getBottomY(), EdgePoint.BOTTOM_CENTER);

    // Player options screen buttons.
    String[] menuItemsPlayersOptionsText = {"Type"};
    this.menuItemsPlayersOptions = new MenuItem[menuItemsPlayersOptionsText.length];
    for (int i = 0; i < menuItemsPlayersOptionsText.length; i++)
      this.menuItemsPlayersOptions[i] = new MenuItem(renderer, menuItemsPlayersOptionsText[i],
          10 + screenWidth * 2,
          screenHeight * 4/5 - (screenWidth-110)/9 - glText.getCharHeight() * (i + 1) * 0.65f,
          EdgePoint.BOTTOM_LEFT);

    // Set functionality for each menuItem.
    this.menuItemsMain[0].setAction((action, origin) -> {
      OpenGLActivity originActivity = (OpenGLActivity) renderer.getContext();
      if (originActivity.isGuest() || originActivity.isHost())
        originActivity.setReady(!originActivity.isReady());
      //else
        //renderer.startGame(renderer.getMenu().getPlayers());
    });

    this.menuItemsMain[4].setAction((action, origin) -> originActivity.showAd());

    this.menuItemsConnect[0].setAction((action,origin) -> renderer.getMenu()
        .setConnectionRole(ConnectionRole.HOST));
    this.menuItemsConnect[1].setAction((action,origin) -> renderer.getMenu()
        .setConnectionRole(ConnectionRole.GUEST));
    this.menuItemsConnect[2].setAction((action,origin) -> renderer.getMenu()
        .setConnectionType(ConnectionType.BLUETOOTH));
    this.menuItemsConnect[3].setAction((action,origin) -> renderer.getMenu()
        .setConnectionType(ConnectionType.WIFI));
    this.menuItemsConnect[4].setAction((action,origin) -> renderer.getMenu().beginSearch());
    this.menuItemsConnect[6].setAction((action,origin) -> renderer.getMenu().beginHost());

    // Create the graphics.
    this.colorSelectionSquare = new MenuButton[8];
    float squareSize = (screenWidth - 10 - 10*this.colorSelectionSquare.length)
                        / (float) this.colorSelectionSquare.length;
    for (int index = 0; index < this.colorSelectionSquare.length; index++) {
      this.colorSelectionSquare[index]
          = new MenuButton(renderer,
                           screenWidth*2 + 10*(index+1) + squareSize*index,
                           screenHeight - glText.getCharHeight()*0.65f - squareSize,
                           squareSize,
                           squareSize,
                           EdgePoint.BOTTOM_LEFT) {};

      this.colorSelectionSquare[index].setColors(getColorFromIndex(index));
    }
    this.colorSelectionSquare[0].setAction((action, origin)-> renderer.getMenu().onColorSquareTouch(0));
    this.colorSelectionSquare[1].setAction((action, origin)-> renderer.getMenu().onColorSquareTouch(1));
    this.colorSelectionSquare[2].setAction((action, origin)-> renderer.getMenu().onColorSquareTouch(2));
    this.colorSelectionSquare[3].setAction((action, origin)-> renderer.getMenu().onColorSquareTouch(3));
    this.colorSelectionSquare[4].setAction((action, origin)-> renderer.getMenu().onColorSquareTouch(4));
    this.colorSelectionSquare[5].setAction((action, origin)-> renderer.getMenu().onColorSquareTouch(5));
    this.colorSelectionSquare[6].setAction((action, origin)-> renderer.getMenu().onColorSquareTouch(6));
    this.colorSelectionSquare[7].setAction((action, origin)-> renderer.getMenu().onColorSquareTouch(7));

    // Some items should be disabled for online game guests.
    guestDisabledDrawables.add(menuItemsMain[2]);     // The board menu button.
    guestDisabledDrawables.add(menuItemsConnect[0]);  // The 4 connection specifiers.
    guestDisabledDrawables.add(menuItemsConnect[1]);
    guestDisabledDrawables.add(menuItemsConnect[2]);
    guestDisabledDrawables.add(menuItemsConnect[3]);
    guestDisabledDrawables.add(menuItemsPlayers[0]);  // The 4 player config buttons.
    guestDisabledDrawables.add(menuItemsPlayers[1]);  // They can later be re-enabled (by removing
    guestDisabledDrawables.add(menuItemsPlayers[2]);  // them from this list).
    guestDisabledDrawables.add(menuItemsPlayers[3]);

    // Add items to the drawables list for each screen.
    drawablesMain = new ArrayList<>();
    drawablesMain.addAll(Arrays.asList(menuItemsMain));

    drawablesConnect = new CopyOnWriteArrayList<>();
    drawablesConnect.addAll(Arrays.asList(menuItemsConnect));
    drawablesConnect.add(bluetoothStatusIcon);
    drawablesConnect.add(disconnectButton);

    drawablesBoard = new ArrayList<>();
    drawablesBoard.addAll(Arrays.asList(menuItemsBoard));

    drawablesPlayers = new ArrayList<>();
    drawablesPlayers.addAll(Arrays.asList(menuItemsPlayers));
    drawablesPlayers.add(addSnakeButton);

    drawablesPlayersOptions = new ArrayList<>();
    drawablesPlayersOptions.addAll(Arrays.asList(menuItemsPlayersOptions));
    drawablesPlayersOptions.addAll(Arrays.asList(colorSelectionSquare));

    this.gl.glEnable(GL10.GL_TEXTURE_2D);
    this.gl.glEnable(GL10.GL_BLEND);
    this.gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
  }

  public void run(double dt) {
    this.gl.glTranslatef((float) screenTransformX.getTime(),
                         (float) -screenTransformY.getTime(), 0f);
    this.gl.glColor4f(1f, 1f, 1f, 1f);

    //this.gl.glEnable(GL10.GL_BLEND);
    //this.gl.glBlendFunc(GL10.GL_SRC_ALPHA,GL10.GL_ONE_MINUS_SRC_ALPHA);

    // Handle timers.
    if (!this.screenTransformX.isDone()) this.screenTransformX.countEaseOut(dt, 8, screenWidth/2);
    if (!this.screenTransformY.isDone()) this.screenTransformY.countEaseOut(dt, 8, screenHeight/2);
    if (!this.scrollInertia.isDone()) {
      if (renderer.getPointerDownTime() == 0)
        scroll((float) scrollInertia.getTime());
      scrollInertia.countEaseOut(dt, 2, screenHeight/16);
    }
    if (originActivity.isHost() && hostUpdatePacketTimer.count(dt)) {
      hostUpdatePacketTimer.reset();
      for (ConnectedThread thread : originActivity.connectedThreads)
        if (thread != null)
          thread.write(Protocol.encodeSeveralCalls(setupBuffer.allInformationCallList(thread),
                                                   Protocol.BASIC_AGGREGATE_CALL));
    }

    // Draw the background items.
    this.backgroundSnakeTimer.count(dt);
    if (this.backgroundSnakeTimer.isDone()) {
      BackgroundSnake newSnake = new BackgroundSnake(this);
      this.backgroundSnakes.add(newSnake);
      this.backgroundSnakeTimer.reset();
      this.backgroundSnakeTimer.setEndTime(Math.random());
    }
    for (BackgroundSnake backgroundSnake : backgroundSnakes) {
      if (!backgroundSnake.isDead()) {
        backgroundSnake.update(dt);
        backgroundSnake.draw(gl);
      }
    }

    // Transition animation.
    if (transitionAnimation == null) {
      currentScreen.moveAll(dt);
      currentScreen.drawAll();
    } else {
      transitionAnimation.run(dt);
    }
  }

  public void refresh() {
    currentScreen.refresh();
  }

  public void setScreen(MenuScreen screen) { currentScreen = screen; }

  public MenuScreen getCurrentScreen() { return currentScreen; }

  /**
   * Handles moving back one screen.
   */
  public void goBack() {
    currentScreen.goBack();
  }


  // Enables the first currently disabled snake to play and handles the players menu animations.
  /*
  public Player addSnake() {
    int index;
    for (index = 0; index < players.length; index++)
      if (players[index].getControlType() == Player.ControlType.OFF) {
        players[index].setControlType(Player.ControlType.CORNER);
        if (index != players.length - 1)
          addSnakeButton.setDestinationY(menuItemsPlayers[index+1].getBottomY());
        else
          addSnakeButton.setOpacity(0);
        break;
      }

    // Inform everybody of the change if hosting.
    if (originActivity.isHost())
      for (ConnectedThread thread : originActivity.connectedThreads)
        if (thread != null)
          thread.write(getDetailedSnakesList(thread));

    return players[index];
  }*/

  // Disables a given snake from play and handles the players menu animations.
  /*
  public void removeSnake(int snakeIndex) {
    int index;
    for (index = snakeIndex; index < players.length - 1; index++)
      if (players[index + 1].getControlType() != Player.ControlType.OFF)
        swapSnakes(snakeIndex, snakeIndex + 1);
      else
        break;
    players[index].setControlType(Player.ControlType.OFF);
    addSnakeButton.setDestinationY(menuItemsPlayers[index].getBottomY());
    addSnakeButton.setOpacity(1);

    // Inform everybody of the change if hosting.
    if (originActivity.isHost())
      for (ConnectedThread thread : originActivity.connectedThreads)
        if (thread != null)
          thread.write(getDetailedSnakesList(thread));
  }*/

  // Swaps the indices of two snakes in the players menu screen.
//  private void swapSnakes(int firstSnakeIndex, int secondSnakeIndex) {
//    Player playerHolder = players[secondSnakeIndex];
//    players[secondSnakeIndex] = players[firstSnakeIndex];
//    players[firstSnakeIndex] = playerHolder;
//  }

//  public void cyclePlayerControlTypes() {
//    switch (this.players[this.playersOptionsIndex].getControlType()) {
//      case OFF:
//        break;
//      case CORNER:
//        boolean swipeTaken = false;
//        for (Player player : players)
//          if (player.getControlType() == Player.ControlType.SWIPE)
//            swipeTaken = true;
//
//        if (!swipeTaken)
//          this.players[this.playersOptionsIndex].setControlType(Player.ControlType.SWIPE);
//        else
//          this.players[this.playersOptionsIndex].setControlType(Player.ControlType.GAMEPAD);
//        break;
//      case SWIPE:
//        // TODO the next one would be keyboard and then gamepad, but they're not implemented
//        this.players[this.playersOptionsIndex].setControlType(Player.ControlType.GAMEPAD);
//        break;
//      case KEYBOARD:
//        this.players[this.playersOptionsIndex].setControlType(Player.ControlType.CORNER);
//        break;
//      case GAMEPAD:
//        this.players[this.playersOptionsIndex].setControlType(Player.ControlType.CORNER);
//        break;
//      // The following ones you should not be able to easily switch off.
//      case BLUETOOTH:
//        break;
//      case WIFI:
//        break;
//    }
//  }

  // Sets the currently selected player's color to a color from a given color square index.
  private void onColorSquareTouch(int index) {
    // Only guests have to wait for a signal from somewhere else before setting the color.
//    if (!this.isGuest())
//      this.setPlayerColor(playersOptionsIndex, index);

    // If it's an online user have it send the information.
    if (originActivity.isGuest() || originActivity.isHost()) {
      byte protocolId;
      switch (playersOptionsIndex) {
        case 0: protocolId = Protocol.SNAKE_LL_SKIN; break;
        case 1: protocolId = Protocol.SNAKE_UL_SKIN; break;
        case 2: protocolId = Protocol.SNAKE_UR_SKIN; break;
        case 3: protocolId = Protocol.SNAKE_LR_SKIN; break;
        default: return;
      }

//      originActivity.writeBytesAuto(
//          new byte[] {protocolId, (byte) players[playersOptionsIndex].getSkinIndex()});
    }
  }

  /* Sets the currently selected player's control corner to a Corner represented by a corner square.
  private void onCornerSquareTouch(ControllerBuffer.Corner representedCorner) {
    // Only guests have to wait for a signal from somewhere else before setting the corner.
    if (!this.isGuest())
      this.setPlayerCorner(playersOptionsIndex, representedCorner);

    // If it's an online user have it send the information.
    if (originActivity.isGuest() || originActivity.isHost()) {
      byte protocolId;
      switch (playersOptionsIndex) {
        case 0: protocolId = Protocol.SNAKE1_CORNER_CHANGED; break;
        case 1: protocolId = Protocol.SNAKE2_CORNER_CHANGED; break;
        case 2: protocolId = Protocol.SNAKE3_CORNER_CHANGED; break;
        case 3: protocolId = Protocol.SNAKE4_CORNER_CHANGED; break;
        default: return;
      }

      originActivity.writeBytesAuto(
          new byte[] {protocolId, Protocol.encodeCorner(representedCorner)});
    }
  }*/

//  public void setPlayerCorner(int playerIndex, ControllerBuffer.Corner corner) {
//    // Find the other player that uses the selected corner and set it to the current player's.
//    for (int index = 0; index < this.players.length; index++)
//      if (index != playerIndex && this.players[index].getControlCorner() == corner);
//  }

  // Sets the connection type to a given value and handles the connection menu animation.
  public void setConnectionType(ConnectionType type) {
    this.connectionType = type;
    if (type == null) {
      this.menuItemsConnect[2].setOpacity(1);
      this.menuItemsConnect[3].setOpacity(1);
    } else if (type == ConnectionType.BLUETOOTH) {
      this.menuItemsConnect[2].setOpacity(1);
      this.menuItemsConnect[3].setOpacity(0.25f);
    } else if (type == ConnectionType.WIFI) {
      this.menuItemsConnect[2].setOpacity(0.25f);
      this.menuItemsConnect[3].setOpacity(1);
    }
    updateConnectMenuContents();
  }
  // Likewise, but for connection roles.
  public void setConnectionRole(ConnectionRole role) {
    this.connectionRole = role;
    if (role == null) {
      this.menuItemsConnect[0].setOpacity(1);
      this.menuItemsConnect[1].setOpacity(1);
    } else if (role == ConnectionRole.HOST) {
      this.menuItemsConnect[0].setOpacity(1);
      this.menuItemsConnect[1].setOpacity(0.25f);
    } else if (role == ConnectionRole.GUEST) {
      this.menuItemsConnect[0].setOpacity(0.25f);
      this.menuItemsConnect[1].setOpacity(1);
    }
    updateConnectMenuContents();
  }
  // Updates the visible contents of the "connect" menu based on the selected role and type.
  public void updateConnectMenuContents() {
    // Set all to not drawable.
    for (MenuDrawable drawable : bluetoothHostMenu) drawable.setDrawable(false);
    for (MenuDrawable drawable : bluetoothGuestMenu) drawable.setDrawable(false);
    // Update based on state.
    if (connectionType == ConnectionType.BLUETOOTH) {
      if (connectionRole == ConnectionRole.HOST)
        for (MenuDrawable drawable : bluetoothHostMenu) drawable.setDrawable(true);
      else if (connectionRole == ConnectionRole.GUEST)
        for (MenuDrawable drawable : bluetoothGuestMenu) drawable.setDrawable(true);
    }
  }

  // Adds all the paired devices to the list and displays them.
  public void updatePairedDevices() {
    Set<BluetoothDevice> bondedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
    // Add the new search results.
    for (BluetoothDevice device : bondedDevices) {
      if (!pairedDevices.has(device)) {
        MenuItem deviceItem = new MenuItem(renderer, device.getName(), screenWidth + 10,
            menuItemsConnect[5].getBottomY() - (pairedDevices.size() + 1) * glText.getCharHeight() * 0.65f * 5 / 4,
            EdgePoint.BOTTOM_LEFT);
        deviceItem.setDescription(device.getAddress());
        deviceItem.setAction((action,origin)
            -> renderer.getMenu().connectToDeviceViaItem((MenuItem) origin));
        pairedDevices.add(device, deviceItem);
      }
    }
  }

  // Adds a newly found device to the list and displays it.
  public void addFoundDevice(BluetoothDevice device) {
    if (!foundDevices.has(device) && !pairedDevices.has(device)) {
      // Create a new item representing that device.
      MenuItem deviceItem = new MenuItem(renderer,
          (device.getName() != null) ? device.getName() : device.getAddress(),
          screenWidth + 10,
          menuItemsConnect[5].getBottomY() - (pairedDevices.size() + foundDevices.size() + 1)
              * glText.getCharHeight() * 0.65f * 5 / 4, EdgePoint.BOTTOM_LEFT);
      deviceItem.setDescription(device.getAddress());
      deviceItem.setAction((action,origin)
          -> renderer.getMenu().connectToDeviceViaItem((MenuItem) origin));
      foundDevices.add(device, deviceItem);
      bluetoothGuestMenu.add(deviceItem);
      drawablesConnect.add(deviceItem);
    } else {
      // The device is already present in either foundDevices or pairedDevices, so update its
      // information.
      if (foundDevices.has(device)) {
        foundDevices.getItem(device)
            .setText((device.getName() != null) ? device.getName() : device.getAddress());
      } else {
        pairedDevices.getItem(device)
            .setText((device.getName() != null) ? device.getName() : device.getAddress());
      }
    }
  }

  // Finds the device corresponding to a given menu item and connects to it.
  private void connectToDeviceViaItem(MenuItem item) {
    BluetoothDevice targetDevice;
    if (   ((targetDevice = foundDevices.getDevice(item)) != null)
        || ((targetDevice = pairedDevices.getDevice(item)) != null)) {
      // Set a fullscreen loading message.
      FullscreenMessage loadingMessage =
          new FullscreenMessage(renderer, "Connecting to " + item.getText()) {
            @Override
            public void onCancel() {
              super.onCancel();
              originActivity.connectThread.cancel();
              originActivity.connectThread = null;
            }
          }.withLoadingSnake(true);

      originActivity.connectThread = new ConnectThread(originActivity, targetDevice,loadingMessage);
      originActivity.connectThread.start();

      setScreen(new MultiplayerSnakeOverviewScreen(this));
    }
  }

  // Begins the search for nearby devices.
  public void beginSearch() {
    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    if (adapter == null) {
      // There is no bluetooth adapter, so don't do anything.
    } else {
      if (!adapter.isEnabled()) {
        // There is an adapter, but it's not enabled.
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        originActivity.startActivityForResult(enableBtIntent, originActivity.REQUEST_ENABLE_BT);
      }
      // Add all the already paired devices to the list.
      this.updatePairedDevices();
      // Begin device discovery.
      ActivityCompat.requestPermissions(originActivity,
          new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
    }
  }

  // Begins the hosting of a bluetooth game.
  public void beginHost() {
    if (BluetoothAdapter.getDefaultAdapter().getScanMode()
        != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
      // Make discoverable for 5 minutes if not discoverable already.
      Intent discoverableIntent =
          new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
      discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
      originActivity.startActivity(discoverableIntent);
    }

    // Begin the thread for accepting devices.
    originActivity.acceptThread = new AcceptThread(originActivity);
    originActivity.acceptThread.start();
  }

  // Cancels the hosting of a bluetooth game.
  public void stopHost() {
    if (originActivity.acceptThread != null) {
      originActivity.acceptThread.cancel();
      originActivity.acceptThread = null;
    }

    // Shut down all connections.
    for (int index = 0; index < originActivity.connectedThreads.length; index++)
      if (originActivity.connectedThreads[index] != null) {
        originActivity.connectedThreads[index].write(new byte[] {Protocol.DISCONNECT});
        originActivity.awaitingDisconnectThreads.add(originActivity.connectedThreads[index]);
      }
  }

  // Sets up the menu to work as if you're a guest.
  public void beginGuest() {
    // Clear all corners.
    for (ControllerBuffer.Corner corner : ControllerBuffer.Corner.values())
      setupBuffer.cornerMap.emptyCorner(corner);
  }

  // Sets up the menu to work as if you're not a guest anymore.
  public void endGuest() {
    // Turn all players back to off (except the first one). TODO Make it revert to saved.
  }

  public void handleInputBytes(byte[] inputBytes, ConnectedThread sourceThread) {
    Player playerLL = setupBuffer.cornerMap.getPlayer(ControllerBuffer.Corner.LOWER_LEFT);
    Player playerUL = setupBuffer.cornerMap.getPlayer(ControllerBuffer.Corner.UPPER_LEFT);
    Player playerUR = setupBuffer.cornerMap.getPlayer(ControllerBuffer.Corner.UPPER_RIGHT);
    Player playerLR = setupBuffer.cornerMap.getPlayer(ControllerBuffer.Corner.LOWER_RIGHT);
    switch(inputBytes[0]) {
      case Protocol.HOR_SQUARES_CHANGED: setupBuffer.gameMode.horizontalSquares.set(inputBytes[1]); break;
      case Protocol.VER_SQUARES_CHANGED: setupBuffer.gameMode.verticalSquares.set(inputBytes[1]); break;
      case Protocol.SPEED_CHANGED: setupBuffer.gameMode.speed.set(inputBytes[1]); break;
      case Protocol.STAGE_BORDERS_CHANGED: setupBuffer.gameMode.stageBorders.set(inputBytes[1] == 1); break;

      case Protocol.GAME_MODE:
        setupBuffer.setGameMode(IDGenerator.getGameModeClass(inputBytes[1]));
        break;

      case Protocol.SNAKE_LL_SKIN:
        if (playerLL != null)
          playerLL.setSkin(SnakeSkin.allSkins.get(inputBytes[1]));
        break;
      case Protocol.SNAKE_UL_SKIN:
        if (playerUL != null)
          playerUL.setSkin(SnakeSkin.allSkins.get(inputBytes[1]));
        break;
      case Protocol.SNAKE_UR_SKIN:
        if (playerUR != null)
          playerUR.setSkin(SnakeSkin.allSkins.get(inputBytes[1]));
        break;
      case Protocol.SNAKE_LR_SKIN:
        if (playerLR != null)
          playerLR.setSkin(SnakeSkin.allSkins.get(inputBytes[1]));
        break;

      case Protocol.SNAKE_LL_NAME:
        if (playerLL != null)
          playerLL.setName(new String(inputBytes, 1, inputBytes.length - 1));
        break;
      case Protocol.SNAKE_UL_NAME:
        if (playerUL != null)
          playerUL.setName(new String(inputBytes, 1, inputBytes.length - 1));
        break;
      case Protocol.SNAKE_UR_NAME:
        if (playerUR != null)
          playerUR.setName(new String(inputBytes, 1, inputBytes.length - 1));
        break;
      case Protocol.SNAKE_LR_NAME:
        if (playerLR != null)
          playerLR.setName(new String(inputBytes, 1, inputBytes.length - 1));
        break;

      // HOST ONLY
      case Protocol.REQUEST_ADD_SNAKE:
        if (originActivity.isHost()) {
          ControllerBuffer.Corner requestedCorner = Protocol.decodeCorner(inputBytes[1]);
          if (setupBuffer.cornerMap.getPlayer(requestedCorner) == null) {
            Player addedSnake = new Player(renderer);
            addedSnake
                .setControllerForced(new BluetoothControllerBuffer(addedSnake, sourceThread));
            addedSnake.setName(sourceThread.device.getName());
            setupBuffer.cornerMap.addPlayer(addedSnake, requestedCorner);
          }
        }
        break;

      // GUEST ONLY
      case Protocol.DETAILED_SNAKES_LIST:
        if (this.isGuest()) {
          int index = 0;
          for (ControllerBuffer.Corner corner : ControllerBuffer.Corner.values()) {
            index++;
            Player player = setupBuffer.cornerMap.getPlayer(corner);

            switch (inputBytes[index]) {
              case Protocol.DSL_SNAKE_OFF:
                setupBuffer.cornerMap.emptyCorner(corner);
                break;
              case Protocol.DSL_SNAKE_LOCAL:
                if (player == null) {
                  Player playerToBeAdded = new Player(renderer).defaultPreset(setupBuffer);
                  setupBuffer.cornerMap.addPlayer(playerToBeAdded, corner);
                } else {
                  player.setController(new CornerLayoutControllerBuffer(player));
                }
                break;
              case Protocol.DSL_SNAKE_REMOTE:
                if (player == null) {
                  Player playerToBeAdded = new Player(renderer);
                  playerToBeAdded.setControllerForced(new BluetoothControllerBuffer(playerToBeAdded,
                                                                                    sourceThread));
                  setupBuffer.cornerMap.addPlayer(playerToBeAdded, corner);
                } else {
                  player.setControllerForced(new BluetoothControllerBuffer(player, sourceThread));
                }
                break;
              default:
                break;
            }
          }
        }
        break;

      case Protocol.BASIC_AGGREGATE_CALL:
        for (byte[] call : Protocol.decodeSeveralCalls(inputBytes))
          if (call.length > 0)
            renderer.handleInputBytes(call, sourceThread);
        break;

      case Protocol.GAME_START_CALL:
        if (this.isGuest()) {
          // Tell the host you've received the aggregate call.
          sourceThread.write(new byte[] {Protocol.GAME_START_RECEIVED});

          // Decode all calls and execute them.
          inputBytes[0] = Protocol.BASIC_AGGREGATE_CALL;
          renderer.handleInputBytes(inputBytes, sourceThread);

          // Start the game.
          renderer.startGame(new GuestGame(setupBuffer.cornerMap,
                                           setupBuffer.gameMode.horizontalSquares.get(),
                                           setupBuffer.gameMode.verticalSquares.get(),
                                           setupBuffer.gameMode.speed.get(),
                                           setupBuffer.gameMode.stageBorders.get(),
                                           null // TODO get the entity blueprints somehow
                                           ));
        }
        break;

      default: break;
    }
  }

  // Takes a color index and returns the whole corresponding color as rgba.
  public static float[] getColorFromIndex(int index) {
    switch(index) {
      case 0: return new float[] {1f, 1f, 1f, 1f};
      case 1: return new float[] {1f, 0f, 0f, 1f};
      case 2: return new float[] {1f, 0.5f, 0f, 1f};
      case 3: return new float[] {1f, 1f, 0f, 1f};
      case 4: return new float[] {0f, 1f, 0f, 1f};
      case 5: return new float[] {0f, 1f, 1f, 1f};
      case 6: return new float[] {0f, 0f, 1f, 1f};
      case 7: return new float[] {1f, 0f, 1f, 1f};
      default: return new float[] {1f, 1f, 1f, 1f};
    }
  }

  // Tells if the current screen should be scrollable or locked.
  public boolean isScrollable() {
    return this.getScrollHeight() < 0 || screenTransformY.getTime() < this.getScrollHeight();
  }

  // Gets the lowest point of the lowest item drawn on screen.
  public float getScrollHeight() {
    float minHeight = 0;
    for (MenuDrawable drawable : currentScreen.getAllDrawables())
      if (drawable.isDrawable() && drawable.getBottomY() < minHeight) minHeight = drawable.getBottomY();
    return minHeight;
  }

  // Performs a screen scroll vertically by a given amount.
  public void scroll(float amount) {
    this.screenTransformY.setTime(Math.max(this.getScrollHeight(),
                                  Math.min(0, amount + screenTransformY.getTime())));
  }

  // Sets the scroll inertia so that the screen moves with a bit of "inertia" that slowly decreases.
  public void setScrollInertia(float inertia) {
    scrollInertia.setTime(inertia);
    scrollInertia.setEndTimeFromNow(0.0);
  }

  // More getters.
  public float getScreenTransformX() { return (float) this.screenTransformX.getTime(); }
  public float getScreenTransformY() { return (float) this.screenTransformY.getTime(); }
  public GameRenderer getRenderer() { return this.renderer; }
  public OpenGLActivity getOriginActivity() { return this.originActivity; }

  public GameSetupBuffer getSetupBuffer() { return setupBuffer; }

  // Protocol simplifier getters.
  public boolean isGuest() { return originActivity.isGuest(); }

//  public byte[] getDetailedSnakesList(ConnectedThread thread) {
//    byte[] output = new byte[5];
//    output[0] = Protocol.DETAILED_SNAKES_LIST;
//    int outputIndex = 1;
//
//    for (Player player : players) {
//      if (player == null || player.getControlType().equals(Player.ControlType.OFF))
//        output[outputIndex++] = Protocol.DSL_SNAKE_OFF;
//      else if (player.getControlType().equals(Player.ControlType.BLUETOOTH)
//            && player.getControllerThread().equals(thread))
//        output[outputIndex++] = Protocol.DSL_SNAKE_LOCAL;
//      else
//        output[outputIndex++] = Protocol.DSL_SNAKE_REMOTE;
//    }
//
//    return output;
//  }
}



// Backgrounds snakes which will appear randomly on the screen going from left to right.
class BackgroundSnake {
  private final float size, speed, initialY;
  private final int length;
  private float x;
  private Menu menu;
  private double movementTimer;
  private boolean dead = false;
  private Mesh snakeMesh;

  // Constructor.
  public BackgroundSnake(Menu menu) {
    this.menu = menu;
    float screenHeight = this.menu.getRenderer().getScreenHeight();

    // The z-index is chosen randomly. It represents how close the snake will appear to be.
    // It can take whole values in the range 0 - 20.
    int zIndex = (int)Math.floor(Math.random()*105 % 21);

    this.size = screenHeight * 0.05f + zIndex / 20f * screenHeight * 0.1f;
    this.length = 4 + (int) ((20 - zIndex) / 4f * Math.random()) + (int) Math.round(Math.random()*6);
    this.speed = 1f / (5 + zIndex * 5f/20f + Math.round(Math.random()*5));
    this.initialY = screenHeight - size / 2f - (float) (screenHeight * Math.random());
    this.movementTimer = speed;
    this.x = -((size + 1) * length);

    snakeMesh = new Mesh(menu.getRenderer(), x, initialY, EdgePoint.BOTTOM_LEFT, size, length, 1,
                         new GameTextureMap(SnakeSkin.white));
    for (int index = 0; index < length - 1; index++) {
      snakeMesh.updateColors(index, 0.25f, 0.25f, 0.25f, 0.5f);
      snakeMesh.updateTextures(index, 0, snakeMesh.textureMap
                                                  .getTexture(SnakeSkin.white,
                                                              SnakeSkin.TextureType.BODY,
                                                              Snake.Direction.UP));
    }
    snakeMesh.updateColors(length - 1, 1f, 1f, 1f, 0.5f);
    snakeMesh.updateTextures(length - 1, 0, snakeMesh.textureMap
                                                     .getTexture(SnakeSkin.white,
                                                                 SnakeSkin.TextureType.HEAD,
                                                                 Snake.Direction.UP));
  }

  // Moves the snake whenever it is time to be moved.
  public void update(double dt) {
    if (!dead) {
      this.movementTimer -= dt;
      while (this.movementTimer < 0) {
        this.movementTimer += speed;
        this.x += this.size;
      }

      if (x > menu.getRenderer().getScreenWidth() * 5) {
        this.dead = true;
        tidy();
      }
    }
  }

  public void draw(GL10 gl) {
    gl.glPushMatrix();
    gl.glTranslatef(x, 0, 0);
    snakeMesh.draw();
    gl.glPopMatrix();
  }

  private void tidy() {
    snakeMesh.recycle();
  }

  public boolean isDead() { return this.dead; }
}



// A map that holds unique devices and their respective menu items.
class DeviceItemMap{
  private ArrayList<Pair<BluetoothDevice, MenuItem>> deviceMap = new ArrayList<>();

  public DeviceItemMap() {
  }

  public boolean add(BluetoothDevice device, MenuItem deviceItem) {
    if (this.has(device)) return false;

    deviceMap.add(new Pair<>(device, deviceItem));
    return true;
  }

  public boolean has(BluetoothDevice device) {
    for (Pair<BluetoothDevice, MenuItem> pair : deviceMap)
      if (device.getAddress().equals(pair.first.getAddress()))
        return true;

    return false;
  }

  public int size() { return deviceMap.size(); }

  public List<MenuDrawable> getItems() {
    List<MenuDrawable> itemsList = new ArrayList<>();
    for (Pair<BluetoothDevice, MenuItem> pair : deviceMap)
      itemsList.add(pair.second);
    return itemsList;
  }

  public MenuItem getItem(BluetoothDevice device) {
    for (Pair<BluetoothDevice, MenuItem> pair : deviceMap)
      if (device.getAddress().equals(pair.first.getAddress()))
        return pair.second;
    return null;
  }

  public BluetoothDevice getDevice(MenuItem item) {
    for (Pair<BluetoothDevice, MenuItem> pair : deviceMap)
      if (item.equals(pair.second))
        return pair.first;
    return null;
  }
}