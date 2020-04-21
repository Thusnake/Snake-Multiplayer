package thusnake.snakemultiplayer;

import com.android.texample.GLText;

import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import thusnake.snakemultiplayer.netplay.ConnectedThread;
import thusnake.snakemultiplayer.textures.GameTextureMap;

/**
 * Draws the game's board. Really not that useful, as the game is the only class that extends this
 * and it has to supply some stuff, like the board's Mesh, manually.
 */
public abstract class BoardDrawer {
  private final GameRenderer renderer;
  private final GL10 gl;
  private final GLText glText;
  private float boardOffsetX, boardOffsetY, squareWidth;
  private int screenWidth, screenHeight;
  private final Square boardOutline, boardFill;
  private Mesh boardSquares;
  private final float[] boardSquareColors = {0.125f, 0.125f, 0.125f, 1.0f};

  public final int horizontalSquares, verticalSquares;
  public final boolean stageBorders;

  public BoardDrawer(int horizontalSquares, int verticalSquares, boolean stageBorders) {
    renderer = OpenGLActivity.current.getRenderer();
    screenWidth = (int) renderer.getScreenWidth();
    screenHeight = (int) renderer.getScreenHeight();
    gl = renderer.getGl();
    glText = renderer.getGlText();

    this.horizontalSquares = horizontalSquares;
    this.verticalSquares = verticalSquares;
    this.stageBorders = stageBorders;

    // TODO Calculate board offset.
    this.boardOffsetY = 10.0f;
    this.boardOffsetX
        = (screenWidth - ((screenHeight - 20f) / this.verticalSquares) * horizontalSquares) / 2f;
    this.squareWidth
        = (float) ((screenHeight - 20.0) / verticalSquares);
    // Apply offset to some Squares.
    this.boardOutline = new Square(renderer, boardOffsetX - 1.0, boardOffsetY - 1.0,
        screenWidth - boardOffsetX*2 + 2.0, screenHeight - boardOffsetY*2 + 2.0);
    this.boardFill = new Square(renderer, boardOffsetX, boardOffsetY,
        screenWidth - boardOffsetX*2, screenHeight - boardOffsetY*2);

    this.gl.glEnable(GL10.GL_TEXTURE_2D);
    this.gl.glEnable(GL10.GL_BLEND);
    this.gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
  }

  public abstract void run(double dt);

  // Handles the drawing of the board itself.
  public void drawBoard() {
    gl.glColor4f(1f, 1f, 1f, 1f);
    boardOutline.draw(gl);
    gl.glColor4f(0f, 0f, 0f, 1f);
    boardFill.draw(gl);
  }

  public void drawControllerLayout(Snake snake) {
    float[] colors = snake.getSkin().headColors();
    gl.glColor4f(colors[0], colors[1], colors[2], colors[3]);

    snake.controller.draw();
  }

  public void drawCountdownText(SimpleTimer beginTimer) {
    gl.glPushMatrix();
    gl.glScalef(2f, 2f, 1f);
    gl.glTranslatef(-this.getScreenWidth() / 4f, -this.getScreenHeight() / 4f, 0f);
    glText.begin(1f, 1f, 1f, 1f);
    String countdownText;
    if (beginTimer.getTime() < 2.0) countdownText =
        Integer.toString(3 - (int)(beginTimer.getTime() * 3.0 / 2.0));
    else countdownText = "Go!";
    glText.draw(countdownText, (this.getScreenWidth() - glText.getLength(countdownText)) / 2f,
        (this.getScreenHeight() - glText.getHeight()) / 2f);
    glText.end();
    gl.glPopMatrix();
  }

  public abstract void handleInputBytes(byte[] inputBytes, ConnectedThread source);

  /**
   * Generates the game mesh used for drawing the square tiles.
   * This method <b>must</b> be called before attempting to do anything with the boardSquares.
   * @param meshTextureMap The texture map to bind to the generated Mesh.
   */
  public void generateMesh(GameTextureMap meshTextureMap) {
    boardSquares = new Mesh(renderer, boardOffsetX, boardOffsetY,
        MenuDrawable.EdgePoint.BOTTOM_LEFT, squareWidth, this, meshTextureMap);
  }

  // Getters.
  public int getScreenWidth() { return this.screenWidth; }
  public int getScreenHeight() { return this.screenHeight; }
  public int getHorizontalSquares() { return this.horizontalSquares; }
  public int getVerticalSquares() { return this.verticalSquares; }
  public Mesh getBoardSquares() { return this.boardSquares; }
  public float[] getBoardSquareColors() { return this.boardSquareColors; }
  public GameRenderer getRenderer() { return this.renderer; }
  public abstract List<Player> getPlayers();
}
