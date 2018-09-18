package thusnake.snakemultiplayer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Thusnake on 01-Jul-16.
 */
public class Mesh extends MenuDrawable implements TextureReloadable {
  private FloatBuffer verticesBuffer = null;
  private ShortBuffer indicesBuffer = null;
  private int numOfIndices = -1;
  private float[] rgba = new float[]{1.0f,1.0f,1.0f,1.0f};
  private FloatBuffer colorBuffer = null;
  private FloatBuffer textureBuffer = null;
  private float[] vertices = null;
  private short[] indices = null;
  private float[] colors = null;
  private float[] textures = null;
  private int[] texturePointers = new int[1];
  private int textureId;
  private boolean textureLoaded = true;
  private int numOfSquares = 0;

  private final float squareSize;
  private final int horizontalSquares;
  private final int verticalSquares;
  
  public Mesh (GameRenderer renderer, float x, float y, EdgePoint alignPoint, float squareSize,
               BoardDrawer game) {
    this(renderer, x, y, alignPoint, squareSize,
        game.getHorizontalSquares(), game.getVerticalSquares());
  }

  public Mesh (GameRenderer renderer, float x, float y, EdgePoint alignPoint, float squareSize,
               int horizontalSquares, int verticalSquares) {
    super(renderer, x, y, alignPoint);
    this.squareSize = squareSize;
    this.horizontalSquares = horizontalSquares;
    this.verticalSquares = verticalSquares;
    this.addAllSquares();
    setTexture(R.drawable.singleplayer_icon);
  }

  private void addAllSquares() {
    for (int y = 0; y < this.verticalSquares; y++) {
      for (int x = 0; x < this.horizontalSquares; x++) {
        this.addSquare(-getEdgePointOffset(originPoint).first + squareSize * x + x + 1,
                       -getEdgePointOffset(originPoint).second + y + 1 + squareSize * y,
                       squareSize, squareSize);
      }
    }
    this.applySquares();
  }

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

  public void setColors(float[] colors) {
    ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
    cbb.order(ByteOrder.nativeOrder());
    colorBuffer = cbb.asFloatBuffer();
    colorBuffer.put(colors);
    colorBuffer.position(0);
  }

  public void setTextures(float[] textures) {
    ByteBuffer tbb = ByteBuffer.allocateDirect(textures.length * 4);
    tbb.order(ByteOrder.nativeOrder());
    textureBuffer = tbb.asFloatBuffer();
    textureBuffer.put(textures);
    textureBuffer.position(0);
  }

  private void addSquare(float x, float y, float width, float height){
    short indices[] = { 0, 1, 2, 0, 2, 3 };
    float vertices[] = {x, y+height, 0.0f,   // top left
        x, y, 0.0f,   // bottom left
        x+width, y, 0.0f,   // bottom right
        x+width, y+height, 0.0f }; // top right
    float colors[] = {0.125f,0.125f,0.125f,1.0f,
        0.125f,0.125f,0.125f,1.0f,
        0.125f,0.125f,0.125f,1.0f,
        0.125f,0.125f,0.125f,1.0f};
    float[] textures = {0, 0,
                        0, 0.0125f,
                        0.0125f, 0.0125f,
                        0.0125f, 0};
    if (numOfSquares == 0){
      this.indices = indices;
      this.vertices = vertices;
      this.colors = colors;
      this.textures = textures;
    } else {
      this.vertices = concatf(this.vertices,vertices);
      for (int i=0; i<indices.length; i++){
        indices[i] += numOfSquares * 4;
      }
      this.indices = concats(this.indices, indices);
      this.colors = concatf(this.colors, colors);
      this.textures = concatf(this.textures, textures);
    }
    this.numOfSquares++;
  }

  private void applySquares(){
    setVertices(this.vertices);
    setIndices(this.indices);
    setColors(this.colors);
    setTextures(this.textures);
  }

  public void updateColors(int i, int j, float color[]){
    for (int n=0; n<4;n++) {
      this.colors[(i + j * horizontalSquares)*16 + n*4] = color[0];
      this.colors[(i + j * horizontalSquares)*16 + n*4 + 1] = color[1];
      this.colors[(i + j * horizontalSquares)*16 + n*4 + 2] = color[2];
      this.colors[(i + j * horizontalSquares)*16 + n*4 + 3] = color[3];
    }
    setColors(this.colors);
  }

  public void updateColors(int i, int j, double color[]){
    float[] colorFloat = {(float) color[0], (float) color[1], (float) color[2], (float) color[3]};
    this.updateColors(i, j, colorFloat);
  }

  public void updateColors(int i, int j, float r, float g, float b, float a){
    for (int n=0; n<4;n++) {
      this.colors[(i + j * horizontalSquares)*16 + n*4] = r;
      this.colors[(i + j * horizontalSquares)*16 + n*4 + 1] = g;
      this.colors[(i + j * horizontalSquares)*16 + n*4 + 2] = b;
      this.colors[(i + j * horizontalSquares)*16 + n*4 + 3] = a;
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

  public void updateColors(int i, float[] rgba) {
    this.updateColors(i, rgba[0], rgba[1], rgba[2], rgba[3]);
  }

  public float[] getColors(int i, int j) {
    return new float[] {
        colors[(i + j * horizontalSquares) * 16],
        colors[(i + j * horizontalSquares) * 16 + 1],
        colors[(i + j * horizontalSquares) * 16 + 2],
        colors[(i + j * horizontalSquares) * 16 + 3]
    };
  }

  public float[] getColors(int i) {
    return new float[] {
        colors[i*16],
        colors[i*16 + 1],
        colors[i*16 + 2],
        colors[i*16 + 3]
    };
  }

  public void updateTextures(int x, int y, float leftX, float bottomY, float rightX, float topY) {
    this.textures[(x + y * horizontalSquares)*8] = leftX;
    this.textures[(x + y * horizontalSquares)*8 + 1] = topY;

    this.textures[(x + y * horizontalSquares)*8 + 2] = leftX;
    this.textures[(x + y * horizontalSquares)*8 + 3] = bottomY;

    this.textures[(x + y * horizontalSquares)*8 + 4] = rightX;
    this.textures[(x + y * horizontalSquares)*8 + 5] = bottomY;

    this.textures[(x + y * horizontalSquares)*8 + 6] = rightX;
    this.textures[(x + y * horizontalSquares)*8 + 7] = topY;

    setTextures(this.textures);
  }

  public void setTexture(int id) {
    textureLoaded = false;
    textureId = id;
  }

  public void loadGLTexture(int id) {
    // Get the texture from the renderer.
    texturePointers[0] = renderer.loadTextureBitmapToPointer(id);
    textureId = id;
    textureLoaded = true;
  }

  public void reloadTexture() {
    if (textureId != 0) {
      texturePointers[0] = renderer.loadTextureBitmapToPointer(textureId);
    }
  }

  public void draw(GL10 gl, float[] parentColors){
    if (!textureLoaded)
      loadGLTexture(textureId);

    gl.glPushMatrix();
    gl.glTranslatef(getX(originPoint), getY(originPoint), 0);
    gl.glScalef((float) scale.getTime(), (float) scale.getTime(), 0);

    gl.glFrontFace(GL10.GL_CCW);
    gl.glEnable(GL10.GL_CULL_FACE);
    gl.glCullFace(GL10.GL_BACK);
    gl.glBindTexture(GL10.GL_TEXTURE_2D, texturePointers[0]); // Bind texture.
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);              // Enable client states.
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, verticesBuffer);  // Point the pointers.
    glColor4array(gl, combineColorArrays(rgba, parentColors));
    if (colorBuffer != null){                                 // Calculate the colors.
      gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

      FloatBuffer currentColorBuffer = colorBuffer;
      // If the parent colors or the drawable's colors are not the default ones we want to multiply
      // them with the entire color buffer.
      if (!(parentColors[0] == 1f && parentColors[1] == 1f && parentColors[2] == 1f && parentColors[3] == 1f)
          || !(getColors()[0] == 1f && getColors()[1] == 1f && getColors()[2] == 1f && getColors()[3] == 1f)) {
        float[] multipliedColors = new float[colorBuffer.capacity()];
        for (int index = 0; index < multipliedColors.length; index += 16) {
          for (int n=0; n<4;n++) {
            multipliedColors[index + n*4]     = parentColors[0] * getColors()[0] * colorBuffer.get(index + n*4);
            multipliedColors[index + n*4 + 1] = parentColors[1] * getColors()[1] * colorBuffer.get(index + n*4 + 1);
            multipliedColors[index + n*4 + 2] = parentColors[2] * getColors()[2] * colorBuffer.get(index + n*4 + 2);
            multipliedColors[index + n*4 + 3] = parentColors[3] * getColors()[3] * colorBuffer.get(index + n*4 + 3);
          }
        }

        ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
        cbb.order(ByteOrder.nativeOrder());
        currentColorBuffer = cbb.asFloatBuffer();
        currentColorBuffer.put(multipliedColors);
        currentColorBuffer.position(0);
      }

      gl.glColorPointer(4,GL10.GL_FLOAT,0,currentColorBuffer);
    }
    gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer); // Point the rest of the pointers.
    gl.glDrawElements(GL10.GL_TRIANGLES,numOfIndices,GL10.GL_UNSIGNED_SHORT,indicesBuffer); // Draw.
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);            // Disable the client states.
    gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
    gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    gl.glDisable(GL10.GL_CULL_FACE);

    gl.glPopMatrix();
  }

  public void draw(float[] parentColors) {
    if (isDrawable())
      draw(gl, parentColors);
  }

  @Override
  public float getWidth() {
    return horizontalSquares * squareSize + (horizontalSquares - 1);
  }

  @Override
  public float getHeight() {
    return verticalSquares * squareSize + (verticalSquares - 1);
  }

  private static float[] concatf(float[] a, float[] b) {
    int aLen = a.length;
    int bLen = b.length;
    float[] c= new float[aLen+bLen];
    System.arraycopy(a, 0, c, 0, aLen);
    System.arraycopy(b, 0, c, aLen, bLen);
    return c;
  }

  private static short[] concats(short[] a, short[] b) {
    int aLen = a.length;
    int bLen = b.length;
    short[] c= new short[aLen+bLen];
    System.arraycopy(a, 0, c, 0, aLen);
    System.arraycopy(b, 0, c, aLen, bLen);
    return c;
  }
}
