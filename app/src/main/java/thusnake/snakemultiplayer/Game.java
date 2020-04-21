package thusnake.snakemultiplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.util.ArraySet;

import com.android.texample.GLText;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.microedition.khronos.opengles.GL10;

import thusnake.snakemultiplayer.controllers.ControllerBuffer;
import thusnake.snakemultiplayer.netplay.ConnectedThread;
import thusnake.snakemultiplayer.textures.GameTextureMap;

/**
 * Created by Nick on 12/12/2017.
 */

// An instance of this class would be a single game of snake.
public class Game extends BoardDrawer implements Activity {
  private boolean gameOver = false, paused = false;
  private SimpleTimer beginTimer = new SimpleTimer(0.0, 3.0), moveTimer,
      screenRumbleTimer = new SimpleTimer(0.0), gameOverTimer = new SimpleTimer(0.0);
  private Snake winner;
  private double speed;
  private float screenTransformX = 0f, screenTransformY = 0f;
  private SharedPreferences scores;
  private String scoreKey;
  private MenuItem gameModeAnnouncement;
  private final CornerMap cornerMap;
  private final List<Snake> snakes = new LinkedList<>();
  private final List<Entity> entities = new LinkedList<>();
  private final Square[] boardLines = new Square[2];
  private final Square boardFade;
  private final GameRenderer renderer;
  private final GL10 gl;
  private final GLText glText;
  private final GameTextureMap textureMap;
  private final OpenGLActivity originActivity;
  private final Set<GameListener> listeners = new ArraySet<>();
  private final long randSeed;
  private final Random rng;

  private MenuItem gameOverTopItem, gameOverMiddleItem, gameOverBottomItem;
  private final List<MenuItem> gameOverItems = new ArrayList<>();

  // Constructor that sets up a local session.
  public Game(CornerMap cornerMap, int horizontalSquares, int verticalSquares, int speed,
              boolean stageBorders, List<Class<? extends Entity>> entityBlueprints,
              Set<GameListener> listeners) {
    super(horizontalSquares, verticalSquares, stageBorders);
    this.speed = speed;

    // Get the essentials.
    originActivity = OpenGLActivity.current;
    renderer = originActivity.getRenderer();
    gl = renderer.getGl();
    glText = renderer.getGlText();

    moveTimer = new SimpleTimer(1.0 / speed);
    beginTimer.reset();
    gameOverTimer.reset();
    screenRumbleTimer.reset();
    gameOver = false;
    winner = null;
    randSeed = System.nanoTime() * 31;
    rng = new Random(randSeed);

    // Register the listeners.
    if (listeners != null)
      for (GameListener listener : listeners) registerListener(listener);

    // Create the snakes for each player.
    this.cornerMap = cornerMap;
    for (Player player : getPlayers()) {
      Snake snake = new Snake(this, player);
      for (GameListener listener : this.listeners) listener.onSnakeCreation(snake);
      snakes.add(snake);
    }

    // Create the entities from the blueprints.
    for (Class<? extends Entity> entityBlueprint : entityBlueprints) {
      try {
        entities.add(entityBlueprint.getConstructor(Game.class).newInstance(this));
      }
      catch (Exception e) {
        System.err.println("Couldn't create an instance of " + entityBlueprint.toString());
        System.err.println(e.toString());
      }
    }

    // Create the texture map. Initialize the board squares with it.
    textureMap = new GameTextureMap(snakes, entities);
    generateMesh(textureMap);
    
    // Create the rest of the square objects.
    boardLines[0] = new Square(renderer, 0, getScreenHeight()/3f, getScreenWidth(), 4);
    boardLines[1] = new Square(renderer, 0, getScreenHeight()*2f/3f, getScreenWidth(), 4);
    boardFade = new Square(renderer, 0.0, 0.0, getScreenWidth(), getScreenHeight());

    gameOverTopItem = new MenuItem(renderer, isSingleplayer() ? "Try again" : "Rematch",
                                   -renderer.getScreenWidth() / 2,
                                   renderer.getScreenHeight() * 5 / 6 - glText.getHeight() / 2,
                                   MenuDrawable.EdgePoint.BOTTOM_CENTER) {
      @Override
      public boolean isClicked(float x, float y) {
        return y < 1/3f * renderer.getScreenHeight();
      }
    }.addTextShadow();
    gameOverTopItem.setAction((action, origin) -> action.restartGame(this));

    gameOverMiddleItem = new MenuItem(renderer, "Everyone loses",
                                      renderer.getScreenWidth() / 2,
                                      renderer.getScreenHeight() / 2 - glText.getHeight() / 2,
                                      MenuDrawable.EdgePoint.BOTTOM_CENTER) {
      @Override
      public boolean isClicked(float x, float y) {
        return y > 1/3f * renderer.getScreenHeight() && y < 2/3f * renderer.getScreenHeight();
      }
    }.addTextShadow();
    gameOverMiddleItem.setAction((action, origin) -> action.triggerStats());

    gameOverBottomItem = new MenuItem(renderer, "Menu",
                                      renderer.getScreenWidth() * 1.5f,
                                      renderer.getScreenHeight() * 1 / 6 - glText.getHeight() / 2,
                                      MenuDrawable.EdgePoint.BOTTOM_CENTER) {
      @Override
      public boolean isClicked(float x, float y) {
        return y > 2/3f * renderer.getScreenHeight();
      }
    }.addTextShadow();
    gameOverBottomItem.setAction((action, origin) -> action.quitGame());

    gameOverTopItem.setEaseOutVariables(5, 0.2);
    gameOverBottomItem.setEaseOutVariables(5, 0.2);

    gameOverItems.add(gameOverTopItem);
    gameOverItems.add(gameOverMiddleItem);
    gameOverItems.add(gameOverBottomItem);

    // Draw everything to the mesh.
    for (Snake snake : snakes)
      snake.drawToMesh();
    for (Entity entity : getEntities())
      entity.drawToMesh();

    for (GameListener listener : this.listeners) listener.onGameCreated();
  }

  public Game(CornerMap cornerMap, int horizontalSquares, int verticalSquares, int speed,
              boolean stageBorders, List<Class<? extends Entity>> entityBlueprints) {
    this(cornerMap, horizontalSquares, verticalSquares, speed, stageBorders, entityBlueprints,
         null);
  }

  /**
   * Enables score saving and sets the key for it.
   * Used only for singleplayer, thus the difficulty input variable.
   * @param gameModeName The name of the game mode.
   * @param difficulty The difficulty of the game mode.
   */
  public Game withScoreSaveKey(String gameModeName, int difficulty) {
    scores = originActivity.getSharedPreferences("scores", Context.MODE_PRIVATE);
    scoreKey = gameModeName + "-" + difficulty;

    gameModeAnnouncement
        = new MenuItem(renderer,
                       gameModeName + " - " + GameSetupBuffer.difficultyToString(difficulty),
                       -10, renderer.getScreenHeight() - 10, MenuDrawable.EdgePoint.TOP_RIGHT)
        .addTextShadow();

    gameModeAnnouncement
        .setAnimation(new MenuAnimation(gameModeAnnouncement)
            .addKeyframe(new MoveKeyframe(0.5, 10 + gameModeAnnouncement.getWidth(),
                gameModeAnnouncement.getY(), BezierTimer.easeOutBack))
            .addKeyframe(new Keyframe(2.0))
            .addKeyframe(new MoveKeyframe(0.5, -10, gameModeAnnouncement.getY(),
                BezierTimer.easeInBack)));

    return this;
  }

  /** @param entities A list of entities to be added to the game. */
  public void addEntities(List<Entity> entities) {
    for (Entity entity : entities) addEntity(entity);
  }

  /** @param entity An entity to be added to the game. */
  public void addEntity(Entity entity) {
    for (GameListener listener : listeners) listener.onEntityCreation(entity);
    entities.add(entity);
  }

  /**
   * Runs a single frame of the game.
   * @param dt The time difference (in seconds) between this frame and the last frame.
   */
  @Override
  public void run(double dt) {
    if (!paused) {
      // Check if the game is over.
      checkGameOver();

      // Handle timers.
      if (!gameOver && beginTimer.getTime() > 2.0) moveTimer.countDown(dt);
      if (beginTimer.getTime() < 3.0) beginTimer.count(dt);
      if (screenRumbleTimer.getTime() > 0.0) {
        screenRumbleTimer.countDown(dt * 5);
        if (screenRumbleTimer.getTime() < 0.0) screenRumbleTimer.reset();
        screenTransformX = (float) ((Math.random() - 0.5) * 10.0 * screenRumbleTimer.getTime());
        screenTransformY = (float) ((Math.random() - 0.5) * 10.0 * screenRumbleTimer.getTime());
      }

      // Handle movement based on moveTimer.
      while (!gameOver && moveTimer.getTime() <= 0.0) {
        moveTimer.countUp(1.0 / speed);
        onStep();
      }

      // Apply the transformation and draw the board.
      gl.glLoadIdentity();
      if (screenTransformX != 0.0 && screenTransformY != 0.0)
        gl.glTranslatef(screenTransformX, screenTransformY, 0f);
      this.drawBoard();

      // Update the square colors at every frame for each snake that is flashing.
      /* TODO I believe that if a snake goes over a flashing dead snake, the dead snake's flash will take priority over the alive one. This is not intended. */
      for (Snake snake : snakes) {
        if (snake != null && !snake.isAlive() && snake.isFlashing()) {
          snake.drawToMesh();
        }
      }

      this.getBoardSquares().draw();

      // Draw the display controllers.
      for (Snake snake : snakes) {
        this.drawControllerLayout(snake);
      }

      // Draw countdown text.
      if (beginTimer.getTime() < 2.5) this.drawCountdownText(beginTimer);
      if (gameModeAnnouncement != null) {
        gameModeAnnouncement.move(dt);
        gameModeAnnouncement.draw();
      }

      // Draw text for when the game is over.
      if (gameOver) {
        gameOverTimer.countUp(dt);
        gl.glColor4f(0f, 0f, 0f, Math.min((float) gameOverTimer.getTime() * 0.375f, 0.75f));
        boardFade.draw(gl);

        if (isSingleplayer()) {
          gl.glPushMatrix();
          gl.glScalef(0.25f, 0.25f, 1f);
          glText.begin();
          glText.drawC("Score: " + snakes.get(0).getScore(), this.getScreenWidth() * 2f,
              this.getScreenHeight() / 2f * 4 + glText.getHeight() * (-2f));
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
  }

  public void setPaused(boolean paused) { this.paused = paused; }

  public void refresh() {
    for (Snake snake : snakes)
      snake.controller.reloadTexture();
  }

  /** Called every time a move has to be executed, which is determined by the speed of the game. */
  public void onStep() {
    performMove();
  }

  protected boolean checkGameOver() {
    if ((!isSingleplayer() && getAliveSnakes().size() <= 1)
        || (isSingleplayer() && getAliveSnakes().size() == 0)
        && !gameOver) {

      // Declare game over.
      gameOver = true;
      for (GameListener listener : listeners) listener.onGameOver();

      if (!isSingleplayer()) {
        winner = this.assessWinner();
      } else {
        if (snakes.get(0).getScore() > scores.getInt("high_score_" + scoreKey, 0)) {
          SharedPreferences.Editor scoresEditor = scores.edit();
          scoresEditor.putInt("high_score_" + scoreKey, snakes.get(0).getScore());
          scoresEditor.apply();
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

  /**
   * Updates the positions of all snakes and entities and then redraws them onto the Mesh.
   * Usually called on every game step.
   */
  protected void performMove() {
    for (GameListener listener : listeners) listener.beforeMoveExecuted();

    // Move all players.
    for (Snake snake : snakes)
      if (snake != null && snake.isAlive())
        snake.move();

    // Check for deaths.
    for (Snake snake : snakes)
      if(snake != null && snake.isAlive())
        if (snake.checkDeath())
          for (GameListener listener : listeners) listener.onSnakeDeath(snake);

    // Update all entities.
    for (Entity entity : entities)
      entity.onMove();

    // Draw all drawable snakes.
    for (Snake snake : snakes)
      if (snake != null && snake.isDrawable())
        snake.drawToMesh();

    for (GameListener listener : listeners) listener.afterMoveExecuted();
  }

  /**
   * Registers a given GameListener for this game.
   * The game will send actively send it signals for the listener to handle.
   */
  public void registerListener(GameListener listener) {
    listener.onRegistered(this);
    listeners.add(listener);
  }

  protected void sendBytes(byte[] bytes) {
    for(ConnectedThread connectedThread : originActivity.connectedThreads) {
      if (connectedThread != null) connectedThread.write(bytes);
    }
  }

  // Methods for overriding purposes.
  public void handleInputBytes(byte[] inputBytes, ConnectedThread sourceThread) {
    for (GameListener listener : listeners) listener.onInputBytesReceived(inputBytes, sourceThread);
  }
  public void onAppleEaten(Apple apple) {}

  /**
   * @param thread The receiver of this list of calls.
   * @return A list of protocol calls for initial synchronization between the host and a guest.
   */
  public List<byte[]> setupCallList(ConnectedThread thread) {
    List<byte[]> calls = new LinkedList<>();
    // TODO Add calls for initialization.

    return calls;
  }

  // Getters.
  public GameRenderer getRenderer() { return this.renderer; }
  public long getRandSeed() { return randSeed; }
  public double getSpeed() { return this.speed; }
  public boolean isOver() { return this.gameOver; }
  public List<Entity> getEntities() { return this.entities; }
  public List<Player> getPlayers() { return cornerMap.getPlayers(); }

  /** @return A list of all the snakes in this game that are still alive. */
  public List<Snake> getAliveSnakes() {
    List<Snake> aliveSnakes = new LinkedList<>();
    for (Snake snake : snakes) {
      if (snake != null && snake.isAlive()) aliveSnakes.add(snake);
    }
    return aliveSnakes;
  }

  /** @return The snake that occupies a given game corner. */
  public Snake getSnakeAt(ControllerBuffer.Corner corner) {
    for (Snake snake : snakes)
      if (snake.getControlCorner() == corner)
        return snake;
    return null;
  }


  public boolean isSingleplayer() { return cornerMap.getNumberOfPlayers() == 1; }
  public Snake assessWinner() {
    for (Snake snake : snakes)
      if (snake != null && snake.isAlive()) 
        return snake;
    
    // If no players are alive it returns null.
    return null;
  }
  public SimpleTimer getGameOverTimer() { return this.gameOverTimer; }

  public List<MenuItem> getGameOverItems() { return this.gameOverItems; }
  public MenuItem getGameOverTopItem() { return this.gameOverTopItem; }
  public MenuItem getGameOverMiddleItem() { return this.gameOverMiddleItem; }
  public MenuItem getGameOverBottomItem() { return this.gameOverBottomItem; }

  public Coordinates<Integer> getRandomEmptySpace() {
    List<Coordinates<Integer>> emptySpaces = new LinkedList<>();
    for (int y = 0; y < getVerticalSquares(); y++)
      for (int x = 0; x < getHorizontalSquares(); x++)
        emptySpaces.add(new Coordinates<>(x, y));

    for (Snake snake : snakes)
      if (snake != null && snake.isAlive())
        for (BodyPart bodyPart : snake)
          emptySpaces.remove(new Coordinates<>(bodyPart.getX(), bodyPart.getY()));

    for (Entity entity : getEntities())
      emptySpaces.remove(new Coordinates<>(entity.x, entity.y));

    return emptySpaces.get((int) Math.floor(Math.random() * emptySpaces.size()));
  }
}
