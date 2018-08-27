package thusnake.snakemultiplayer;

import java.util.List;

public abstract class SettingsScreen extends MenuScreen {
  public SettingsScreen(Menu menu, String title) {
    super(menu);

    MultilineMenuItem titleItem
        = new MultilineMenuItem(renderer, title, renderer.getScreenWidth() / 2f,
                                renderer.getScreenHeight() - 10,
                                MenuDrawable.EdgePoint.TOP_CENTER,
                                renderer.getScreenWidth() - backButton.getWidth() * 2);

    MenuListOfItems listOfOptions = new MenuListOfItems(renderer, 10,
        titleItem.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) - renderer.getScreenHeight() / 9f,
        MenuDrawable.EdgePoint.TOP_LEFT);
    for (MenuDrawable drawable : createListOfOptions())
      listOfOptions.addItem(drawable);

    drawables.add(titleItem);
    drawables.add(listOfOptions);
  }

  public abstract List<MenuDrawable> createListOfOptions();
}
