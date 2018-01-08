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
  private final GL10 gl;
  private final GLText glText;
  private final MenuItem[] menuItemsMain, menuItemsConnect, menuItemsBoard, menuItemsPlayers,
      menuItemsPlayersOptions, menuItemsBack;
  private final MenuItem menuStateItem;
  public enum MenuState {MAIN, CONNECT, BOARD, PLAYERS, PLAYERSOPTIONS};
  private String[] menuItemStateNames = {"", "Connect", "Board", "Players", ""};
  private MenuState menuState, menuStatePrevious;
  private int playersOptionsIndex, expandedItemIndex = -1;
  private SimpleTimer menuAnimationTimer = new SimpleTimer(0.0, 1.0);

  // Constructor.
  public Menu(GameRenderer renderer, float screenWidth, float screenHeight) {
    this.gl = renderer.getGl();
    this.glText = renderer.getGlText();

    this.screenTransformX = new SimpleTimer(0.0);
    this.screenTransformY = new SimpleTimer(0.0);
    this.menuState = MenuState.MAIN;
    this.menuStateItem = new MenuItem(renderer, menuItemStateNames[0], 0, 0);

    this.screenWidth = screenWidth;
    this.screenHeight = screenHeight;

    // Create menuItem instances for each button.
    String[] menuItemsMainText = {"Play", "Connect", "Board", "Players", "Watch ad"};
    this.menuItemsMain = new MenuItem[menuItemsMainText.length];
    for (int i = 0; i < menuItemsMainText.length; i++)
      this.menuItemsMain[i] = new MenuItem(renderer, menuItemsMainText[i], 10,
          screenHeight - glText.getCharHeight() * (i + 1) * 0.65f
              - (screenHeight - glText.getCharHeight() * menuItemsMainText.length * 0.65f) / 2);

    this.menuItemsConnect = new MenuItem[4];
    this.menuItemsConnect[0] = new MenuItem(renderer, "Host", 10 + screenWidth,
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f);
    this.menuItemsConnect[1] = new MenuItem(renderer, "Join", 10 + screenWidth,
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f * 2);
    this.menuItemsConnect[2] = new MenuItem(renderer, "Bluetooth",
        screenWidth * 2 - 10 - glText.getLength("Bluetooth"),
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f);
    this.menuItemsConnect[3] = new MenuItem(renderer, "Wi-Fi",
        screenWidth * 2 - 10 - glText.getLength("Wi-Fi"),
        screenHeight * 4/5 - glText.getCharHeight() * 0.65f * 2);

    String[] menuItemsBoardText = {"Hor Squares", "Ver Squares", "Speed", "Stage Borders"};
    this.menuItemsBoard = new MenuItem[menuItemsBoardText.length];
    for (int i = 0; i < menuItemsBoardText.length; i++)
      this.menuItemsBoard[i] = new MenuItem(renderer, menuItemsBoardText[i], 10 + screenWidth,
          screenHeight - glText.getCharHeight() * (i * 5/4 + 1) * 0.65f - screenHeight / 5);

    this.menuItemsPlayers = new MenuItem[4];
    for (int i = 0; i < 4; i++)
      this.menuItemsPlayers[i] = new MenuItem(renderer, "Player " + (i + 1),
          10 + screenWidth, screenHeight * 4/5 - glText.getCharHeight() * (i * 5/4 + 1) * 0.65f);

    String[] menuItemsPlayersOptionsText = {"Type", ""};
    this.menuItemsPlayersOptions = new MenuItem[menuItemsPlayersOptionsText.length];
    for (int i = 0; i < menuItemsPlayersOptionsText.length; i++)
      this.menuItemsPlayersOptions[i] = new MenuItem(renderer, menuItemsPlayersOptionsText[i],
          10 + screenWidth * 2,
          screenHeight * 4/5 - (screenWidth-110)/9 - glText.getCharHeight() * (i + 1) * 0.65f);

    this.menuItemsBack = new MenuItem[2];
    for (int i = 0; i < menuItemsBack.length; i++)
      this.menuItemsBack[i] = new MenuItem(renderer, "< back", 10 + screenWidth * (i + 1),
          screenHeight - 10 - glText.getCharHeight() * 0.65f);

    // Set functionality for each menuItem.
    this.menuItemsMain[0].setAction(action -> renderer.startGame());
    this.menuItemsMain[1].setAction(action -> renderer.setMenuState(MenuState.CONNECT));
    this.menuItemsMain[2].setAction(action -> renderer.setMenuState(MenuState.BOARD));
    this.menuItemsMain[3].setAction(action -> renderer.setMenuState(MenuState.PLAYERS));

    this.menuItemsBoard[0].setAction(action -> renderer.getMenu().expandItem(0));
    this.menuItemsBoard[1].setAction(action -> renderer.getMenu().expandItem(1));
    this.menuItemsBoard[2].setAction(action -> renderer.getMenu().expandItem(2));
    this.menuItemsBoard[3].setAction(action -> renderer.getMenu().expandItem(3));

    this.menuItemsPlayers[0].setAction(action -> renderer.setMenuStateToPlayerOptions(0));
    this.menuItemsPlayers[1].setAction(action -> renderer.setMenuStateToPlayerOptions(1));
    this.menuItemsPlayers[2].setAction(action -> renderer.setMenuStateToPlayerOptions(2));
    this.menuItemsPlayers[3].setAction(action -> renderer.setMenuStateToPlayerOptions(3));

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
        menuItem.draw();
        menuItem.move(dt);
      }
      // TODO To have all board menu items have descriptions, use the strings xml file and find a way to implement it.
    }

    if (menuState == MenuState.PLAYERS || menuStatePrevious == MenuState.PLAYERS) {
      for (MenuItem menuItem : menuItemsPlayers) menuItem.draw();
    }

    if (menuState == MenuState.PLAYERSOPTIONS || menuStatePrevious == MenuState.PLAYERSOPTIONS) {
      for (MenuItem menuItem : menuItemsPlayersOptions) menuItem.draw();
    }

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
    switch (state) {
      case MAIN:
        this.screenTransformX.setEndTimeFromNow(0); break;
      case CONNECT:
        this.screenTransformX.setEndTimeFromNow(-this.screenWidth); break;
      case BOARD:
        this.screenTransformX.setEndTimeFromNow(-this.screenWidth); break;
      case PLAYERS:
        this.screenTransformX.setEndTimeFromNow(-this.screenWidth); break;
      case PLAYERSOPTIONS:
        this.screenTransformX.setEndTimeFromNow(-this.screenWidth * 2); break;
      default:
        this.screenTransformX.setEndTimeFromNow(0); break;
    }
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
    if (expandIndex < this.getCurrentMenuItems().length && this.menuState == MenuState.BOARD) {
      if (expandIndex == this.expandedItemIndex) {
        // If the item pressed has already been expanded, then retract it and all other items.
        for (int itemIndex = 0; itemIndex < this.getCurrentMenuItems().length; itemIndex++) {
          this.getCurrentMenuItems()[itemIndex].setDestinationYFromOrigin(0);
        }
        this.expandedItemIndex = -1;
      } else {
        // Do not push items before it.
        for (int itemIndex = 0; itemIndex < expandIndex; itemIndex++) {
          this.getCurrentMenuItems()[itemIndex].setDestinationYFromOrigin(0);
        }
        // Expand the item itself by half its height.
        MenuItem item = this.getCurrentMenuItems()[expandIndex];
        item.setDestinationYFromOrigin(-item.getHeight() / 2);
        // Push all following items down by the expanded item's height.
        for (int itemIndex = expandIndex + 1; itemIndex < this.getCurrentMenuItems().length;
             itemIndex++) {
          this.getCurrentMenuItems()[itemIndex].setDestinationYFromOrigin(-item.getHeight());
        }

        this.expandedItemIndex = expandIndex;
      }
    }
  }
  public void setPlayerOptionsIndex(int index) { this.playersOptionsIndex = index; }
  public float getScreenTransformX() { return (float) this.screenTransformX.getTime(); }
  public float getScreenTransformY() { return (float) this.screenTransformY.getTime(); }
}
