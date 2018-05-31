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

/**
 * Created by Nick on 14/12/2017.
 */

// And instance of this class would be a single menu with all menu items.
public class Menu {
  private SimpleTimer screenTransformX, screenTransformY;
  private int currentScreen = 0;
  private float screenWidth, screenHeight;
  private GameRenderer renderer;
  private Player[] players = new Player[4];
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
  private final DeviceItemMap pairedDevices = new DeviceItemMap();
  private final DeviceItemMap foundDevices = new DeviceItemMap();
  private final List<MenuDrawable> bluetoothHostMenu = new ArrayList<>();
  private final List<MenuDrawable> bluetoothGuestMenu = new ArrayList<>();
  private final List<MenuDrawable> guestDisabledDrawables = new ArrayList<>();
  private final MenuDrawable bluetoothStatusIcon, readyDevicesCounter;

  private final MenuImage[] colorSelectionSquare, cornerSelectionSquare;
  private final MenuItem menuStateItem, addSnakeButton;
  private final MenuButtonRemoveSnake[] removeSnakeButtons = new MenuButtonRemoveSnake[4];
  public enum MenuState {MAIN, CONNECT, BOARD, PLAYERS, PLAYERSOPTIONS};
  private String[] menuItemStateNames = {"", "Connect", "Board", "Players", ""};
  private MenuState menuState = MenuState.MAIN, menuStatePrevious = MenuState.MAIN;
  private int playersOptionsIndex, expandedItemIndex = -1;
  private SimpleTimer menuAnimationTimer = new SimpleTimer(0.0, 1.0);
  private SimpleTimer backgroundSnakeTimer = new SimpleTimer(0.0, 0.5 + Math.random());
  private LinkedList<BackgroundSnake> backgroundSnakes = new LinkedList<>();

  private int onlineIdentifier;
  private final OpenGLES20Activity originActivity;

  // Menu variables
  public int horizontalSquares, verticalSquares, speed;
  public boolean stageBorders;

  // TODO Find a way to not use this, currently used for the background snakes, they refuse to render without it.
  private Square testSquare = new Square(500,0,0,0);

  // Constructor.
  public Menu(GameRenderer renderer, float screenWidth, float screenHeight) {
    this.renderer = renderer;
    this.gl = renderer.getGl();
    this.glText = renderer.getGlText();
    this.originActivity = (OpenGLES20Activity) renderer.getContext();

    this.screenTransformX = new SimpleTimer(0.0);
    this.screenTransformY = new SimpleTimer(0.0);
    this.menuState = MenuState.MAIN;
    this.menuStateItem = new MenuItem(renderer, menuItemStateNames[0], 0, 0,
        MenuItem.Alignment.RIGHT);
    this.menuStateItem.setEaseOutVariables(32, this.menuStateItem.getHeight() * 2);

    this.screenWidth = screenWidth;
    this.screenHeight = screenHeight;

    // TODO get these values from the options strings.
    this.horizontalSquares = 20;
    this.verticalSquares = 20;
    this.speed = 12;
    this.stageBorders = true;

    // Initialize the players. TODO get the snake values from the options strings.
    for (int index = 0; index < players.length; index++) players[index] = new Player();
    this.players[0].setName("Player 1");
    this.players[1].setName("Player 2");
    this.players[2].setName("Player 3");
    this.players[3].setName("Player 4");
    this.players[0].setCorner(PlayerController.Corner.LOWER_LEFT);
    this.players[1].setCorner(PlayerController.Corner.LOWER_RIGHT);
    this.players[2].setCorner(PlayerController.Corner.UPPER_LEFT);
    this.players[3].setCorner(PlayerController.Corner.UPPER_RIGHT);

    // Create menuItem instances for each button.
    // Main screen buttons.
    String[] menuItemsMainText = {"Play", "Connect", "Board", "Players", "Watch ad"};
    this.menuItemsMain = new MenuItem[menuItemsMainText.length];
    this.menuItemsMain[0] = new MenuItem(renderer, menuItemsMainText[0], 10,
        screenHeight - glText.getCharHeight() * 0.65f
            - (screenHeight - glText.getCharHeight() * menuItemsMain.length * 0.65f) / 2,
        MenuItem.Alignment.LEFT) {
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
    for (int i = 1; i < menuItemsMainText.length; i++)
      this.menuItemsMain[i] = new MenuItem(renderer, menuItemsMainText[i], 10,
          screenHeight - glText.getCharHeight() * (i + 1) * 0.65f
              - (screenHeight - glText.getCharHeight() * menuItemsMainText.length * 0.65f) / 2,
          MenuItem.Alignment.LEFT);

    this.readyDevicesCounter = new MenuItem(renderer, "", screenWidth - 10,
        menuItemsMain[0].getY(), MenuItem.Alignment.RIGHT) {
      private int readyDevices = 0;
      private int connectedDevices = 0;

      @Override
      public void move(double dt) {
        super.move(dt);
        if (originActivity.isGuest() || originActivity.isHost()) {
          if (originActivity.getNumberOfReadyRemoteDevices() != readyDevices
              || originActivity.getNumberOfRemoteDevices() != connectedDevices) {
            this.setText(originActivity.getNumberOfReadyRemoteDevices() + " / "
                + originActivity.getNumberOfRemoteDevices());

            if (readyDevices == connectedDevices && readyDevices > 1) {
              // Everyone is ready - begin game.
              renderer.startGame(players);
            }
          }
        } else {
          if (!this.getText().equals("")) this.setText("");
        }
      }
    };

    // Connect screen buttons.
    this.menuItemsConnect = new MenuItem[8];
    this.menuItemsConnect[0] = new MenuItem(renderer, "Host", 10 + screenWidth,
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f, MenuItem.Alignment.LEFT);
    this.menuItemsConnect[1] = new MenuItem(renderer, "Join", 10 + screenWidth,
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f * 2, MenuItem.Alignment.LEFT);
    this.menuItemsConnect[2] = new MenuItem(renderer, "Bluetooth", screenWidth * 2 - 10,
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f, MenuItem.Alignment.RIGHT);
    this.menuItemsConnect[3] = new MenuItem(renderer, "Wi-Fi", screenWidth * 2 - 10,
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f * 2, MenuItem.Alignment.RIGHT);
    this.menuItemsConnect[4] = new MenuItem(renderer, "Search", screenWidth*1.5f,
        screenHeight / 8 - glText.getCharHeight() * 0.65f, MenuItem.Alignment.CENTER);
    this.menuItemsConnect[5] = new MenuItem(renderer, "Devices:", screenWidth + 10,
        screenHeight / 8, MenuItem.Alignment.LEFT);
    this.menuItemsConnect[6] = new MenuItem(renderer, "Start server", screenWidth*1.5f,
        screenHeight / 8, MenuItem.Alignment.CENTER);
    this.menuItemsConnect[7] = new MenuItem(renderer, "", screenWidth*1.5f,
        screenHeight / 8, MenuItem.Alignment.CENTER) {
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
        menuItemsConnect[5].getY(), MenuItem.Alignment.RIGHT) {
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
          MenuItem.Alignment.LEFT);

    // Players screen buttons.
    this.menuItemsPlayers = new MenuItem[4];
    for (int i = 0; i < 4; i++)
      this.menuItemsPlayers[i] = new MenuItem(renderer, "Player " + (i + 1),
          10 + screenWidth, screenHeight * 4/5f - glText.getCharHeight() * (i * 5/4f + 1) * 0.65f,
          MenuItem.Alignment.LEFT);

    this.addSnakeButton = new MenuItem(renderer, "+", screenWidth*2 - screenWidth*0.0425f,
        menuItemsPlayers[1].getY(), MenuItem.Alignment.CENTER);
    for (int index = 0; index < removeSnakeButtons.length; index++)
      this.removeSnakeButtons[index]
          = new MenuButtonRemoveSnake(renderer, menuItemsPlayers[index]);

    // Player options screen buttons.
    String[] menuItemsPlayersOptionsText = {"Type"};
    this.menuItemsPlayersOptions = new MenuItem[menuItemsPlayersOptionsText.length];
    for (int i = 0; i < menuItemsPlayersOptionsText.length; i++)
      this.menuItemsPlayersOptions[i] = new MenuItem(renderer, menuItemsPlayersOptionsText[i],
          10 + screenWidth * 2,
          screenHeight * 4/5 - (screenWidth-110)/9 - glText.getCharHeight() * (i + 1) * 0.65f,
          MenuItem.Alignment.LEFT);

    // Set functionality for each menuItem.
    this.menuItemsMain[0].setAction((action, origin) -> {
      OpenGLES20Activity originActivity = (OpenGLES20Activity) renderer.getContext();
      if (originActivity.isGuest() || originActivity.isHost())
        originActivity.setReady(!originActivity.isReady());
      else
        renderer.startGame(renderer.getMenu().getPlayers());
    });

    this.menuItemsMain[1].setAction((action, origin) -> renderer.setMenuState(origin, MenuState.CONNECT));
    this.menuItemsMain[2].setAction((action, origin) -> renderer.setMenuState(origin, MenuState.BOARD));
    this.menuItemsMain[3].setAction((action, origin) -> renderer.setMenuState(origin, MenuState.PLAYERS));

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

    this.menuItemsBoard[0].setAction((action, origin) -> renderer.getMenu().expandItem(0));
    this.menuItemsBoard[1].setAction((action, origin) -> renderer.getMenu().expandItem(1));
    this.menuItemsBoard[2].setAction((action, origin) -> renderer.getMenu().expandItem(2));
    this.menuItemsBoard[3].setAction((action, origin) -> renderer.getMenu().expandItem(3));

    this.menuItemsBoard[0].setValue(new MenuValue(this.renderer, this.horizontalSquares,
        screenWidth * 2 - 10,
        this.menuItemsBoard[0].getY(), MenuItem.Alignment.RIGHT));
    this.menuItemsBoard[1].setValue(new MenuValue(this.renderer, this.verticalSquares,
        screenWidth * 2 - 10,
        this.menuItemsBoard[1].getY(), MenuItem.Alignment.RIGHT));
    this.menuItemsBoard[2].setValue(new MenuValue(this.renderer, this.speed,
        screenWidth * 2 - 10,
        this.menuItemsBoard[2].getY(), MenuItem.Alignment.RIGHT));
    this.menuItemsBoard[3].setValue(new MenuValue(this.renderer, this.stageBorders,
        screenWidth * 2 - 10,
        this.menuItemsBoard[3].getY(), MenuItem.Alignment.RIGHT));

    this.menuItemsBoard[0].getValue().setBoundaries(1, 100);
    this.menuItemsBoard[1].getValue().setBoundaries(1, 100);
    this.menuItemsBoard[2].getValue().setBoundaries(1, 100);

    this.menuItemsPlayers[0].setAction((action, origin) -> renderer.setMenuStateToPlayerOptions(0));
    this.menuItemsPlayers[1].setAction((action, origin) -> renderer.setMenuStateToPlayerOptions(1));
    this.menuItemsPlayers[2].setAction((action, origin) -> renderer.setMenuStateToPlayerOptions(2));
    this.menuItemsPlayers[3].setAction((action, origin) -> renderer.setMenuStateToPlayerOptions(3));

    this.addSnakeButton.setAction((action, origin) -> renderer.getMenu().addSnake());
    this.removeSnakeButtons[0].setAction((action, origin) -> renderer.getMenu().removeSnake(0));
    this.removeSnakeButtons[1].setAction((action, origin) -> renderer.getMenu().removeSnake(1));
    this.removeSnakeButtons[2].setAction((action, origin) -> renderer.getMenu().removeSnake(2));
    this.removeSnakeButtons[3].setAction((action, origin) -> renderer.getMenu().removeSnake(3));

    this.menuItemsPlayersOptions[0]
        .setAction((action, origin) -> renderer.getMenu().cyclePlayerControlTypes());

    this.menuItemsPlayersOptions[0].setValue(new MenuValue(this.renderer, "",
        screenWidth * 3 - 10, this.menuItemsPlayersOptions[0].getY(), MenuItem.Alignment.RIGHT));

    // Create the graphics.
    this.colorSelectionSquare = new MenuImage[8];
    float squareSize = (screenWidth - 10 - 10*this.colorSelectionSquare.length)
                        / (float) this.colorSelectionSquare.length;
    for (int index = 0; index < this.colorSelectionSquare.length; index++) {
      this.colorSelectionSquare[index] = new MenuImage(this.renderer,
          screenWidth*2 + 10*(index+1) + squareSize*index,
          screenHeight - glText.getCharHeight()*0.65f - squareSize, squareSize, squareSize);
      this.colorSelectionSquare[index].setColors(getColorFromIndex(index));
    }
    this.colorSelectionSquare[0].setAction((action, origin)-> renderer.getMenu().setPlayerColor(0));
    this.colorSelectionSquare[1].setAction((action, origin)-> renderer.getMenu().setPlayerColor(1));
    this.colorSelectionSquare[2].setAction((action, origin)-> renderer.getMenu().setPlayerColor(2));
    this.colorSelectionSquare[3].setAction((action, origin)-> renderer.getMenu().setPlayerColor(3));
    this.colorSelectionSquare[4].setAction((action, origin)-> renderer.getMenu().setPlayerColor(4));
    this.colorSelectionSquare[5].setAction((action, origin)-> renderer.getMenu().setPlayerColor(5));
    this.colorSelectionSquare[6].setAction((action, origin)-> renderer.getMenu().setPlayerColor(6));
    this.colorSelectionSquare[7].setAction((action, origin)-> renderer.getMenu().setPlayerColor(7));

    this.cornerSelectionSquare = new MenuImage[4];
    squareSize = (screenWidth*0.6f - 10 - 10*this.cornerSelectionSquare.length)
                  / (float) this.cornerSelectionSquare.length;
    for (int index = 0; index < this.cornerSelectionSquare.length; index++) {
      this.cornerSelectionSquare[index] = new MenuImage(this.renderer,
          screenWidth*2.2f + 10*(index+1) + squareSize*index,
          this.menuItemsPlayersOptions[this.menuItemsPlayersOptions.length - 1].getY() - squareSize,
          squareSize, squareSize);
    }
    this.cornerSelectionSquare[0].setGraphic(R.drawable.lowerleft);
    this.cornerSelectionSquare[1].setGraphic(R.drawable.upperleft);
    this.cornerSelectionSquare[2].setGraphic(R.drawable.upperright);
    this.cornerSelectionSquare[3].setGraphic(R.drawable.lowerright);
    this.cornerSelectionSquare[0].setAction((action, origin)
        -> renderer.getMenu().setPlayerControlCorner(PlayerController.Corner.LOWER_LEFT));
    this.cornerSelectionSquare[1].setAction((action, origin)
        -> renderer.getMenu().setPlayerControlCorner(PlayerController.Corner.UPPER_LEFT));
    this.cornerSelectionSquare[2].setAction((action, origin)
        -> renderer.getMenu().setPlayerControlCorner(PlayerController.Corner.UPPER_RIGHT));
    this.cornerSelectionSquare[3].setAction((action, origin)
        -> renderer.getMenu().setPlayerControlCorner(PlayerController.Corner.LOWER_RIGHT));

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
    guestDisabledDrawables.add(removeSnakeButtons[0]);// The 4 remove snakes buttons.
    guestDisabledDrawables.add(removeSnakeButtons[1]);
    guestDisabledDrawables.add(removeSnakeButtons[2]);
    guestDisabledDrawables.add(removeSnakeButtons[3]);

    // Add items to the drawables list for each screen.
    drawablesMain = new ArrayList<>();
    drawablesMain.addAll(Arrays.asList(menuItemsMain));
    drawablesMain.add(readyDevicesCounter);

    drawablesConnect = new CopyOnWriteArrayList<>();
    drawablesConnect.addAll(Arrays.asList(menuItemsConnect));
    drawablesConnect.add(bluetoothStatusIcon);

    drawablesBoard = new ArrayList<>();
    drawablesBoard.addAll(Arrays.asList(menuItemsBoard));

    drawablesPlayers = new ArrayList<>();
    drawablesPlayers.addAll(Arrays.asList(menuItemsPlayers));
    drawablesPlayers.add(addSnakeButton);
    drawablesPlayers.addAll(Arrays.asList(removeSnakeButtons));

    drawablesPlayersOptions = new ArrayList<>();
    drawablesPlayersOptions.addAll(Arrays.asList(menuItemsPlayersOptions));
    drawablesPlayersOptions.addAll(Arrays.asList(colorSelectionSquare));
    drawablesPlayersOptions.addAll(Arrays.asList(cornerSelectionSquare));

    // Add one snake to initialize the add and remove buttons.
    this.addSnake();

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
    if (!this.menuAnimationTimer.isDone()) this.menuAnimationTimer.countEaseOut(dt, 3, 0);
    if (!this.screenTransformX.isDone()) this.screenTransformX.countEaseOut(dt, 8, screenWidth/2);
    if (!this.screenTransformY.isDone()) this.screenTransformY.countEaseOut(dt, 8, screenHeight/2);

    // Draw the background items.
    this.backgroundSnakeTimer.count(dt);
    if (this.backgroundSnakeTimer.isDone()) {
      BackgroundSnake newSnake = new BackgroundSnake(this);
      this.backgroundSnakes.add(newSnake);
      this.backgroundSnakeTimer.reset();
      this.backgroundSnakeTimer.setEndTime(Math.random());
    }
    for (BackgroundSnake backgroundSnake : backgroundSnakes) {
      backgroundSnake.update(dt);
      backgroundSnake.draw(gl);
    }

    glText.begin(1f, 1f, 1f, 1f);

    // Draw the current menu items.
    for (MenuDrawable drawable : getCurrentDrawables()) {
      if (drawable.isDrawable()
          && (drawable.isDrawableOutsideOfScreen() || screenTransformX.isDone()))
        drawable.draw();
      drawable.move(dt);
    }

    // Also draw the menu items for the previous screen.
    if (menuStatePrevious != menuState)
      for (MenuDrawable drawable : getPreviousDrawables()) {
        if (drawable.isDrawable() && drawable.isDrawableOutsideOfScreen())
          drawable.draw();
        drawable.move(dt);
      }

    // Draw the menu state element.
    this.menuStateItem.move(dt);
    this.menuStateItem.draw();

    glText.end();

    // This is required for the background snakes to render. Do not understand why.
    testSquare.draw(gl);
  }

  public MenuState getState() { return this.menuState; }
  public MenuState getPreviousState() { return this.menuStatePrevious; }
  public MenuItem[] getCurrentMenuItems() {
    switch(menuState) {
      case MAIN:
        return menuItemsMain;
      case CONNECT:
        return menuItemsConnect;
      case BOARD:
        return menuItemsBoard;
      case PLAYERS:
        return menuItemsPlayers;
      case PLAYERSOPTIONS:
        return menuItemsPlayersOptions;
      default:
        return null;
    }
  }
  public List<MenuDrawable> getCurrentDrawables() { return getDrawablesFromState(menuState); }
  public List<MenuDrawable> getPreviousDrawables() {
    return getDrawablesFromState(menuStatePrevious);
  }
  public List<MenuDrawable> getDrawablesFromState(MenuState state) {
    switch(state) {
      case MAIN: return drawablesMain;
      case CONNECT: return drawablesConnect;
      case BOARD: return drawablesBoard;
      case PLAYERS: return drawablesPlayers;
      case PLAYERSOPTIONS: return drawablesPlayersOptions;
      default: return null;
    }
  }

  // Handles setting the menu state to a given one.
  public void setState(MenuState state) {
    if (this.expandedItemIndex != -1) this.expandItem(this.expandedItemIndex);
    this.menuStatePrevious = this.menuState;
    this.menuState = state;
    // Reset all items' opacity in case we've used an animation to transition out of that screen.
    for (MenuItem item : this.getCurrentMenuItems()) item.setOpacity(1);
    this.menuStateItem.setColors(1f,1f,1f,1f);
    int screen;
    switch (state) {
      case MAIN:
        this.menuStateItem.setText("");
        screen = 0;
        break;
      case CONNECT:
        this.menuStateItem.setText("Connect");
        screen = 1;
        // Update the items' opacity.
        if (this.connectionType == ConnectionType.BLUETOOTH)
          this.menuItemsConnect[3].setOpacity(0.25f);
        else if (this.connectionType == ConnectionType.WIFI)
          this.menuItemsConnect[2].setOpacity(0.25f);
        if (this.connectionRole == ConnectionRole.HOST)
          this.menuItemsConnect[1].setOpacity(0.25f);
        else if (this.connectionRole == ConnectionRole.GUEST)
          this.menuItemsConnect[0].setOpacity(0.25f);
        break;
      case BOARD:
        this.menuStateItem.setText("Board");
        screen = 1;
        break;
      case PLAYERS:
        this.menuStateItem.setText("Players");
        screen = 1;
        // Update the descriptions.
        for (int index = 0; index < this.menuItemsPlayers.length; index++) {
          menuItemsPlayers[index].setDescription(this.players[index].getControlType().toString());
          menuItemsPlayers[index].setColors(this.players[index].getColors());
          menuItemsPlayers[index].setOpacity(
              (players[index].getControlType() == Player.ControlType.OFF) ? 0.5f : 1f);
          menuItemsPlayers[index].setDescriptionOpacity(
              (players[index].getControlType() == Player.ControlType.OFF) ? 0.5f : 1f);
        }
        break;
      case PLAYERSOPTIONS:
        this.menuStateItem.setText(this.players[this.playersOptionsIndex].getName());
        screen = 2;
        // Update the chosen player's options values.
        this.menuItemsPlayersOptions[0].getValue()
            .setValue(this.players[this.playersOptionsIndex].getControlType().toString());
        this.menuStateItem.setColors(this.players[this.playersOptionsIndex].getColors());
        this.fadeAllButOne(colorSelectionSquare,
            colorSelectionSquare[this.players[this.playersOptionsIndex].getColorIndex()]);
        switch (this.players[this.playersOptionsIndex].getControlCorner()) {
          case LOWER_LEFT: fadeAllButOne(cornerSelectionSquare, cornerSelectionSquare[0]); break;
          case UPPER_LEFT: fadeAllButOne(cornerSelectionSquare, cornerSelectionSquare[1]); break;
          case UPPER_RIGHT: fadeAllButOne(cornerSelectionSquare, cornerSelectionSquare[2]); break;
          case LOWER_RIGHT: fadeAllButOne(cornerSelectionSquare, cornerSelectionSquare[3]); break;
        }
        break;
      default:
        this.menuStateItem.setText("");
        screen = 0;
        break;
    }

    this.setScreen(screen);
    this.menuStateItem.setDestinationX(this.screenWidth * (screen + 1) - 10);
    this.menuStateItem.setDestinationY(this.screenHeight - 10 - glText.getCharHeight()*0.65);
  }

  // A modified version which also handles the menuStateItem's animation.
  public void setState(MenuDrawable origin, MenuState state) {
    this.menuStateItem.setX(origin.getX());
    this.menuStateItem.setY(origin.getY());
    origin.setOpacity(0);
    this.setState(state);
  }

  // Calls set state for the menu's current state so that it updates some visual values.
  public void updateState() {
    this.setState(menuState);
  }

  // Handles moving back one screen.
  public void goBack() {
    if (this.screenTransformX.isDone())
      switch (this.menuState) {
        case MAIN: break;
        case CONNECT: this.setState(MenuState.MAIN); break;
        case BOARD: this.setState(MenuState.MAIN); break;
        case PLAYERS: this.setState(MenuState.MAIN); break;
        case PLAYERSOPTIONS: this.setState(MenuState.PLAYERS); break;
        default: break;
      }
  }

  public void setScreen(int screen) {
    this.screenTransformX.setEndTimeFromNow(-this.screenWidth * screen);
    this.screenTransformY.setEndTimeFromNow(0);
    this.currentScreen = screen;
  }

  // Expands an item which has an integer value, pushing all following items down to make room for
  // the plus/minus buttons interface.
  public void expandItem(int expandIndex) {
    if (expandIndex < this.getCurrentMenuItems().length
        && this.getCurrentMenuItems()[expandIndex].getValue() != null) {
      MenuItem[] items = this.getCurrentMenuItems();
      // If it has an integer value - expand it.
      if (items[expandIndex].getValue().getType() == MenuValue.Type.INTEGER) {
        if (expandIndex == this.expandedItemIndex) {
          // If the item pressed has already been expanded, then retract it and all other items.
          for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
            items[itemIndex].setDestinationYFromOrigin(0);
            if (items[itemIndex].getValue() != null) {
              items[itemIndex].getValue().setExpanded(false);
              items[itemIndex].getValue().setDestinationYFromOrigin(0);
            }
          }
          this.expandedItemIndex = -1;
        } else {
          if (this.expandedItemIndex >= 0 && this.expandedItemIndex < items.length
              && items[this.expandedItemIndex].getValue() != null)
            items[this.expandedItemIndex].getValue().setExpanded(false);
          // Do not push items before it.
          for (int itemIndex = 0; itemIndex < expandIndex; itemIndex++) {
            items[itemIndex].setDestinationYFromOrigin(0);
            if (items[itemIndex].getValue() != null)
              items[itemIndex].getValue().setDestinationYFromOrigin(0);
          }
          // Expand the item itself by half its height.
          items[expandIndex].setDestinationYFromOrigin(-items[expandIndex].getHeight() / 2);
          // Push all following items down by the expanded item's height.
          for (int itemIndex = expandIndex + 1; itemIndex < items.length; itemIndex++) {
            items[itemIndex].setDestinationYFromOrigin(-items[expandIndex].getHeight());
            if (items[itemIndex].getValue() != null)
              items[itemIndex].getValue()
                              .setDestinationYFromOrigin(-items[expandIndex].getHeight());
          }

          if (items[expandIndex].getValue() != null)
            items[expandIndex].getValue().setExpanded(true);
          this.expandedItemIndex = expandIndex;
        }
      // If it has a boolean value - just invert the value.
      } else if (items[expandIndex].getValue().getType() == MenuValue.Type.BOOLEAN) {
        items[expandIndex].getValue().setValue(!items[expandIndex].getValue().getValueBoolean());
      // If it has a string value - open the keyboard layout to type.
      } else {
        // TODO
      }
    }
  }

  // This will be called after every change of a menuValue's value.
  public void syncValues() {
    if (this.menuState == MenuState.BOARD) {
      this.horizontalSquares = menuItemsBoard[0].getValue().getValueInteger();
      this.verticalSquares = menuItemsBoard[1].getValue().getValueInteger();
      this.speed = menuItemsBoard[2].getValue().getValueInteger();
      this.stageBorders = menuItemsBoard[3].getValue().getValueBoolean();
    }
  }

  // Enables the first currently disabled snake to play and handles the players menu animations.
  public void addSnake() {
    for (int index = 0; index < players.length; index++)
      if (players[index].getControlType() == Player.ControlType.OFF) {
        players[index].setControlType(Player.ControlType.CORNER);
        removeSnakeButtons[index].show();
        if (index != players.length - 1)
          addSnakeButton.setDestinationY(menuItemsPlayers[index+1].getY());
        else
          addSnakeButton.setOpacity(0);
        break;
      }
    this.updateState();
  }

  // Disables a given snake from play and handles the players menu animations.
  public void removeSnake(int snakeIndex) {
    int index;
    for (index = snakeIndex; index < players.length - 1; index++)
      if (players[index + 1].getControlType() != Player.ControlType.OFF)
        swapSnakes(snakeIndex, snakeIndex + 1);
      else
        break;
    players[index].setControlType(Player.ControlType.OFF);
    removeSnakeButtons[index].hide();
    addSnakeButton.setDestinationY(menuItemsPlayers[index].getY());
    addSnakeButton.setOpacity(1);
    this.updateState();
  }

  // Swaps the indices of two snakes in the players menu screen.
  private void swapSnakes(int firstSnakeIndex, int secondSnakeIndex) {
    Player playerHolder = players[secondSnakeIndex];
    players[secondSnakeIndex] = players[firstSnakeIndex];
    players[firstSnakeIndex] = playerHolder;
  }

  public void cyclePlayerControlTypes() {
    switch (this.players[this.playersOptionsIndex].getControlType()) {
      case OFF:
        break;
      case CORNER:
        boolean swipeTaken = false;
        for (Player player : players)
          if (player.getControlType() == Player.ControlType.SWIPE)
            swipeTaken = true;

        if (!swipeTaken)
          this.players[this.playersOptionsIndex].setControlType(Player.ControlType.SWIPE);
        else
          this.players[this.playersOptionsIndex].setControlType(Player.ControlType.GAMEPAD);
        break;
      case SWIPE:
        // TODO the next one would be keyboard and then gamepad, but they're not implemented
        this.players[this.playersOptionsIndex].setControlType(Player.ControlType.GAMEPAD);
        break;
      case KEYBOARD:
        this.players[this.playersOptionsIndex].setControlType(Player.ControlType.CORNER);
        break;
      case GAMEPAD:
        this.players[this.playersOptionsIndex].setControlType(Player.ControlType.CORNER);
        break;
      // The following ones you should not be able to easily switch off.
      case BLUETOOTH:
        break;
      case WIFI:
        break;
    }
    // Update the display value.
    this.menuItemsPlayersOptions[0].getValue()
        .setValue(this.players[this.playersOptionsIndex].getControlType().toString());
  }

  // Sets the currently selected player's color to a color from a given index.
  public void setPlayerColor(int index) {
    this.players[this.playersOptionsIndex].setColors(index);
    this.menuStateItem.setColors(getColorFromIndex(index));
  }

  // Sets the currently selected player's control corner to a given PlayerController.
  public void setPlayerControlCorner(PlayerController.Corner corner) {
    // Find the other player that uses the selected corner and set it to the current player's.
    for (int index = 0; index < this.players.length; index++)
      if (index != this.playersOptionsIndex && this.players[index].getControlCorner() == corner)
        this.players[index]
            .setCorner(players[playersOptionsIndex].getControlCorner());
    // Then set the current player's corner to the selected one.
    this.players[this.playersOptionsIndex].setCorner(corner);
  }

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
            menuItemsConnect[5].getY() - (pairedDevices.size() + 1) * glText.getCharHeight() * 0.65f * 5 / 4,
            MenuItem.Alignment.LEFT);
        deviceItem.setDescription(device.getAddress());
        deviceItem.setAction((action,origin)
            -> renderer.getMenu().connectToDeviceViaItem((MenuItem) origin));
        pairedDevices.add(device, deviceItem);
      }
    }
    bluetoothGuestMenu.addAll(pairedDevices.getItems());
    drawablesConnect.addAll(pairedDevices.getItems());
  }

  // Adds a newly found device to the list and displays it.
  public void addFoundDevice(BluetoothDevice device) {
    if (!foundDevices.has(device) && !pairedDevices.has(device)) {
      // Create a new item representing that device.
      MenuItem deviceItem = new MenuItem(renderer,
          (device.getName() != null) ? device.getName() : device.getAddress(),
          screenWidth + 10,
          menuItemsConnect[5].getY() - (pairedDevices.size() + foundDevices.size() + 1)
              * glText.getCharHeight() * 0.65f * 5 / 4, MenuItem.Alignment.LEFT);
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
    if ((targetDevice = foundDevices.getDevice(item)) != null) {
      originActivity.connectThread = new ConnectThread(originActivity, targetDevice);
      originActivity.connectThread.run();
    } else if ((targetDevice = pairedDevices.getDevice(item)) != null) {
      originActivity.connectThread = new ConnectThread(originActivity, targetDevice);
      originActivity.connectThread.run();
    }
  }

  // Begins the search for nearby devices.
  public void beginSearch() {
    // Set the search button to invisible.
    this.menuItemsConnect[4].setDrawable(false);
    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    if (adapter == null) {
      // There is no bluetooth adapter, so don't do anything.
      this.menuItemsConnect[4].setDrawable(true);
      this.menuItemsConnect[4].setText("Bluetooth error :/");
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

      if (!adapter.startDiscovery()) {
        menuItemsConnect[4].setDrawable(true);
        menuItemsConnect[4].setText("Bluetooth error :/");
      }
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

    menuItemsConnect[6].setText("Stop server");
    menuItemsConnect[6].setAction((action, origin) -> renderer.getMenu().stopHost());
    menuItemsConnect[6].setDestinationYFromOrigin(-glText.getCharHeight() * 0.65f);
  }

  // Cancels the hosting of a bluetooth game.
  public void stopHost() {
    if (originActivity.acceptThread != null) {
      originActivity.acceptThread.cancel();
      originActivity.acceptThread = null;

      menuItemsConnect[6].setText("Start server");
      menuItemsConnect[6].setAction((action, origin) -> renderer.getMenu().beginHost());
      menuItemsConnect[6].setDestinationToInitial();
    }
  }

  // Sets up the menu to work as if you're a guest.
  public void beginGuest() {
    // Disable some items.
    for (MenuDrawable drawable : guestDisabledDrawables)
      if (drawable.isEnabled())
        drawable.setEnabled(false);
    // Set the new action for the add snake button.
    addSnakeButton.setAction((action, origin)
        -> originActivity.connectedThread.write(new byte[] {Protocol.REQUEST_ADD_SNAKE}));
    // Make all players uncontrollable.
    for (Player player : players) {
      player.setControlType(Player.ControlType.OFF);
    }
  }

  // Sets up the menu to work as if you're not a guest anymore.
  public void endGuest() {
    // Re-enable the items.
    for (MenuDrawable drawable : guestDisabledDrawables)
      if (drawable.isEnabled())
        drawable.setEnabled(true);
    // Set the add snake button action back to the old one.
    addSnakeButton.setAction((action, origin) -> renderer.getMenu().addSnake());
    // Turn all players back to off (except the first one). TODO Make it revert to saved.
    for (Player player : players)
      player.setControlType(Player.ControlType.OFF);
    players[0].setControlType(Player.ControlType.CORNER);
  }

  public void handleInputBytes(byte[] inputBytes, ConnectedThread sourceThread) {
    switch(inputBytes[0]) {
      case Protocol.SNAKE1_COLOR_CHANGED: players[0].setColors(inputBytes[1]); break;
      case Protocol.SNAKE2_COLOR_CHANGED: players[1].setColors(inputBytes[1]); break;
      case Protocol.SNAKE3_COLOR_CHANGED: players[2].setColors(inputBytes[1]); break;
      case Protocol.SNAKE4_COLOR_CHANGED: players[3].setColors(inputBytes[1]); break;
      case Protocol.SNAKE1_CORNER_CHANGED: players[0]
          .setCorner(Protocol.decodeCorner(inputBytes[1])); break;
      case Protocol.SNAKE2_CORNER_CHANGED: players[1]
          .setCorner(Protocol.decodeCorner(inputBytes[1])); break;
      case Protocol.SNAKE3_CORNER_CHANGED: players[2]
          .setCorner(Protocol.decodeCorner(inputBytes[1])); break;
      case Protocol.SNAKE4_CORNER_CHANGED: players[3]
          .setCorner(Protocol.decodeCorner(inputBytes[1])); break;

      // HOST ONLY
      case Protocol.REQUEST_ADD_SNAKE:
        if (!this.isGuest()) {
          for (Player player : players) {
            if (player != null && player.getControlType() == Player.ControlType.OFF) {
              player.setControlType(Player.ControlType.BLUETOOTH);
              player.setControllerThread(sourceThread);
            }
          }
          sourceThread.write(this.getControlledSnakesList(sourceThread));
        }
        break;

      // GUEST ONLY
      case Protocol.CONTROLLED_SNAKES_LIST:
        if (this.isGuest()) {
          // Disable all.
          for (Player player : players)
            player.setControlType(Player.ControlType.OFF);
          // Enable only the ones specified in the list.
          for (int byteIndex = 1; byteIndex < inputBytes.length; byteIndex++)
            for (Player player : players)
              if (player.getNumber() == inputBytes[byteIndex])
                player.setControlType(Player.ControlType.CORNER);
        }
        break;

      default: break;
    }

    // Tell everyone what happened if the message was some change in settings.
    if (connectionRole == ConnectionRole.HOST && inputBytes[0] >= 20 && inputBytes[0] < 50)
      for(ConnectedThread connectedThread : originActivity.connectedThreads)
        if (connectedThread != null) connectedThread.write(inputBytes);
  }

  // Fades all buttons of a group except one.
  public void fadeAllButOne(MenuDrawable[] buttons, MenuDrawable exception) {
    for (MenuDrawable button : buttons) {
      if (button != exception) button.setOpacity(0.5f);
      else button.setOpacity(1);
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
    for (MenuDrawable drawable : getCurrentDrawables())
      if (drawable.isDrawable() && drawable.getY() < minHeight) minHeight = drawable.getY();
    return minHeight;
  }

  // Performs a screen scroll vertically by a given amount.
  public void scroll(float amount) {
    this.screenTransformY.setTime(Math.max(this.getScrollHeight(),
                                  Math.min(0, amount + screenTransformY.getTime())));
  }

  // Performs a screen scroll horizontally by a given amount, but only to the left.
  public void peekLeftScreen(float amount) {
    if (screenTransformX.isDone())
      this.screenTransformX.setTime(Math.min(Math.max(currentScreen * (-screenWidth),
                                             amount + screenTransformX.getTime())
                                    ,0));
  }

  // Checks the current screen horizontal transformation and snaps it to either the current screen
  // or the one on the left. You cannot snap forward (to the screen on the right).
  public void snapToClosestHorizontalScreen() {
    if (screenTransformX.getEndTime() % screenWidth > -screenWidth * 4 / 5
        && screenTransformX.getEndTime() % screenWidth != 0)
      this.goBack();
    else
      this.screenTransformX.setEndTimeFromNow(-screenWidth * currentScreen);
  }

  // More getters.
  public void setPlayerOptionsIndex(int index) { this.playersOptionsIndex = index; }
  public float getScreenTransformX() { return (float) this.screenTransformX.getTime(); }
  public float getScreenTransformY() { return (float) this.screenTransformY.getTime(); }
  public MenuImage[] getColorSelectionSquares() { return this.colorSelectionSquare; }
  public MenuImage[] getCornerSelectionSquares() { return this.cornerSelectionSquare; }
  public GameRenderer getRenderer() { return this.renderer; }
  public OpenGLES20Activity getOriginActivity() { return this.originActivity; }

  public Player[] getPlayers() { return this.players; }

  // Protocol simplifier getters.
  public boolean isGuest() { return originActivity.isGuest(); }
  public byte[] getControlledSnakesList(ConnectedThread thread) {
    int controlledSnakes = 0;
    for (Player player : players)
      if (player.getControllerThread().equals(thread))
        controlledSnakes++;

    byte[] output = new byte[controlledSnakes + 1];
    output[0] = Protocol.CONTROLLED_SNAKES_LIST;
    int outputIndex = 1;
    for (Player player : players)
      if (player.getControllerThread().equals(thread))
        output[outputIndex++] = (byte) player.getNumber();

    return output;
  }

  public byte[] getAvailableSnakesList() {
    int availableSlots = 4;
    for (Player player : players)
      if (player.getControlType() == Player.ControlType.OFF)
        availableSlots--;

    byte[] output = new byte[availableSlots + 1];
    output[0] = Protocol.AVAILABLE_SNAKES_LIST;
    int outputIndex = 1;
    for (Player player : players)
      if (player.getControlType() == Player.ControlType.OFF)
        output[outputIndex++] = (byte) player.getNumber();

    return output;
  }
}



// Backgrounds snakes which will appear randomly on the screen going from left to right.
class BackgroundSnake extends Mesh {
  private final float size, speed, initialY;
  private final int length;
  private float x;
  private Menu menu;
  private double movementTimer;

  // Constructor.
  public BackgroundSnake(Menu menu) {
    super();
    this.menu = menu;
    float screenHeight = this.menu.getRenderer().getScreenHeight();

    // Set their variables randomly. You can change up the formulas.
    this.size = screenHeight * 0.05f + (float) Math.random() * screenHeight * 0.1f;
    this.length = 4 + (int) Math.round(Math.random()*11);
    this.speed = 1f / (6 + (int) Math.round(Math.random()*12));
    this.initialY = size + (float) Math.random() * screenHeight * 2f;
    this.movementTimer = speed;
    this.x = -((size + 1) * length);

    for (int index = 0; index < length; index++)
      this.addSquare(index * size + index, 0, size, size);
    this.applySquares();
    this.updateColors(length - 1, 1f, 1f, 1f, 0.5f);
  }

  // Moves the snake whenever it is time to be moved.
  public void update(double dt) {
    this.movementTimer -= dt;
    while (this.movementTimer < 0) {
      this.movementTimer += speed;
      this.x += this.size + 1;
    }
  }

  public void draw(GL10 gl) {
    gl.glPushMatrix();
    gl.glTranslatef(x,initialY,0);
    super.draw(gl);
    gl.glPopMatrix();
  }
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
      if (device == pair.first)
        return true;

    return false;
  }

  public int size() { return deviceMap.size(); }

  public ArrayList<MenuItem> getItems() {
    ArrayList<MenuItem> itemsList = new ArrayList<>();
    for (Pair<BluetoothDevice, MenuItem> pair : deviceMap)
      itemsList.add(pair.second);
    return itemsList;
  }

  public MenuItem getItem(BluetoothDevice device) {
    for (Pair<BluetoothDevice, MenuItem> pair : deviceMap)
      if (device == pair.first)
        return pair.second;
    return null;
  }

  public BluetoothDevice getDevice(MenuItem item) {
    for (Pair<BluetoothDevice, MenuItem> pair : deviceMap)
      if (item == pair.second)
        return pair.first;
    return null;
  }
}