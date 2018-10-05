package thusnake.snakemultiplayer;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.texample.GLText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 12/12/2017.
 */

// An instance of this class would be a single game of snake.
class Game extends BoardDrawer implements Activity {
  private boolean gameOver = false;
  private SimpleTimer beginTimer = new SimpleTimer(0.0, 3.0);
  private SimpleTimer moveTimer;
  private SimpleTimer screenRumbleTimer = new SimpleTimer(0.0);
  private SimpleTimer gameOverTimer = new SimpleTimer(0.0);
  private Player winner;
  private double speed;
  private float screenTransformX = 0f, screenTransformY = 0f;
  private SharedPreferences scores;
  private SharedPreferences.Editor scoresEditor;
  private final String scoreKey;
  private final MenuItem gameModeAnnouncement;
  final CornerMap cornerMap;
  private final List<Apple> apples = new LinkedList<>();
  private final Square[] boardLines = new Square[2];
  private final Square boardFade;
  private final GameRenderer renderer;
  private final GL10 gl;
  private final GLText glText;
  private final OpenGLES20Activity originActivity;

  private MenuItem gameOverTopItem, gameOverMiddleItem, gameOverBottomItem;
  private final List<MenuItem> gameOverItems = new ArrayList<>();

  // Constructor that sets up a local session.
  public Game(GameRenderer renderer, GameSetupBuffer setupBuffer) {
    super(renderer, (int) renderer.getScreenWidth(), (int) renderer.getScreenHeight());

    // Get the essentials.
    this.renderer = renderer;
    this.gl = renderer.getGl();
    this.glText = renderer.getGlText();
    this.originActivity = renderer.getOriginActivity();
    this.scores = originActivity.getSharedPreferences("scores", Context.MODE_PRIVATE);
    this.scoresEditor = scores.edit();
    scoreKey = setupBuffer.gameMode.toString() + "-" + setupBuffer.difficulty;

    // Get the options from the setup buffer.
    this.cornerMap = setupBuffer.getCornerMap();
    this.speed = setupBuffer.speed;

    this.moveTimer = new SimpleTimer(1.0 / speed);
    this.beginTimer.reset();
    this.gameOverTimer.reset();
    this.screenRumbleTimer.reset();
    this.gameOver = false;
    this.winner = null;

    // Prepare the players
    for (Player player : getPlayers())
      player.prepareForGame(this);

    // Create the apple.
    this.createApples();
    
    // Create the rest of the square objects.
    this.boardLines[0] = new Square(renderer, 0, getScreenHeight()/3f, getScreenWidth(), 4);
    this.boardLines[1] = new Square(renderer, 0, getScreenHeight()*2f/3f, getScreenWidth(), 4);
    this.boardFade = new Square(renderer, 0.0, 0.0, getScreenWidth(), getScreenHeight());
    gameModeAnnouncement = new MenuItem(renderer,
                          GameSetupBuffer.gameModeToString(setupBuffer.gameMode)
                              + " - " + GameSetupBuffer.difficultyToString(setupBuffer.difficulty),
                          -10, renderer.getScreenHeight() - 10, MenuDrawable.EdgePoint.TOP_RIGHT);
    gameModeAnnouncement
        .setAnimation(new MenuAnimation(gameModeAnnouncement)
          .addKeyframe(new MoveKeyframe(0.5, 10 + gameModeAnnouncement.getWidth(),
                                        gameModeAnnouncement.getY(), BezierTimer.easeOutBack))
          .addKeyframe(new Keyframe(2.0))
          .addKeyframe(new MoveKeyframe(0.5, -10, gameModeAnnouncement.getY(),
                                        BezierTimer.easeInBack)));

    this.gameOverTopItem = new MenuItem(renderer, isSingleplayer() ? "Try again" : "Rematch",
        -renderer.getScreenWidth() / 2,
        renderer.getScreenHeight() * 5 / 6 - glText.getHeight() / 2,
        MenuDrawable.EdgePoint.BOTTOM_CENTER) {
      @Override
      public boolean isClicked(float x, float y) {
        return y < 1/3f * renderer.getScreenHeight();
      }
    };
    this.gameOverTopItem.setAction((action, origin) -> action.restartGame(this));

    this.gameOverMiddleItem = new MenuItem(renderer, "Everyone loses",
        renderer.getScreenWidth() / 2,
        renderer.getScreenHeight() / 2 - glText.getHeight() / 2,
        MenuDrawable.EdgePoint.BOTTOM_CENTER) {
      @Override
      public boolean isClicked(float x, float y) {
        return y > 1/3f * renderer.getScreenHeight() && y < 2/3f * renderer.getScreenHeight();
      }
    };
    this.gameOverMiddleItem.setAction((action, origin) -> action.triggerStats());

    this.gameOverBottomItem = new MenuItem(renderer, "Menu",
        renderer.getScreenWidth() * 1.5f,
        renderer.getScreenHeight() * 1 / 6 - glText.getHeight() / 2,
        MenuDrawable.EdgePoint.BOTTOM_CENTER) {
      @Override
      public boolean isClicked(float x, float y) {
        return y > 2/3f * renderer.getScreenHeight();
      }
    };
    this.gameOverBottomItem.setAction((action, origin) -> action.quitGame());

    this.gameOverTopItem.setEaseOutVariables(5, 0.2);
    this.gameOverBottomItem.setEaseOutVariables(5, 0.2);

    this.gameOverItems.add(gameOverTopItem);
    this.gameOverItems.add(gameOverMiddleItem);
    this.gameOverItems.add(gameOverBottomItem);
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
    for (Player player : getPlayers())
      if (player != null && player.isDrawable())
        player.updateColors();

    // Draw all apples.
    for (Apple apple : apples)
      apple.updateColors();
    
    // Apply the transformation and draw the board.
    gl.glLoadIdentity();
    if (screenTransformX != 0.0 && screenTransformY != 0.0)
      gl.glTranslatef(screenTransformX, screenTransformY, 0f);
    this.drawBoard();

    // Update the square colors at every frame for each snake that is flashing.
    /* TODO I believe that if a snake goes over a flashing dead snake, the dead snake's flash will
    take priority over the alive one. This is not intended. */
    for(Player player : getPlayers()) {
      if(player != null && !player.isAlive() && player.isFlashing()) {
        this.drawPlayerSnake(player);
      }
    }

    this.getBoardSquares().draw();

    // Draw the display controllers.
    for (Player player : getPlayers()) {
      if (player != null && player.getPlayerController() != null) {
        this.drawControllerLayout(player);
      }
    }

    // Draw countdown text.
    if (beginTimer.getTime() < 2.5) this.drawCountdownText(beginTimer);
    gameModeAnnouncement.move(dt);
    gameModeAnnouncement.draw();

    // Draw text for when the game is over.
    if (gameOver) {
      gameOverTimer.countUp(dt);
      gl.glColor4f(0f, 0f, 0f, Math.min((float) gameOverTimer.getTime() * 0.375f, 0.75f));
      boardFade.draw(gl);

      if (isSingleplayer()) {
        gl.glPushMatrix();
        gl.glScalef(0.25f, 0.25f, 1f);
        glText.begin();
        glText.drawC("Score: " + getPlayers().get(0).getScore(), this.getScreenWidth() * 2f,
            this.getScreenHeight() / 2f * 4 + glText.getHeight() * (- 2f));
        glText.drawC("High Score: " + scores.getInt("high_score_" + scoreKey, 0),
            this.getScreenWidth() * 2,
            this.getScreenHeight() / 2f * 4 + glText.getHeight() * 2f);
        glText.end();
        gl.glPopMatrix();
      }

      // Move and draw all the game over items.
      for (MenuItem item : getGameOverItems()) {
        item.move(dt);
        item.draw();
      }
    }

    // Draw text for when the game is over which moves.
    gl.glPushMatrix();
    gl.glTranslatef(this.getScreenWidth() - Math.min((float) gameOverTimer.getTime(), 1f)
        * this.getScreenWidth(), 0, 0);
    boardLines[0].draw(gl);
    gl.glPopMatrix();
    gl.glPushMatrix();
    gl.glTranslatef(
        -this.getScreenWidth() + Math.min((float) gameOverTimer.getTime(), 1f) * this.getScreenWidth(),
        0, 0);
    boardLines[1].draw(gl);
    gl.glPopMatrix();
  }

  public void refresh() {
    for (Player player : getPlayers())
      player.getPlayerController().reloadTexture();
  }

  public void createApples() {
    apples.add(new Apple(this));
  }

  protected boolean checkGameOver() {
    if ((!isSingleplayer() && this.getAlivePlayers() <= 1)
        || (isSingleplayer() && this.getAlivePlayers() == 0)
        && !gameOver) {
      gameOver = true;

      if (!isSingleplayer()) {
        winner = this.assessWinner();
      } else {
        if (getPlayers().get(0).getScore() > scores.getInt("high_score_" + scoreKey, 0)) {
          scoresEditor.putInt("high_score_" + scoreKey, getPlayers().get(0).getScore());
          scoresEditor.commit();
        }
      }

      gameOverTopItem.setDestinationX(getScreenWidth() / 2);
      gameOverBottomItem.setDestinationX(getScreenWidth() / 2);

      if (isSingleplayer())
        gameOverMiddleItem.setText("Game over");
      else {
        if (winner == null)
          gameOverMiddleItem.setText("It's a draw!");
        else {
          float[] colors = winner.getSkin().headColors();
          gameOverMiddleItem.setColors(colors[0], colors[1], colors[2], colors[3]);
          gameOverMiddleItem.setText(winner.getName() + " wins!");
        }
      }

      return true;
    }
    return false;
  }

  protected void moveAllSnakes() {
    for (Player player : getPlayers()) {
      // Move and check if it has eaten the apple.
      if (player != null && player.isAlive() && player.move()) {
        for (Apple apple : apples)
          if (apple.check(player))
            this.getBoardSquares().updateColors(apple.x, apple.y, apple.getColors());
      }
    }
    for (Player player : getPlayers())
      if(player != null && player.isAlive()) player.checkDeath();
  }

  protected void sendBytes(byte[] bytes) {
    for(ConnectedThread connectedThread : originActivity.connectedThreads) {
      if (connectedThread != null) connectedThread.write(bytes);
    }
  }

  // Methods for overriding purposes.
  public void handleInputBytes(byte[] inputBytes, ConnectedThread sourceThread) {}
  public void onAppleEaten(Apple apple) {}

  // Getters.
  public GameRenderer getRenderer() { return this.renderer; }
  public double getSpeed() { return this.speed; }
  public boolean isOver() { return this.gameOver; }
  public List<Apple> getApples() { return this.apples; }
  public List<Player> getPlayers() { return cornerMap.getPlayers(); }
  public int getAlivePlayers() {
    int playersAlive = 0;
    for (Player player : getPlayers()) {
      if (player != null && player.isAlive()) playersAlive++;
    }
    return playersAlive;
  }

  public boolean isSingleplayer() { return cornerMap.getNumberOfPlayers() == 1; }
  public Player assessWinner() {
    for (Player player : getPlayers())
      if (player != null && player.isAlive()) 
        return player;
    
    // If no players are alive it returns null.
    return null;
  }
  public SimpleTimer getGameOverTimer() { return this.gameOverTimer; }

  public List<MenuItem> getGameOverItems() { return this.gameOverItems; }
  public MenuItem getGameOverTopItem() { return this.gameOverTopItem; }
  public MenuItem getGameOverMiddleItem() { return this.gameOverMiddleItem; }
  public MenuItem getGameOverBottomItem() { return this.gameOverBottomItem; }
}
