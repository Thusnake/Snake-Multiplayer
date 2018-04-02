package thusnake.snakemultiplayer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Thusnake on 01-Jul-16.
 */
public class Mesh {
  private FloatBuffer verticesBuffer = null;
  private ShortBuffer indicesBuffer = null;
  private int numOfIndices = -1;
  private float[] rgba = new float[]{1.0f,1.0f,1.0f,1.0f};
  private FloatBuffer colorBuffer = null;
  private float[] vertices = null;
  private short[] indices = null;
  private float[] colors = null;
  private int numOfSquares = 0;
  private final BoardDrawer game;
  
  public Mesh (BoardDrawer game) { this.game = game; }
  public Mesh () { this.game = null; }

  protected void setVertices(float[] vertices) {
    ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
    vbb.order(ByteOrder.nativeOrder());
    verticesBuffer = vbb.asFloatBuffer();
    verticesBuffer.put(vertices);
    verticesBuffer.position(0);
  }

  protected void setIndices(short[] indices) {
    ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
    ibb.order(ByteOrder.nativeOrder());
    indicesBuffer = ibb.asShortBuffer();
    indicesBuffer.put(indices);
    indicesBuffer.position(0);
    numOfIndices = indices.length;
  }

  protected void setColor(float red, float green, float blue, float alpha) {
    rgba[0] = red;
    rgba[1] = green;
    rgba[2] = blue;
    rgba[3] = alpha;
  }

  protected void setColors(float[] colors) {
    ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
    cbb.order(ByteOrder.nativeOrder());
    colorBuffer = cbb.asFloatBuffer();
    colorBuffer.put(colors);
    colorBuffer.position(0);
  }

  public void addSquare(float x, float y, float width, float height){
    short indices[] = { 0, 1, 2, 0, 2, 3 };
    float vertices[] = {x, y+height, 0.0f,   // top left
        x, y, 0.0f,   // bottom left
        x+width, y, 0.0f,   // bottom right
        x+width, y+height, 0.0f }; // top right
    float colors[] = {0.125f,0.125f,0.125f,1.0f,
        0.125f,0.125f,0.125f,1.0f,
        0.125f,0.125f,0.125f,1.0f,
        0.125f,0.125f,0.125f,1.0f};
    if (numOfSquares == 0){
      this.indices = indices;
      this.vertices = vertices;
      this.colors = colors;
    } else {
      this.vertices = concatf(this.vertices,vertices);
      for (int i=0; i<indices.length; i++){
        indices[i] += numOfSquares * 4;
      }
      this.indices = concats(this.indices, indices);
      this.colors = concatf(this.colors, colors);
    }
    this.numOfSquares++;
  }

  public void addSquare(double x, double y, double width, double height) {
    this.addSquare((float) x, (float) y, (float) width, (float) height);
  }

  public void applySquares(){
    setVertices(this.vertices);
    setIndices(this.indices);
    setColors(this.colors);
  }

  public void updateColors(int i, int j, float color[]){
    for (int n=0; n<4;n++) {
      this.colors[((i) + (j) * game.getHorizontalSquares())*16 + n*4] = color[0];
      this.colors[((i) + (j) * game.getHorizontalSquares())*16 + n*4 + 1] = color[1];
      this.colors[((i) + (j) * game.getHorizontalSquares())*16 + n*4 + 2] = color[2];
      this.colors[((i) + (j) * game.getHorizontalSquares())*16 + n*4 + 3] = color[3];
    }
    setColors(this.colors);
  }

  public void updateColors(int i, int j, double color[]){
    float[] colorf = {(float) color[0], (float) color[1], (float) color[2], (float) color[3]};
    this.updateColors(i,j,colorf);
  }

  public void updateColors(int i, int j, float r, float g, float b, float a){
    for (int n=0; n<4;n++) {
      this.colors[((i - 1) + (j - 1) * game.getHorizontalSquares())*16 + n*4] = r;
      this.colors[((i - 1) + (j - 1) * game.getHorizontalSquares())*16 + n*4 + 1] = g;
      this.colors[((i - 1) + (j - 1) * game.getHorizontalSquares())*16 + n*4 + 2] = b;
      this.colors[((i - 1) + (j - 1) * game.getHorizontalSquares())*16 + n*4 + 3] = a;
    }
    setColors(this.colors);
  }

  public void updateColors(int i, float r, float g, float b, float a) {
    for (int n=0; n<4;n++) {
      this.colors[i*16 + n*4] = r;
      this.colors[i*16 + n*4 + 1] = g;
      this.colors[i*16 + n*4 + 2] = b;
      this.colors[i*16 + n*4 + 3] = a;
    }
    setColors(this.colors);
  }

  public void draw(GL10 gl){
    gl.glFrontFace(GL10.GL_CCW);
    gl.glEnable(GL10.GL_CULL_FACE);
    gl.glCullFace(GL10.GL_BACK);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, verticesBuffer);
    gl.glColor4f(rgba[0], rgba[1], rgba[2], rgba[3]);
    if (colorBuffer != null){
      gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
      gl.glColorPointer(4,GL10.GL_FLOAT,0,colorBuffer);
    }
    gl.glDrawElements(GL10.GL_TRIANGLES,numOfIndices,GL10.GL_UNSIGNED_SHORT,indicesBuffer);
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
    gl.glDisable(GL10.GL_CULL_FACE);
  }

  public static float[] concatf(float[] a, float[] b) {
    int aLen = a.length;
    int bLen = b.length;
    float[] c= new float[aLen+bLen];
    System.arraycopy(a, 0, c, 0, aLen);
    System.arraycopy(b, 0, c, aLen, bLen);
    return c;
  }

  public static short[] concats(short[] a, short[] b) {
    int aLen = a.length;
    int bLen = b.length;
    short[] c= new short[aLen+bLen];
    System.arraycopy(a, 0, c, 0, aLen);
    System.arraycopy(b, 0, c, aLen, bLen);
    return c;
  }
}
