package thusnake.snakemultiplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLES11;

import com.android.texample.GLText;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 12/12/2017.
 */

// An instance of this class would be a single game of snake.
class Game extends BoardDrawer {
  private boolean gameOver = false;
  private SimpleTimer beginTimer = new SimpleTimer(0.0, 3.0);
  private SimpleTimer moveTimer;
  private SimpleTimer screenRumbleTimer = new SimpleTimer(0.0);
  private SimpleTimer gameOverTimer = new SimpleTimer(0.0);
  private int moveCount = 0;
  private enum GameMode {SINGLEPLAYER, MULTIPLAYER};
  private GameMode gameMode;
  private int winner;
  private double speed;
  private float screenTransformX = 0f, screenTransformY = 0f;
  private SharedPreferences scores;
  private SharedPreferences.Editor scoresEditor;
  private final Player[] players;
  private int playersPlaying;
  private final Apple apple;
  private final Square[] boardLines = new Square[2];
  private final Square boardFade;
  private final GameRenderer renderer;
  private final GL10 gl;
  private final GLText glText;
  private final Context context;
  private final OpenGLES20Activity originActivity;

  // Constructor that sets up a local session.
  public Game(GameRenderer renderer, int screenWidth, int screenHeight, Player[] players) {
    super(renderer, screenWidth, screenHeight);
    
    // Get the essentials.
    this.renderer = renderer;
    this.gl = renderer.getGl();
    this.glText = renderer.getGlText();
    this.context = renderer.getContext();
    this.originActivity = (OpenGLES20Activity) context;
    this.scores = context.getSharedPreferences("scores", Context.MODE_PRIVATE);
    this.scoresEditor = scores.edit();

    // Get the options from the menu.
    this.speed = renderer.getMenu().speed;

    this.moveTimer = new SimpleTimer(1.0 / speed);
    this.beginTimer.reset();
    this.gameOverTimer.reset();
    this.screenRumbleTimer.reset();
    this.gameOver = false;
    this.winner = -1;

    int playersToCreate = 0;
    for (Player player : players)
      if (player != null && player.getControlType() != Player.ControlType.OFF) playersToCreate++;
    if (playersToCreate == 0) this.renderer.quitGame();
    else if (playersToCreate == 1) this.gameMode = GameMode.SINGLEPLAYER;
    else this.gameMode = GameMode.MULTIPLAYER;

    // Prepare the players
    this.players = players;
    if (gameMode == GameMode.SINGLEPLAYER) {
      this.players[0].prepareForGame(this, 0);
    } else {
      for (int index = 0; index < playersToCreate; index++) {
        if (this.players[index] == null) {
          this.playersPlaying = index;
          break;
        }
        this.players[index].prepareForGame(this, index);
      }
    }

    // Create the apple.
    this.apple = new Apple(this);
    
    // Create the rest of the square objects.
    this.boardLines[0] = new Square(0, screenHeight/3f, screenWidth, 4);
    this.boardLines[1] = new Square(0, screenHeight*2f/3f, screenWidth, 4);
    this.boardFade = new Square(0.0, 0.0, screenWidth, screenHeight);
  }

  // Runs a single frame of the snake game.
  @Override
  public void run(double dt) {
    // Check if the game is over.
    checkGameOver();

    // Handle timers.
    if (!gameOver && beginTimer.getTime() > 2.0) moveTimer.countDown(dt);
    if (beginTimer.getTime() < 3.0) beginTimer.count(dt);
    if (screenRumbleTimer.getTime() > 0.0) {
      screenRumbleTimer.countDown(dt*5);
      if (screenRumbleTimer.getTime() < 0.0) screenRumbleTimer.reset();
      screenTransformX = (float) ((Math.random() - 0.5) * 10.0 * screenRumbleTimer.getTime());
      screenTransformY = (float) ((Math.random() - 0.5) * 10.0 * screenRumbleTimer.getTime());
    }

    // Handle movement based on moveTimer.
    while(!gameOver && moveTimer.getTime() <= 0.0) {
      moveTimer.countUp(1.0 / speed);
      moveAllSnakes();
    }

    // Draw all drawable snakes.
    for (Player player : players) if (player != null && player.isDrawable()) player.updateColors();

    // Draw all apples.
    apple.updateColors();
    
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
      if (player != null && player.getControlType() == Player.ControlType.CORNER) {
        this.drawCornerLayout(player);
      }
    }

    // Draw countdown text.
    if (beginTimer.getTime() < 2.5) this.drawCountdownText(beginTimer);

    // Draw text for when the game is over.
    if (gameOver) {
      gameOverTimer.countUp(dt);
      gl.glColor4f(0f, 0f, 0f, Math.min((float) gameOverTimer.getTime() * 0.375f, 0.75f));
      boardFade.draw(gl);

      glText.begin(1f, 1f, 1f, 1f);
      if (gameMode == GameMode.SINGLEPLAYER) {
        glText.drawC("Game Over", this.getScreenWidth() / 2f,
            this.getScreenHeight() / 2f + glText.getCharHeight() * 0.077f);
        glText.end();
        gl.glPushMatrix();
        gl.glScalef(0.25f, 0.25f, 1f);
        glText.begin();
        glText.drawC("Score: " + players[0].getScore(), this.getScreenWidth() * 2f,
            this.getScreenHeight() / 2f * 4 + glText.getCharHeight() * (0.077f - 2f));
        glText.drawC("High Score: " + scores.getInt("high_score_classic", 0),
            this.getScreenWidth() * 2,
            this.getScreenHeight()/2 * 4 + glText.getCharHeight() * 2.077f);
        glText.end();
        gl.glPopMatrix();
        glText.begin();
      } else {
        if (winner == -1) {
          glText.drawC("It's a draw!", this.getScreenWidth() / 2f,
              this.getScreenHeight() / 2f + glText.getCharHeight() * 0.077f);
        } else {
          glText.end();
          glText.begin(players[winner].getColors()[0], players[winner].getColors()[1],
              players[winner].getColors()[2], players[winner].getColors()[3]);
          glText.drawC(players[winner].getName() + " wins!", this.getScreenWidth() / 2f,
              this.getScreenHeight() / 2f + glText.getCharHeight() * 0.077f);
          glText.end();
          glText.begin();
        }
      }
      glText.end();
    }

    // Draw text for when the game is over which moves.
    gl.glPushMatrix();
    gl.glTranslatef(this.getScreenWidth() - Math.min((float) gameOverTimer.getTime(), 1f)
        * this.getScreenWidth(), 0, 0);
    boardLines[0].draw(gl);
    glText.begin();
    glText.drawC((gameMode == GameMode.SINGLEPLAYER) ? "Try Again" : "Rematch",
        this.getScreenWidth() / 2f, this.getScreenHeight() * 5f / 6f + glText.getCharHeight() * 0.077f);
    glText.end();
    gl.glPopMatrix();
    gl.glPushMatrix();
    gl.glTranslatef(
        -this.getScreenWidth() + Math.min((float) gameOverTimer.getTime(), 1f) * this.getScreenWidth(),
        0, 0);
    boardLines[1].draw(gl);
    glText.begin();
    glText.drawC("Menu", this.getScreenWidth() / 2f,
        this.getScreenHeight() * 1f / 6f + glText.getCharHeight() * 0.077f);
    glText.end();
    gl.glPopMatrix();
  }

  protected boolean checkGameOver() {
    if ((gameMode == GameMode.MULTIPLAYER && this.getAlivePlayers() <= 1)
        || (gameMode == GameMode.SINGLEPLAYER && this.getAlivePlayers() == 0)
        && !gameOver) {
      gameOver = true;
      if (gameMode == GameMode.MULTIPLAYER) {
        winner = this.assessWinner();
      } else if (gameMode == GameMode.SINGLEPLAYER) {
        if (players[0].getScore() > scores.getInt("high_score_classic", 0)) {
          scoresEditor.putInt("high_score_classic", players[0].getScore());
          scoresEditor.commit();
        }
      }
      return true;
    }
    return false;
  }

  protected void moveAllSnakes() {
    moveCount++;
    for (Player player : players) {
      // Move and check if it has eaten the apple.
      if (player != null && player.isAlive() && player.move()) {
        if (apple.check(player))
          this.getBoardSquares().updateColors(apple.x, apple.y, apple.getColors());
      }
    }
    for (Player player : players)
      if(player != null && player.isAlive()) player.checkDeath();
  }

  protected void sendBytes(byte[] bytes) {
    for(ConnectedThread connectedThread : originActivity.connectedThreads) {
      if (connectedThread != null) connectedThread.write(bytes);
    }
  }

  public void handleInputBytes(byte[] inputBytes) {

  }

  public GameRenderer getRenderer() { return this.renderer; }
  public GameMode getGameMode() { return this.gameMode; }
  public boolean isOver() { return this.gameOver; }
  public int getMoveCount() { return this.moveCount; }
  public Player[] getPlayers() { return this.players; }
  public int getAlivePlayers() {
    int playersAlive = 0;
    for (Player player : players) {
      if (player != null && player.isAlive()) playersAlive++;
    }
    return playersAlive;
  }
  public int getPlayingLocal() {
    int localPlayers = 0;
    for (Player player : players) {
      if (player != null
          && player.getControlType() != Player.ControlType.BLUETOOTH
          && player.getControlType() != Player.ControlType.WIFI) {
        localPlayers++;
      }
    }
    return localPlayers;
  }
  public int assessWinner() {
    for (Player player : players) {
      if (player != null && player.isAlive()) return player.getNumber();
    }
    // If no players are alive it returns -1.
    return -1;
  }
  public int getPlayersPlaying() { return this.playersPlaying; }
  public SimpleTimer getGameOverTimer() { return this.gameOverTimer; }
}
