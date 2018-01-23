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
class Game {
  private boolean gameOver = false, onlineSession, screenBorders;
  private SimpleTimer beginTimer = new SimpleTimer(0.0, 3.0);
  private SimpleTimer moveTimer;
  private SimpleTimer screenRumbleTimer = new SimpleTimer(0.0);
  private SimpleTimer gameOverTimer = new SimpleTimer(0.0);
  private enum GameMode {SINGLEPLAYER, MULTIPLAYER};
  private GameMode gameMode;
  private int winner;
  private double speed;
  private double screenTransformX = 0.0, screenTransformY = 0.0;
  private SharedPreferences scores;
  private SharedPreferences.Editor scoresEditor;
  private final Player[] players;
  private final Apple apple;
  private final int horizontalSquares, verticalSquares;
  private final Mesh boardSquares = new Mesh(this);
  private final double boardOffsetX, boardOffsetY, screenWidth, screenHeight, squareWidth;
  private final Square boardOutline, boardFill, boardFade;
  private final Square[] boardLines = new Square[2];
  private final GameRenderer renderer;
  private final GL10 gl;
  private final GLText glText;
  private final Context context;
  private final float[] boardSquareColors = {0.125f, 0.125f, 0.125f, 1.0f};

  // Constructor that sets up a local session.
  public Game(GameRenderer renderer, int screenWidth, int screenHeight) {
    onlineSession = false;

    // Get the essentials.
    this.renderer = renderer;
    this.gl = renderer.getGl();
    this.glText = renderer.getGlText();
    this.context = renderer.getContext();
    this.scores = context.getSharedPreferences("scores", Context.MODE_PRIVATE);
    this.scoresEditor = scores.edit();

    // Get the options from the menu.
    this.horizontalSquares = renderer.getMenu().horizontalSquares;
    this.verticalSquares = renderer.getMenu().verticalSquares;
    this.speed = renderer.getMenu().speed;
    this.screenBorders = renderer.getMenu().stageBorders;

    // TODO Calculate board offset.
    this.boardOffsetY = 10.0;
    this.boardOffsetX = (screenWidth - ((screenHeight - 20f) / this.verticalSquares) * horizontalSquares) / 2f;
    this.screenWidth = screenWidth;
    this.screenHeight = screenHeight;
    this.squareWidth = (screenHeight - 20.0 - this.verticalSquares - 1.0) / verticalSquares;
    this.screenTransformX = 0.0;
    this.screenTransformY = 0.0;
    // Apply offset to some Squares.
    boardOutline = new Square(boardOffsetX - 1.0, boardOffsetY - 1.0,
        screenWidth - boardOffsetX*2 + 2.0, screenHeight - boardOffsetY*2 + 2.0);
    boardFill = new Square(boardOffsetX, boardOffsetY,
        screenWidth - boardOffsetX*2, screenHeight - boardOffsetY*2);
    boardFade = new Square(0.0, 0.0, screenWidth, screenHeight);

    this.moveTimer = new SimpleTimer(1.0 / speed);
    this.beginTimer.reset();
    this.gameOverTimer.reset();
    this.screenRumbleTimer.reset();
    this.gameOver = false;
    this.winner = -1;

    int playersToCreate = 0;
    for (Player.ControlType controlType : this.renderer.getMenu().playerControlType)
      if (controlType != Player.ControlType.OFF) playersToCreate++;
    if (playersToCreate == 0) this.renderer.quitGame();
    else if (playersToCreate == 1) this.gameMode = GameMode.SINGLEPLAYER;
    else this.gameMode = GameMode.MULTIPLAYER;

    // Create the players
    if (gameMode == GameMode.SINGLEPLAYER) {
      players = new Player[1];
      players[0] = new Player(this, 0);
    } else {
      this.players = new Player[playersToCreate];
      for (int index = 0; index < this.players.length; index++)
        players[index] = new Player(this, index);
    }

    // Create the board mesh, the apple(s) and other objects.
    for (int y = 0; y < this.verticalSquares; y++) {
      for (int x = 0; x < this.horizontalSquares; x++) {
        this.boardSquares.addSquare(this.boardOffsetX + this.squareWidth * x + x + 1,
            this.boardOffsetY + y + 1 + this.squareWidth * y,
            this.squareWidth, this.squareWidth);
      }
    }
    this.boardSquares.applySquares();
    this.apple = new Apple(this);
    this.boardLines[0] = new Square(0, screenHeight/3f, screenWidth, 4);
    this.boardLines[1] = new Square(0, screenHeight*2f/3f, screenWidth, 4);

    this.gl.glEnable(GL10.GL_TEXTURE_2D);
    this.gl.glEnable(GL10.GL_BLEND);
    this.gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
  }

  // Constructor that sets up a online game.
  // TODO

  // Runs a single frame of the snake game.
  public void run(double dt) {
    // Check if the game is over.
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
      if (onlineSession) {
        byte[] bytesToSend = {6,1,(byte)winner};
        this.sendBytes(bytesToSend);
      }
    }

    // Handle timers.
    if (!gameOver && beginTimer.getTime() > 2.0) moveTimer.countDown(dt);
    if (beginTimer.getTime() < 3.0) beginTimer.count(dt);
    if (screenRumbleTimer.getTime() > 0.0) {
      screenRumbleTimer.countDown(dt*5);
      if (screenRumbleTimer.getTime() < 0.0) screenRumbleTimer.reset();
      screenTransformX = (Math.random() - 0.5) * 10.0 * screenRumbleTimer.getTime();
      screenTransformY = (Math.random() - 0.5) * 10.0 * screenRumbleTimer.getTime();
    }

    // Handle movement based on moveTimer.
    while(!gameOver && moveTimer.getTime() <= 0.0) {
      moveTimer.countUp(1.0 / speed);
      for (Player player : players) {
        // Move and check if it has eaten the apple.
        if (player.isAlive() && player.move()) {
          if (apple.check(player))
            boardSquares.updateColors(apple.x, apple.y, apple.getColors());
        }
      }
      for (Player player : players)
        if(player.isAlive()) player.checkDeath();
      // Online play
      if (onlineSession) {
        // TODO send bytes type 9
      }
    }

    // Draw all drawable snakes.
    for (Player player : players) if (player.isDrawable()) player.updateColors();

    // Draw all apples.
    apple.updateColors();

    // Update the boardSquares and draw them.
    if (screenTransformX != 0.0 && screenTransformY != 0.0)
      gl.glTranslatef((float)screenTransformX, (float)screenTransformY, 0f);
    gl.glColor4f(1f, 1f, 1f, 1f);
    boardOutline.draw(gl);
    gl.glColor4f(0f, 0f, 0f, 1f);
    boardFill.draw(gl);

    // Update the square colors at every frame for each snake that is flashing.
    /* TODO I believe that if a snake goes over a flashing dead snake, the dead snake's flash will
    take priority over the alive one. This is not intended. */
    for(Player player : players) {
      if(!player.isAlive() && player.isFlashing()) {
        for (BodyPart bodyPart : player.getBodyParts()) {
          if (!bodyPart.isOutOfBounds() && !player.isDrawable())
            boardSquares.updateColors(bodyPart.getX(), bodyPart.getY(), boardSquareColors);
          else if (!bodyPart.isOutOfBounds())
            boardSquares.updateColors(bodyPart.getX(), bodyPart.getY(), bodyPart.getColors());
        }
      }
    }

    boardSquares.draw(gl);

    // Draw the display controllers.
    for (Player player : players) {
      if (player.getControlType() == Player.ControlType.CORNER) {
        gl.glColor4f(player.getColors()[0], player.getColors()[1],
                     player.getColors()[2], player.getColors()[3]);
        // For some unknown reason I can not load the texture inside the constructor properly.
        // This is my band-aid fix.
        if (!player.getCornerLayout().textureIsLoaded())
          player.getCornerLayout().loadGLTexture(this.context);
        player.getCornerLayout().draw(gl);
      }
    }

    // Draw countdown text.
    if (beginTimer.getTime() < 2.5) {
      gl.glPushMatrix();
      gl.glScalef(2f, 2f, 1f);
      gl.glTranslatef(-(float)screenWidth / 4f, -(float)screenHeight / 4f, 0f);
      glText.begin(1f, 1f, 1f, 1f);
      String countdownText;
      if (beginTimer.getTime() < 2.0) countdownText =
          Integer.toString(3 - (int)(beginTimer.getTime() * 3.0 / 2.0));
      else countdownText = "Go!";
      glText.draw(countdownText, (float)(screenWidth - glText.getLength(countdownText)) / 2f,
          (float)(screenHeight - glText.getCharHeight()) / 2f);
      glText.end();
      gl.glPopMatrix();
    }

    // Draw text for when the game is over.
    if (gameOver) {
      gameOverTimer.countUp(dt);
      gl.glColor4f(0f, 0f, 0f, Math.min((float) gameOverTimer.getTime() * 0.375f, 0.75f));
      boardFade.draw(gl);

      glText.begin(1f, 1f, 1f, 1f);
      if (gameMode == GameMode.SINGLEPLAYER) {
        glText.drawC("Game Over", (float) screenWidth / 2f,
            (float) screenHeight / 2f + glText.getCharHeight() * 0.077f);
        glText.end();
        gl.glPushMatrix();
        gl.glScalef(0.25f, 0.25f, 1f);
        glText.begin();
        glText.drawC("Score: " + players[0].getScore(), (float) screenWidth * 2f,
            (float) screenHeight / 2f * 4 + glText.getCharHeight() * (0.077f - 2f));
        glText.drawC("High Score: " + scores.getInt("high_score_classic", 0),
            (float)screenWidth * 2, (float)screenHeight/2 * 4 + glText.getCharHeight() * 2.077f);
        glText.end();
        gl.glPopMatrix();
        glText.begin();
      } else {
        if (winner == -1) {
          glText.drawC("It's a draw!", (float) screenWidth / 2f,
              (float) screenHeight / 2f + glText.getCharHeight() * 0.077f);
        } else {
          glText.end();
          glText.begin(players[winner].getColors()[0], players[winner].getColors()[1],
              players[winner].getColors()[2], players[winner].getColors()[3]);
          glText.drawC(players[winner].getName() + " wins!", (float) screenWidth / 2f,
              (float) screenHeight / 2f + glText.getCharHeight() * 0.077f);
          glText.end();
          glText.begin();
        }
      }
      glText.end();
    }

    // Draw text for when the game is over which moves.
    gl.glPushMatrix();
    gl.glTranslatef((float) screenWidth - Math.min((float) gameOverTimer.getTime(), 1f)
        * (float) screenWidth, 0, 0);
    boardLines[0].draw(gl);
    glText.begin();
    glText.drawC((gameMode == GameMode.SINGLEPLAYER) ? "Try Again" : "Rematch",
        (float) screenWidth / 2f, (float) screenHeight * 5f / 6f + glText.getCharHeight() * 0.077f);
    glText.end();
    gl.glPopMatrix();
    gl.glPushMatrix();
    gl.glTranslatef(
        (float) -screenWidth + Math.min((float) gameOverTimer.getTime(), 1f) * (float) screenWidth,
        0, 0);
    boardLines[1].draw(gl);
    glText.begin();
    glText.drawC("Menu", (float) screenWidth / 2f,
        (float) screenHeight * 1f / 6f + glText.getCharHeight() * 0.077f);
    glText.end();
    gl.glPopMatrix();
  }

  private void sendBytes(byte[] bytes) {
    for(ConnectedThread connectedThread : OpenGLES20Activity.cnctdThreads) {
      if (connectedThread != null) connectedThread.write(bytes);
    }
  }

  public GameRenderer getRenderer() { return this.renderer; }
  public int getHorizontalSquares() { return this.horizontalSquares; }
  public int getVerticalSquares() { return this.verticalSquares; }
  public GameMode getGameMode() { return this.gameMode; }
  public boolean isOver() { return this.gameOver; }
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
  public int getPlayersPlaying() { return this.players.length; }
  public Mesh getBoardSquares() { return this.boardSquares; }
  public float[] getBoardSquareColors() { return this.boardSquareColors; }
  public SimpleTimer getGameOverTimer() { return this.gameOverTimer; }
}
