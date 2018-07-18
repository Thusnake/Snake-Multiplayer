package thusnake.snakemultiplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 22/01/2018.
 */

public class MenuImage extends MenuDrawable implements TextureReloadable {
  private SimpleTimer scaleX = new SimpleTimer(1.0), scaleY = new SimpleTimer(1.0);
  private Square drawable;

  public MenuImage(GameRenderer renderer, float x, float y, float width, float height) {
    super(renderer, x, y);
    this.setWidth(width);
    this.setHeight(height);
    this.drawable = new Square(renderer, x, y, width, height);
  }

  public MenuImage(GameRenderer renderer, float x, float y, int resourceId) {
    super(renderer, x, y);

    // Try finding the resource in the cache.
    Bitmap image;
    if ((image = renderer.getTextureFromCache(resourceId)) == null) {
      image = BitmapFactory.decodeResource(renderer.getOriginActivity().getResources(), resourceId);

      // Cache the texture for next time if it had to be decoded.
      renderer.cacheTexture(image, resourceId);
    }

    setWidth(image.getWidth());
    setHeight(image.getHeight());
    drawable = new Square(renderer, x, y, getWidth(), getHeight());
    drawable.setTexture(resourceId);
  }

  // Drawing methods
  public void draw() {
    this.drawable.draw(this.gl);
  }

  public void setTexture(int id) {
    this.drawable.setTexture(id);
  }

  @Override
  public void reloadTexture() {
    drawable.reloadTexture();
  }

  public void move(double dt) {}
}
