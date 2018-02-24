package thusnake.snakemultiplayer;

import com.android.texample.GLText;

import java.util.ArrayList;
import java.util.LinkedList;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 14/12/2017.
 */

// And instance of this class would be a single menu with all menu items.
public class Menu {
  private SimpleTimer screenTransformX, screenTransformY;
  private float screenWidth, screenHeight;
  private GameRenderer renderer;
  private Player[] players = new Player[4];
  private final GL10 gl;
  private final GLText glText;
  private final MenuItem[] menuItemsMain, menuItemsConnect, menuItemsBoard, menuItemsPlayers,
      menuItemsPlayersOptions;
  private final ArrayList<MenuItem> pairedDevicesItems = new ArrayList<>();
  private final ArrayList<MenuItem> foundDevicesItems = new ArrayList<>();
  private final MenuImage[] colorSelectionSquare, cornerSelectionSquare;
  private final MenuItem menuStateItem;
  public enum MenuState {MAIN, CONNECT, BOARD, PLAYERS, PLAYERSOPTIONS};
  private String[] menuItemStateNames = {"", "Connect", "Board", "Players", ""};
  private MenuState menuState, menuStatePrevious;
  private int playersOptionsIndex, expandedItemIndex = -1;
  private SimpleTimer menuAnimationTimer = new SimpleTimer(0.0, 1.0);
  private SimpleTimer backgroundSnakeTimer = new SimpleTimer(0.0, 0.5 + Math.random());
  private LinkedList<BackgroundSnake> backgroundSnakes = new LinkedList<>();
  private enum ConnectionType {BLUETOOTH, WIFI}
  private enum ConnectionRole {HOST, GUEST}
  private ConnectionType connectionType = null;
  private ConnectionRole connectionRole = null;
  private final OpenGLES20Activity originActivity;

  // Menu variables
  public int horizontalSquares, verticalSquares, speed;
  public boolean stageBorders;

  // TODO Find a way to not use this, currently used for the background snakes, they refuse to render without it.
  Square testSquare = new Square(500,0,0,0);

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

    // TODO get these values from the options strings
    this.horizontalSquares = 20;
    this.verticalSquares = 20;
    this.speed = 12;
    this.stageBorders = true;

    // Initialize the players.
    for (int index = 0; index < players.length; index++) players[index] = new Player();
    this.players[0].setControlType(Player.ControlType.CORNER);
    this.players[1].setControlType(Player.ControlType.OFF);
    this.players[2].setControlType(Player.ControlType.OFF);
    this.players[3].setControlType(Player.ControlType.OFF);
    this.players[0].setName("Player 1");
    this.players[1].setName("Player 2");
    this.players[2].setName("Player 3");
    this.players[3].setName("Player 4");
    this.players[0].setCornerLayout(CornerLayout.Corner.LOWER_LEFT);
    this.players[1].setCornerLayout(CornerLayout.Corner.LOWER_RIGHT);
    this.players[2].setCornerLayout(CornerLayout.Corner.UPPER_LEFT);
    this.players[3].setCornerLayout(CornerLayout.Corner.UPPER_RIGHT);
    this.players[0].setColors(0);
    this.players[1].setColors(0);
    this.players[2].setColors(0);
    this.players[3].setColors(0);

    // Create menuItem instances for each button.
    String[] menuItemsMainText = {"Play", "Connect", "Board", "Players", "Watch ad"};
    this.menuItemsMain = new MenuItem[menuItemsMainText.length];
    for (int i = 0; i < menuItemsMainText.length; i++)
      this.menuItemsMain[i] = new MenuItem(renderer, menuItemsMainText[i], 10,
          screenHeight - glText.getCharHeight() * (i + 1) * 0.65f
              - (screenHeight - glText.getCharHeight() * menuItemsMainText.length * 0.65f) / 2,
          MenuItem.Alignment.LEFT);

    this.menuItemsConnect = new MenuItem[5];
    this.menuItemsConnect[0] = new MenuItem(renderer, "Host", 10 + screenWidth,
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f, MenuItem.Alignment.LEFT);
    this.menuItemsConnect[1] = new MenuItem(renderer, "Join", 10 + screenWidth,
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f * 2, MenuItem.Alignment.LEFT);
    this.menuItemsConnect[2] = new MenuItem(renderer, "Bluetooth", screenWidth * 2 - 10,
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f, MenuItem.Alignment.RIGHT);
    this.menuItemsConnect[3] = new MenuItem(renderer, "Wi-Fi", screenWidth * 2 - 10,
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f * 2, MenuItem.Alignment.RIGHT);
    this.menuItemsConnect[4] = new MenuItem(renderer, "Test123", screenWidth*2.5f,
        screenHeight / 8, MenuItem.Alignment.CENTER);

    String[] menuItemsBoardText = {"Hor Squares", "Ver Squares", "Speed", "Stage Borders"};
    this.menuItemsBoard = new MenuItem[menuItemsBoardText.length];
    for (int i = 0; i < menuItemsBoardText.length; i++)
      this.menuItemsBoard[i] = new MenuItem(renderer, menuItemsBoardText[i], 10 + screenWidth,
          screenHeight - glText.getCharHeight() * (i * 5/4 + 1) * 0.65f - screenHeight / 5,
          MenuItem.Alignment.LEFT);

    this.menuItemsPlayers = new MenuItem[4];
    for (int i = 0; i < 4; i++)
      this.menuItemsPlayers[i] = new MenuItem(renderer, "Player " + (i + 1),
          10 + screenWidth, screenHeight * 4/5f - glText.getCharHeight() * (i * 5/4f + 1) * 0.65f,
          MenuItem.Alignment.LEFT);

    String[] menuItemsPlayersOptionsText = {"Type"};
    this.menuItemsPlayersOptions = new MenuItem[menuItemsPlayersOptionsText.length];
    for (int i = 0; i < menuItemsPlayersOptionsText.length; i++)
      this.menuItemsPlayersOptions[i] = new MenuItem(renderer, menuItemsPlayersOptionsText[i],
          10 + screenWidth * 2,
          screenHeight * 4/5 - (screenWidth-110)/9 - glText.getCharHeight() * (i + 1) * 0.65f,
          MenuItem.Alignment.LEFT);

    // Set functionality for each menuItem.
    this.menuItemsMain[0].setAction((action, origin) -> renderer.startGame());
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
      this.colorSelectionSquare[index].setColors(this.getColorFromIndex(index));
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
        -> renderer.getMenu().setPlayerControlCorner(CornerLayout.Corner.LOWER_LEFT));
    this.cornerSelectionSquare[1].setAction((action, origin)
        -> renderer.getMenu().setPlayerControlCorner(CornerLayout.Corner.UPPER_LEFT));
    this.cornerSelectionSquare[2].setAction((action, origin)
        -> renderer.getMenu().setPlayerControlCorner(CornerLayout.Corner.UPPER_RIGHT));
    this.cornerSelectionSquare[3].setAction((action, origin)
        -> renderer.getMenu().setPlayerControlCorner(CornerLayout.Corner.LOWER_RIGHT));

    this.gl.glEnable(GL10.GL_TEXTURE_2D);
    this.gl.glEnable(GL10.GL_BLEND);
    this.gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
  }

  public void run(double dt) {
    this.gl.glTranslatef((float) screenTransformX.getTime(),
                         (float) screenTransformY.getTime(), 0f);
    this.gl.glColor4f(1f, 1f, 1f, 1f);

    //this.gl.glEnable(GL10.GL_BLEND);
    //this.gl.glBlendFunc(GL10.GL_SRC_ALPHA,GL10.GL_ONE_MINUS_SRC_ALPHA);

    // Handle timers.
    if (!this.menuAnimationTimer.isDone()) this.menuAnimationTimer.countEaseOut(dt, 3, 0);
    if (!this.screenTransformX.isDone()) this.screenTransformX.countEaseOut(dt, 8, screenWidth/2);

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

    // Draw the menu items.
    glText.begin(1f, 1f, 1f, 1f);
    for (MenuItem menuItem : menuItemsMain) menuItem.draw();

    if (menuState == MenuState.CONNECT || menuStatePrevious == MenuState.CONNECT) {
      for (MenuItem menuItem : menuItemsConnect) menuItem.draw();
    }

    if (menuState == MenuState.BOARD || menuStatePrevious == MenuState.BOARD) {
      for (MenuItem menuItem : menuItemsBoard) {
        menuItem.move(dt);
        menuItem.draw();
      }
      // TODO To have all board menu items have descriptions, use the strings xml file and find a way to implement it.
    }

    if (menuState == MenuState.PLAYERS || menuStatePrevious == MenuState.PLAYERS) {
      for (MenuItem menuItem : menuItemsPlayers) menuItem.draw();
    }

    if (menuState == MenuState.PLAYERSOPTIONS || menuStatePrevious == MenuState.PLAYERSOPTIONS) {
      for (MenuItem menuItem : menuItemsPlayersOptions) menuItem.draw();
      for (MenuImage square : colorSelectionSquare) {
        square.draw();
        square.move(dt);
      }
      if (this.players[this.playersOptionsIndex].getControlType() != Player.ControlType.OFF)
        for (MenuImage square : cornerSelectionSquare) {
          square.draw();
          square.move(dt);
        }
    }

    this.menuStateItem.move(dt);
    this.menuStateItem.draw();

    glText.end();

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

    this.screenTransformX.setEndTimeFromNow(-this.screenWidth * screen);
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

  public void cyclePlayerControlTypes() {
    // TODO Make it so that you can only turn on/off the last player in the stack.
    // TODO Have some buttons to make this easier to do.
    switch (this.players[this.playersOptionsIndex].getControlType()) {
      case OFF:
        this.players[this.playersOptionsIndex].setControlType(Player.ControlType.CORNER);
        break;
      case CORNER:
        this.players[this.playersOptionsIndex].setControlType(Player.ControlType.SWIPE);
        break;
      case SWIPE:
        // TODO the next one would be keyboard and then gamepad, but they're not implemented
        this.players[this.playersOptionsIndex].setControlType(Player.ControlType.OFF);
        break;
      case KEYBOARD:
        this.players[this.playersOptionsIndex].setControlType(Player.ControlType.OFF);
        break;
      case GAMEPAD:
        this.players[this.playersOptionsIndex].setControlType(Player.ControlType.OFF);
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

  // Sets the currently selected player's control corner to a given CornerLayout.
  public void setPlayerControlCorner(CornerLayout.Corner corner) {
    // Find the other player that uses the selected corner and set it to the current player's.
    for (int index = 0; index < this.players.length; index++)
      if (index != this.playersOptionsIndex && this.players[index].getControlCorner() == corner)
        this.players[index]
            .setCornerLayout(this.players[this.playersOptionsIndex].getControlCorner());
    // Then set the current player's corner to the selected one.
    this.players[this.playersOptionsIndex].setCornerLayout(corner);
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
  }

  public void handleInputBytes(byte[] inputBytes, ConnectedThread sourceThread) {
    switch(inputBytes[0]) {
      case Protocol.SNAKE1_COLOR_CHANGED: players[0].setColors(inputBytes[1]); break;
      case Protocol.SNAKE2_COLOR_CHANGED: players[1].setColors(inputBytes[1]); break;
      case Protocol.SNAKE3_COLOR_CHANGED: players[2].setColors(inputBytes[1]); break;
      case Protocol.SNAKE4_COLOR_CHANGED: players[3].setColors(inputBytes[1]); break;
      case Protocol.SNAKE1_CORNER_CHANGED: players[0].setCornerLayout(Protocol
          .decodeCorner(inputBytes[1])); break;
      case Protocol.SNAKE2_CORNER_CHANGED: players[1].setCornerLayout(Protocol
          .decodeCorner(inputBytes[1])); break;
      case Protocol.SNAKE3_CORNER_CHANGED: players[2].setCornerLayout(Protocol
          .decodeCorner(inputBytes[1])); break;
      case Protocol.SNAKE4_CORNER_CHANGED: players[3].setCornerLayout(Protocol
          .decodeCorner(inputBytes[1])); break;
      case Protocol.REQUEST_CONNECT:
        if (connectionRole == ConnectionRole.HOST) {
          sourceThread.write(new byte[] {Protocol.APPROVE_CONNECT});
          // TODO Also send the unique guest code to the guest.
        }
        break;
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

  // More getters.
  public void setPlayerOptionsIndex(int index) { this.playersOptionsIndex = index; }
  public float getScreenTransformX() { return (float) this.screenTransformX.getTime(); }
  public float getScreenTransformY() { return (float) this.screenTransformY.getTime(); }
  public MenuImage[] getColorSelectionSquares() { return this.colorSelectionSquare; }
  public MenuImage[] getCornerSelectionSquares() { return this.cornerSelectionSquare; }
  public GameRenderer getRenderer() { return this.renderer; }
}

// Backgrounds snakes which will appear randomly on the screen going from left to right.
class BackgroundSnake extends Mesh {
  private final float size, speed, initialY;
  private final int length;
  private float x;
  private Menu menu;
  private GL10 gl;
  private double movementTimer;

  // Constructor.
  public BackgroundSnake(Menu menu) {
    super();
    this.menu = menu;
    this.gl = this.menu.getRenderer().getGl();
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