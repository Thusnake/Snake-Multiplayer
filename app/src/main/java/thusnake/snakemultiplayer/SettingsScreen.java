package thusnake.snakemultiplayer;

import java.util.List;

public abstract class SettingsScreen extends MenuScreen {
  public SettingsScreen(Menu menu, String title) {
    super(menu);

    MenuItem titleItem = new MenuItem(renderer, title, renderer.getScreenWidth() / 2f, 10,
                                      MenuDrawable.EdgePoint.TOP_CENTER);

    MenuListOfItems listOfOptions = new MenuListOfItems(renderer, 10,
        titleItem.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER), MenuDrawable.EdgePoint.TOP_LEFT);
    for (MenuDrawable drawable : createListOfOptions())
      listOfOptions.addItem(drawable);

    drawables.add(titleItem);
    drawables.add(listOfOptions);
  }

  public abstract List<MenuDrawable> createListOfOptions();
}
