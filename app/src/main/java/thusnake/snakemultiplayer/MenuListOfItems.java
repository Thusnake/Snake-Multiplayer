package thusnake.snakemultiplayer;

import java.util.LinkedList;
import java.util.List;

public class MenuListOfItems extends MenuDrawable implements TextureReloadable {
  private List<MenuDrawable> contents = new LinkedList<>();
  private MenuItem expandedItem;

  public MenuListOfItems(GameRenderer renderer, float x, float y) {
    super(renderer, x, y);
  }

  public void draw() {
    for (MenuDrawable drawable : contents)
      drawable.draw();
  }

  public void move(double dt) {
    for (MenuDrawable drawable : contents)
      drawable.move(dt);
  }

  @Override
  public void reloadTexture() {
    for (MenuDrawable drawable : contents)
      if (drawable instanceof TextureReloadable)
        ((TextureReloadable) drawable).reloadTexture();
  }

  // Expands an item which has an integer value, pushing all following items down to make room for
  // the plus/minus buttons interface.
  /*public void expandItem(MenuItem item) {
    if (contents.contains(item) && item.getValue() != null) {

      // If it has an integer value - expand it.
      if (item.getValue().getType() == MenuValue.Type.INTEGER) {
        if (item == expandedItem) {
          // If the item pressed has already been expanded, then retract it and all other items.
          for (int itemIndex = 0; itemIndex < con.length; itemIndex++) {
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
  }*/
}
