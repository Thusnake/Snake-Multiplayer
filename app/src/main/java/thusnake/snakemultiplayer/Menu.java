package thusnake.snakemultiplayer;

import com.android.texample.GLText;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 14/12/2017.
 */

// And instance of this class would be a single menu with all menu items.
public class Menu {
  private SimpleTimer screenTransformX, screenTransformY;
  private float screenWidth, screenHeight;
  private GameRenderer renderer;
  private final GL10 gl;
  private final GLText glText;
  private final MenuItem[] menuItemsMain, menuItemsConnect, menuItemsBoard, menuItemsPlayers,
      menuItemsPlayersOptions;
  private final MenuItem menuStateItem;
  public enum MenuState {MAIN, CONNECT, BOARD, PLAYERS, PLAYERSOPTIONS};
  private String[] menuItemStateNames = {"", "Connect", "Board", "Players", ""};
  private MenuState menuState, menuStatePrevious;
  private int playersOptionsIndex, expandedItemIndex = -1;
  private SimpleTimer menuAnimationTimer = new SimpleTimer(0.0, 1.0);
  // Menu variables
  public int horizontalSquares, verticalSquares, speed;
  public boolean stageBorders;

  // Constructor.
  public Menu(GameRenderer renderer, float screenWidth, float screenHeight) {
    this.renderer = renderer;
    this.gl = renderer.getGl();
    this.glText = renderer.getGlText();

    this.screenTransformX = new SimpleTimer(0.0);
    this.screenTransformY = new SimpleTimer(0.0);
    this.menuState = MenuState.MAIN;
    this.menuStateItem = new MenuItem(renderer, menuItemStateNames[0], 0, 0,
        MenuItem.Alignment.RIGHT);
    this.menuStateItem.setEaseOutVariables(16, this.menuStateItem.getHeight() * 2);

    this.screenWidth = screenWidth;
    this.screenHeight = screenHeight;

    // TODO get these values from the options strings
    this.horizontalSquares = 20;
    this.verticalSquares = 20;
    this.speed = 12;
    this.stageBorders = true;

    // Create menuItem instances for each button.
    String[] menuItemsMainText = {"Play", "Connect", "Board", "Players", "Watch ad"};
    this.menuItemsMain = new MenuItem[menuItemsMainText.length];
    for (int i = 0; i < menuItemsMainText.length; i++)
      this.menuItemsMain[i] = new MenuItem(renderer, menuItemsMainText[i], 10,
          screenHeight - glText.getCharHeight() * (i + 1) * 0.65f
              - (screenHeight - glText.getCharHeight() * menuItemsMainText.length * 0.65f) / 2,
          MenuItem.Alignment.LEFT);

    this.menuItemsConnect = new MenuItem[4];
    this.menuItemsConnect[0] = new MenuItem(renderer, "Host", 10 + screenWidth,
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f, MenuItem.Alignment.LEFT);
    this.menuItemsConnect[1] = new MenuItem(renderer, "Join", 10 + screenWidth,
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f * 2, MenuItem.Alignment.LEFT);
    this.menuItemsConnect[2] = new MenuItem(renderer, "Bluetooth", screenWidth * 2 - 10,
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f, MenuItem.Alignment.RIGHT);
    this.menuItemsConnect[3] = new MenuItem(renderer, "Wi-Fi", screenWidth * 2 - 10,
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f * 2, MenuItem.Alignment.RIGHT);

    String[] menuItemsBoardText = {"Hor Squares", "Ver Squares", "Speed", "Stage Borders"};
    this.menuItemsBoard = new MenuItem[menuItemsBoardText.length];
    for (int i = 0; i < menuItemsBoardText.length; i++)
      this.menuItemsBoard[i] = new MenuItem(renderer, menuItemsBoardText[i], 10 + screenWidth,
          screenHeight - glText.getCharHeight() * (i * 5/4 + 1) * 0.65f - screenHeight / 5,
          MenuItem.Alignment.LEFT);

    this.menuItemsPlayers = new MenuItem[4];
    for (int i = 0; i < 4; i++)
      this.menuItemsPlayers[i] = new MenuItem(renderer, "Player " + (i + 1),
          10 + screenWidth, screenHeight * 4/5 - glText.getCharHeight() * (i * 5/4 + 1) * 0.65f,
          MenuItem.Alignment.LEFT);

    String[] menuItemsPlayersOptionsText = {"Type", ""};
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
    }

    this.menuStateItem.move(dt);
    this.menuStateItem.draw();

    glText.end();
  }

  public MenuState getState() { return this.menuState; }
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

  public void setState(MenuState state) {
    if (this.expandedItemIndex != -1) this.expandItem(this.expandedItemIndex);
    this.menuStatePrevious = this.menuState;
    this.menuState = state;
    // Reset all items' opacity in case we've used an animation to transition out of that screen.
    for (MenuItem item : this.getCurrentMenuItems()) item.setOpacity(1);
    int screen;
    switch (state) {
      case MAIN:
        this.menuStateItem.setText("");
        screen = 0;
        break;
      case CONNECT:
        this.menuStateItem.setText("Connect");
        screen = 1;
        break;
      case BOARD:
        this.menuStateItem.setText("Board");
        screen = 1;
        break;
      case PLAYERS:
        this.menuStateItem.setText("Players");
        screen = 1;
        break;
      case PLAYERSOPTIONS:
        // TODO This should display the currently chosen player's name
        this.menuStateItem.setText("");
        screen = 2;
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

  public void setState(MenuItem origin, MenuState state) {
    this.menuStateItem.setX(origin.getX());
    this.menuStateItem.setY(origin.getY());
    origin.setOpacity(0);
    this.setState(state);
  }

  public void goBack() {
    switch (this.menuState) {
      case MAIN: break;
      case CONNECT: this.setState(MenuState.MAIN); break;
      case BOARD: this.setState(MenuState.MAIN); break;
      case PLAYERS: this.setState(MenuState.MAIN); break;
      case PLAYERSOPTIONS: this.setState(MenuState.PLAYERS); break;
      default: break;
    }
  }

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
  public void syncValues() {
    if (this.menuState == MenuState.BOARD) {
      this.horizontalSquares = menuItemsBoard[0].getValue().getValueInteger();
      this.verticalSquares = menuItemsBoard[1].getValue().getValueInteger();
      this.speed = menuItemsBoard[2].getValue().getValueInteger();
      this.stageBorders = menuItemsBoard[3].getValue().getValueBoolean();
    }
  }
  public void setPlayerOptionsIndex(int index) { this.playersOptionsIndex = index; }
  public float getScreenTransformX() { return (float) this.screenTransformX.getTime(); }
  public float getScreenTransformY() { return (float) this.screenTransformY.getTime(); }
}
