package thusnake.snakemultiplayer;

public abstract class MenuScreenAnimation {
  final GameRenderer renderer;
  final Menu menu;
  final MenuScreen currentScreen, nextScreen;

  public MenuScreenAnimation(Menu menu, MenuScreen currentScreen, MenuScreen nextScreen) {
    this.renderer = menu.getRenderer();
    this.menu = menu;
    this.currentScreen = currentScreen;
    this.nextScreen = nextScreen;
  }

  public abstract void run(double dt);
}
