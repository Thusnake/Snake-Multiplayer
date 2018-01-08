package thusnake.snakemultiplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;

import com.android.texample.GLText;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 22/12/2017.
 */

public class GameRenderer implements GLSurfaceView.Renderer {
  private Context context;
  private int fontSize;
  private int screenWidth, screenHeight;
  private GL10 gl;
  private GLText glText;
  private SharedPreferences scores;
  private SharedPreferences.Editor scoresEditor;
  private Menu menu;
  private Game game;
  private long previousTime = System.nanoTime();

  public GameRenderer(Context context) {
    super();
    this.context = context;
  }

  @Override
  public void onSurfaceCreated(GL10 gl, javax.microedition.khronos.egl.EGLConfig config) {
    this.gl = gl;
    this.gl.glDisable(GL10.GL_DITHER);
    this.gl.glClearColor(0f, 0f, 0f, 0f);
    this.gl.glEnable(GL10.GL_TEXTURE_2D);
    this.gl.glShadeModel(GL10.GL_SMOOTH);
    this.gl.glClearColor(0f, 0f, 0f, 0.5f);
    this.gl.glClearDepthf(1.0f);
    this.gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
    this.scores = context.getSharedPreferences("scores", Context.MODE_PRIVATE);
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    // Calculate the delta time.
    long currentTime = System.nanoTime();
    long deltaTimeLong = currentTime - previousTime;
    this.previousTime = currentTime;
    double dt = deltaTimeLong / 1000000000.0;

    gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
    gl.glMatrixMode(GL10.GL_MODELVIEW);
    gl.glLoadIdentity();

    // Draw the game or (if the game doesn't exist) the menu.
    if (this.game == null) menu.run(dt);
    else game.run(dt);
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    this.gl.glViewport(0, 0, width, height);
    this.gl.glEnable(GL10.GL_NORMALIZE);
    this.gl.glMatrixMode(GL10.GL_PROJECTION);
    this.gl.glLoadIdentity();
    this.gl.glOrthof(0f, width, 0f, height, 1, -1);
    this.gl.glMatrixMode(GL10.GL_MODELVIEW);
    this.gl.glEnable(GL10.GL_BLEND);
    this.gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
    this.fontSize = (int)((float)height * (1f/6f));
    this.glText = new GLText(gl, context.getAssets());
    this.glText.load("pf_arma_five.ttf", this.fontSize, 2, 2);
    this.scoresEditor = scores.edit();

    this.screenWidth = width;
    this.screenHeight = height;

    if (this.menu == null) this.menu = new Menu(this, width, height);
  }

  public GL10 getGl() { return this.gl; }
  public GLText getGlText() { return glText; }
  public Context getContext() { return this.context; }
  public boolean isInGame() { return this.game != null; }
  public Menu getMenu() { return this.menu; }
  public Game getGame() { return this.game; }
  public int[] getPairedDevices() { return new int[] {}; } // TODO: Make it actually return the connected devices.
  public void setMenuState(Menu.MenuState state) { menu.setState(state); }
  public void setMenuStateToPlayerOptions(int playerIndex) {
    menu.setState(Menu.MenuState.PLAYERSOPTIONS);
    menu.setPlayerOptionsIndex(playerIndex);
  }
  public float getScreenWidth() { return this.screenWidth; }
  public float getScreenHeight() { return this.screenHeight; }

  public void startGame() {
    game = new Game(this, screenWidth, screenHeight);
  }

  public void quitGame() {
    game = null;
  }
}
