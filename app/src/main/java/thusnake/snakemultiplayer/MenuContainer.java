package thusnake.snakemultiplayer;

import android.view.MotionEvent;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class MenuContainer extends MenuDrawable implements TextureReloadable {
  final List<MenuDrawable> contents = new CopyOnWriteArrayList<>();

  public MenuContainer(GameRenderer renderer, float x, float y, EdgePoint alignPoint,
                       EdgePoint originPoint) {
    super(renderer, x, y, alignPoint, originPoint);
  }

  public MenuContainer(GameRenderer renderer, float x, float y, EdgePoint alignPoint) {
    this(renderer, x, y, alignPoint, EdgePoint.CENTER);
  }

  public void draw(float[] parentColors) {
    if (isDrawable())
      for (MenuDrawable drawable : contents)
        drawable.draw(combineColorArrays(getColors(), parentColors));
  }

  public void move(double dt) {
    super.move(dt);
    for (MenuDrawable drawable : contents)
      drawable.move(dt);
  }

  @Override
  public void reloadTexture() {
    for (MenuDrawable drawable : contents)
      if (drawable instanceof TextureReloadable)
        ((TextureReloadable) drawable).reloadTexture();
  }

  @Override
  public void onMotionEvent(MotionEvent event, float[] pointerX, float[] pointerY) {
    super.onMotionEvent(event, pointerX, pointerY);

    for (MenuDrawable drawable : contents)
      drawable.onMotionEvent(event, pointerX, pointerY);
  }

  public void addItem(MenuDrawable item) {
    contents.add(item);
  }

  public void removeItem(MenuDrawable item) {
    contents.remove(item);
  }
}
