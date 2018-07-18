package thusnake.snakemultiplayer;

import android.util.Pair;

import java.util.LinkedList;
import java.util.List;

public class MenuCarousel extends MenuDrawable implements TextureReloadable {
  private List<Pair<MenuDrawable, String>> choices = new LinkedList<>();

  public MenuCarousel(GameRenderer renderer, float x, float y, float width, float height) {
    super(renderer, x, y);

    setWidth(width);
    setHeight(height);
  }

  @Override
  public void draw() {

  }

  @Override
  public void move(double dt) {

  }

  @Override
  public void reloadTexture() {

  }

  /**
   * Adds a custom choice to the carousel.
   * @param drawable The MenuDrawable which visually represents the choice. It MUST have its x and
   *                 y coordinate variables set to 0.
   * @param name The name of the choice.
   */
  public void addChoice(MenuDrawable drawable, String name) {
    choices.add(new Pair<>(drawable, name));
  }

  /**
   * Adds a choice to the carousel, visually represented via an image.
   * @param resourceId The ID of the image resource.
   * @param name The name of the choice.
   */
  public void addImageChoice(int resourceId, String name) {
    choices.add(new Pair<>(new MenuImage(renderer, 0, 0, resourceId), name));
  }
}
