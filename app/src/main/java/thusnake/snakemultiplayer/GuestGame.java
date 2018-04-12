package thusnake.snakemultiplayer;

import android.content.Context;
import com.android.texample.GLText;
import javax.microedition.khronos.opengles.GL10;

public class GuestGame extends BoardDrawer {
  private final GameRenderer renderer;
  private final GL10 gl;
  private final GLText glText;
  private final Context context;
  private final OpenGLES20Activity originActivity;
  private final SimpleTimer beginTimer, gameOverTimer, screenRumbleTimer;
  private boolean gameOver;
  private byte winner;
  private final Player[] players;
  private final Square[] boardLines = new Square[2];
  private final Square boardFade;
  private float screenTransformX, screenTransformY;

  public GuestGame(GameRenderer renderer, int screenWidth, int screenHeight, Player[] players) {
    super(renderer, screenWidth, screenHeight);

    this.renderer = renderer;
    this.gl = renderer.getGl();
    this.glText = renderer.getGlText();
    this.context = renderer.getContext();
    this.originActivity = (OpenGLES20Activity) context;

    this.beginTimer = new SimpleTimer(0.0, 0.3);
    this.gameOverTimer = new SimpleTimer(0.0);
    this.screenRumbleTimer = new SimpleTimer(0.0);

    this.screenTransformX = 0f;
    this.screenTransformY = 0f;
    this.gameOver = false;
    this.winner = -1;

    this.players = players;
    for (int index = 0; index < players.length; index++)
      if (players[index] != null)
        players[index].prepareForGame(this, index);

    // Create the apple.
    // TODO

    // Create the rest of the square objects.
    this.boardLines[0] = new Square(0, screenHeight/3f, screenWidth, 4);
    this.boardLines[1] = new Square(0, screenHeight*2f/3f, screenWidth, 4);
    this.boardFade = new Square(0.0, 0.0, screenWidth, screenHeight);
  }

  @Override
  public void run(double dt) {
    // Handle timers.
    if (beginTimer.getTime() < 3.0) beginTimer.count(dt);
    if (screenRumbleTimer.getTime() > 0.0) {
      screenRumbleTimer.countDown(dt*5);
      if (screenRumbleTimer.getTime() < 0.0) screenRumbleTimer.reset();
      screenTransformX = (float) ((Math.random() - 0.5) * 10.0 * screenRumbleTimer.getTime());
      screenTransformY = (float) ((Math.random() - 0.5) * 10.0 * screenRumbleTimer.getTime());
    }

    // Draw all drawable snakes.
    for (Player player : players) if (player != null && player.isDrawable()) player.updateColors();

    // Draw all apples.
    // TODO

    // Apply the transformation and draw the board.
    gl.glLoadIdentity();
    if (screenTransformX != 0.0 && screenTransformY != 0.0)
      gl.glTranslatef(screenTransformX, screenTransformY, 0f);
    this.drawBoard();

    // Update the square colors at every frame for each snake that is flashing.
    /* TODO I believe that if a snake goes over a flashing dead snake, the dead snake's flash will
    take priority over the alive one. This is not intended. */
    for(Player player : players) {
      if(player != null && !player.isAlive() && player.isFlashing()) {
        this.drawPlayerSnake(player);
      }
    }

    this.getBoardSquares().draw(gl);

    // Draw the display controllers.
    for (Player player : players) {
      // TODO Check if the player is local also.
      if (player != null && player.getControlType() == Player.ControlType.CORNER) {
        this.drawCornerLayout(player);
      }
    }

    // Draw countdown text.
    if (beginTimer.getTime() < 2.5) this.drawCountdownText(beginTimer);

    // Draw text for when the game is over.
    // TODO

    // Draw text for when the game is over which moves.
    // TODO Might do it with menuItems.
  }

  @Override
  public Player[] getPlayers() { return this.players; }

  public void handleInputBytes(byte[] inputBytes) {
    switch (inputBytes[0]) {
      case Protocol.GAME_MOVEMENT_OCCURED:
        break;
      case Protocol.GAME_MOVEMENT_INFORMATION:
        break;
      case Protocol.GAME_MOVEMENT_MISSING:
        break;
      default:
        break;
    }
  }
}
