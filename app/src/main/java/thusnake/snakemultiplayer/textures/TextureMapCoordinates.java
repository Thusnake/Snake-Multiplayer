package thusnake.snakemultiplayer.textures;

import android.graphics.Bitmap;

public class TextureMapCoordinates {
  public final int offsetX, offsetY, width, height;

  public TextureMapCoordinates(int offset, Bitmap bitmap) {
    this.offsetX = offset;
    this.offsetY = 0;
    this.width = bitmap.getWidth();
    this.height = bitmap.getHeight();
  }

  public TextureMapCoordinates(int offsetX, int offsetY, int width, int height) {
    this.offsetX = offsetX;
    this.offsetY = offsetY;
    this.width = width;
    this.height = height;
  }
}