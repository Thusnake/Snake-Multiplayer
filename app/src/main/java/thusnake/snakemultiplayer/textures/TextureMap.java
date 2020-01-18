package thusnake.snakemultiplayer.textures;

import android.graphics.Bitmap;

public abstract class TextureMap {
  Bitmap textureMap = null;

  public void recycle() { textureMap.recycle(); }

  /** @return The whole Bitmap image of the texture map. */
  public Bitmap getBitmap() { return textureMap; }
}
