package thusnake.snakemultiplayer;

import com.android.texample.GLText;

import javax.microedition.khronos.opengles.GL10;

public abstract class BoardDrawer {
  private final GameRenderer renderer;
  private final GL10 gl;
  private final GLText glText;
  private float boardOffsetX, boardOffsetY, squareWidth;
  private int screenWidth, screenHeight;
  private final Square boardOutline, boardFill;
  private final Mesh boardSquares = new Mesh(this);
  private final float[] boardSquareColors = {0.125f, 0.125f, 0.125f, 1.0f};

  public final int horizontalSquares, verticalSquares;
  public final boolean stageBorders;

  public BoardDrawer(GameRenderer renderer, int screenWidth, int screenHeight) {
    this.renderer = renderer;
    this.gl = renderer.getGl();
    this.glText = renderer.getGlText();

    this.horizontalSquares = renderer.getMenu().horizontalSquares;
    this.verticalSquares = renderer.getMenu().verticalSquares;
    this.stageBorders = renderer.getMenu().stageBorders;

    // TODO Calculate board offset.
    this.boardOffsetY = 10.0f;
    this.boardOffsetX
        = (screenWidth - ((screenHeight - 20f) / this.verticalSquares) * horizontalSquares) / 2f;
    this.screenWidth = screenWidth;
    this.screenHeight = screenHeight;
    this.squareWidth
        = (float) ((screenHeight - 20.0 - this.verticalSquares - 1.0) / verticalSquares);
    // Apply offset to some Squares.
    this.boardOutline = new Square(boardOffsetX - 1.0, boardOffsetY - 1.0,
        screenWidth - boardOffsetX*2 + 2.0, screenHeight - boardOffsetY*2 + 2.0);
    this.boardFill = new Square(boardOffsetX, boardOffsetY,
        screenWidth - boardOffsetX*2, screenHeight - boardOffsetY*2);

    // Create the board mesh.
    for (int y = 0; y < this.verticalSquares; y++) {
      for (int x = 0; x < this.horizontalSquares; x++) {
        this.boardSquares.addSquare(this.boardOffsetX + this.squareWidth * x + x + 1,
            this.boardOffsetY + y + 1 + this.squareWidth * y,
            this.squareWidth, this.squareWidth);
      }
    }
    boardSquares.applySquares();

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

  public void drawControllerLayout(Player player) {
    gl.glColor4f(player.getColors()[0], player.getColors()[1],
        player.getColors()[2], player.getColors()[3]);
    // For some unknown reason I can not load the texture inside the constructor properly.
    // This is my band-aid fix.
    if (!player.getPlayerController().textureIsLoaded())
      player.getPlayerController().loadGLTexture(this.renderer.getContext());
    player.getPlayerController().draw(gl);
  }

  public void drawPlayerSnake(Player player) {
    for (BodyPart bodyPart : player.getBodyParts()) {
      if (!bodyPart.isOutOfBounds() && player.isDrawable())
        this.getBoardSquares()
            .updateColors(bodyPart.getX(), bodyPart.getY(), bodyPart.getColors());
      else if (!bodyPart.isOutOfBounds())
        this.getBoardSquares()
            .updateColors(bodyPart.getX(), bodyPart.getY(), this.getBoardSquareColors());
    }
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
        (this.getScreenHeight() - glText.getCharHeight()) / 2f);
    glText.end();
    gl.glPopMatrix();
  }

  public abstract void handleInputBytes(byte[] inputBytes, ConnectedThread source);

  // Getters.
  public int getScreenWidth() { return this.screenWidth; }
  public int getScreenHeight() { return this.screenHeight; }
  public int getHorizontalSquares() { return this.horizontalSquares; }
  public int getVerticalSquares() { return this.verticalSquares; }
  public Mesh getBoardSquares() { return this.boardSquares; }
  public float[] getBoardSquareColors() { return this.boardSquareColors; }
  public GameRenderer getRenderer() { return this.renderer; }
  public abstract Player[] getPlayers();
}
