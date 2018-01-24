package thusnake.snakemultiplayer;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by ASRock on 24-Feb-16.
 */
public class Square {
  private FloatBuffer vertexBuffer;
  private ShortBuffer drawListBuffer;

  private FloatBuffer textureBuffer;  // buffer holding the texture coordinates
  private float texture[] = {
      // Mapping coordinates for the vertices
      0.0f, 0.0f,    // top left     (V2)
      0.0f, 1.0f,   // bottom left  (V1)
      1.0f, 1.0f,    // bottom right (V3)
      1.0f, 0.0f    // top right    (V4)

  };


  float color[] = {1.0f,1.0f,0.0f,1.0f};

  private short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

  public Square(float x,float y,float width,float height) {
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

  public Square(double x,double y,double width,double height) {
    float squareCoords[] = {
        (float)x, (float)y+(float)height, 0.0f,   // top left
        (float)x, (float)y, 0.0f,   // bottom left
        (float)x+(float)width, (float)y, 0.0f,   // bottom right
        (float)x+(float)width, (float)y+(float)height, 0.0f }; // top right

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

  /** The texture pointer */
  private int[] textures = new int[1];

  public void loadGLTexture(GL10 gl, Context context, int id) {
    // loading texture
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
        id);

    // generate one texture pointer
    gl.glGenTextures(1, textures, 0);
    // ...and bind it to our array
    gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

    // create nearest filtered texture
    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

    // Use Android GLUtils to specify a two-dimensional texture image from our bitmap
    GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

    // Clean up
    bitmap.recycle();
  }


  public void draw(GL10 gl){
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

  public void draw(GLES11 gl){
    // Counter-clockwise winding.
    gl.glFrontFace(GLES11.GL_CCW);
    // Enable face culling.
    gl.glEnable(GLES11.GL_CULL_FACE);
    // What faces to remove with the face culling.
    gl.glCullFace(GLES11.GL_BACK);

    // bind the previously generated texture
    gl.glBindTexture(GLES11.GL_TEXTURE_2D, textures[0]);

    // Enabled the vertices buffer for writing and to be used during rendering.
    gl.glEnableClientState(GLES11.GL_VERTEX_ARRAY);
    gl.glEnableClientState(GLES11.GL_TEXTURE_COORD_ARRAY);

    // Specifies the location and data format of an array of vertex coordinates to use when rendering.
    gl.glVertexPointer(3, GLES11.GL_FLOAT, 0, vertexBuffer);
    gl.glTexCoordPointer(2, GLES11.GL_FLOAT, 0, textureBuffer);

    gl.glDrawElements(GLES11.GL_TRIANGLES, drawOrder.length, GLES11.GL_UNSIGNED_SHORT, drawListBuffer);
    //gl.glDrawArrays(GLES11.GL_TRIANGLE_STRIP,0,squareCoords.length/3);

    // Disable the vertices buffer.
    gl.glDisableClientState(GLES11.GL_VERTEX_ARRAY);
    gl.glDisableClientState(GLES11.GL_TEXTURE_COORD_ARRAY);

    // Disable face culling.
    gl.glDisable(GLES11.GL_CULL_FACE);

  }
}
