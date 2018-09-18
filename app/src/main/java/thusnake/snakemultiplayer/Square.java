package thusnake.snakemultiplayer;

import android.graphics.Bitmap;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by ASRock on 24-Feb-16.
 */
public class Square implements TextureReloadable {
  private FloatBuffer vertexBuffer;
  private ShortBuffer drawListBuffer;
  private FloatBuffer textureBuffer;  // buffer holding the texture coordinates
  private short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

  private int textureId;
  private boolean textureLoaded = true;
  private GL10 gl;
  private OpenGLES20Activity originActivity;

  public Square(GameRenderer renderer, float x,float y,float width,float height) {
    gl = renderer.getGl();
    originActivity = renderer.getOriginActivity();

    float texture[] = {
        0.0f, 0.0f,   // top left     (V2)
        0.0f, 1.0f,   // bottom left  (V1)
        1.0f, 1.0f,   // bottom right (V3)
        1.0f, 0.0f    // top right    (V4)
    };

    float squareCoords[] = {
        x, y+height, 0.0f,      // top left
        x, y, 0.0f,             // bottom left
        x+width, y, 0.0f,       // bottom right
        x+width, y+height, 0.0f // top right
    };

    // initialize vertex byte buffer for shape coordinates
    ByteBuffer bb = ByteBuffer.allocateDirect(
        // (# of coordinate values * 4 bytes per float)
        squareCoords.length * 4);
    bb.order(ByteOrder.nativeOrder());
    vertexBuffer = bb.asFloatBuffer();
    vertexBuffer.put(squareCoords);
    vertexBuffer.position(0);

    bb = ByteBuffer.allocateDirect(texture.length * 4);
    bb.order(ByteOrder.nativeOrder());
    textureBuffer = bb.asFloatBuffer();
    textureBuffer.put(texture);
    textureBuffer.position(0);

    // initialize byte buffer for the draw list
    ByteBuffer dlb = ByteBuffer.allocateDirect(
        // (# of coordinate values * 2 bytes per short)
        drawOrder.length * 2);
    dlb.order(ByteOrder.nativeOrder());
    drawListBuffer = dlb.asShortBuffer();
    drawListBuffer.put(drawOrder);
    drawListBuffer.position(0);
  }

  public Square(GameRenderer renderer, double x,double y,double width,double height) {
    this(renderer, (float) x, (float) y, (float) width, (float) height);
  }

  public void setCoordinates(float x, float y, float width, float height) {
    float squareCoords[] = {
        x, y+height, 0.0f,   // top left
        x, y, 0.0f,   // bottom left
        x+width, y, 0.0f,   // bottom right
        x+width, y+height, 0.0f }; // top right

    // initialize vertex byte buffer for shape coordinates
    ByteBuffer bb = ByteBuffer.allocateDirect(
        // (# of coordinate values * 4 bytes per float)
        squareCoords.length * 4);
    bb.order(ByteOrder.nativeOrder());
    vertexBuffer = bb.asFloatBuffer();
    vertexBuffer.put(squareCoords);
    vertexBuffer.position(0);
  }

  /** The texture pointer */
  private int[] textures = new int[1];

  public void setTexture(int id) {
    textureLoaded = false;
    textureId = id;
  }

  public void loadGLTexture(int id) {
    // Get the texture from the renderer.
    Bitmap bitmap = originActivity.getRenderer().loadTextureBitmap(id);

    // generate one texture pointer
    gl.glDeleteTextures(1, textures, 0);
    gl.glGenTextures(1, textures, 0);
    // ...and bind it to our array
    gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

    // create nearest filtered texture
    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

    // Use Android GLUtils to specify a two-dimensional texture image from our bitmap
    GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

    textureId = id;
    textureLoaded = true;
  }

  public void reloadTexture() {
    if (textureId != 0) {
      Bitmap bitmap = originActivity.getRenderer().loadTextureBitmap(textureId);

      gl.glDeleteTextures(1, textures, 0);
      gl.glGenTextures(1, textures, 0);
      gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

      gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
      gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

      GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
    }
  }


  public void draw(GL10 gl) {
    if (!textureLoaded)
      loadGLTexture(textureId);

    // Counter-clockwise winding.
    gl.glFrontFace(GL10.GL_CCW);
    // Enable face culling.
    gl.glEnable(GL10.GL_CULL_FACE);
    // What faces to remove with the face culling.
    gl.glCullFace(GL10.GL_BACK);

    // bind the previously generated texture
    gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

    // Enabled the vertices buffer for writing and to be used during rendering.
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

    // Specifies the location and data format of an array of vertex coordinates to use when rendering.
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
    gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);

    gl.glDrawElements(GL10.GL_TRIANGLES, drawOrder.length, GL10.GL_UNSIGNED_SHORT, drawListBuffer);
    //gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP,0,squareCoords.length/3);

    // Disable the vertices buffer.
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

    // Disable face culling.
    gl.glDisable(GL10.GL_CULL_FACE);

  }
}
