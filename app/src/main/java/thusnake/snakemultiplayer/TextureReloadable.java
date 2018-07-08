package thusnake.snakemultiplayer;

import android.content.Context;

import javax.microedition.khronos.opengles.GL10;

public interface TextureReloadable {
  void reloadGLTexture(GL10 gl, Context context);
}
