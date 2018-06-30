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
  private OpenGLES20Activity originActivity;
  private int fontSize;
  private int screenWidth, screenHeight;
  private GL10 gl;
  private GLText glText;
  private SharedPreferences scores;
  private SharedPreferences.Editor scoresEditor;
  private Menu menu;
  private Game game;
  private FullscreenMessage interruptingMessage;
  private long previousTime = System.nanoTime();
  private boolean pointerIsDown = false;
  private double pointerDownTime = 0;

  public GameRenderer(Context context) {
    super();
    this.context = context;
    this.originActivity = (OpenGLES20Activity) context;
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

    if (this.pointerIsDown) this.pointerDownTime += dt;

    // Online host routines independent of game and menu.
    if (originActivity.isHost()) {
      // Update the awaiting disconnect list.
      for (ConnectedThread thread : originActivity.awaitingDisconnectThreads)
        if (thread != null)
          if (thread.getDisconnectRequestTimer().getTime() > 5) {
            // If the thread still hasn't disconnected - manually disconnect it and remove it
            // from the list.
            originActivity.awaitingDisconnectThreads.remove(thread);
            originActivity.closeConnectedGuestThread(thread);
          }
          else
            thread.getDisconnectRequestTimer().countUp(dt);

      // Update the last activity trackers and ping threads which have been inactive.
      for (ConnectedThread thread : originActivity.connectedThreads)
        if (thread != null) {
          thread.getLastActivityTimer().countUp(dt);

          if (thread.getLastActivityTimer().getTime() > 10)
            // If a thread has been inactive for too long - disconnect it.
            originActivity.closeConnectedGuestThread(thread);
          else if (thread.getLastActivityTimer().getTime() > 5)
            // If a thread has been inactive for a bit - ping it to force activity.
            thread.write(new byte[]{Protocol.PING});
        }
    }

    gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
    gl.glMatrixMode(GL10.GL_MODELVIEW);
    gl.glLoadIdentity();

    // Draw the game or (if the game doesn't exist) the menu.
    if (interruptingMessage == null) {
      if (this.game == null) menu.run(dt);
      else game.run(dt);
    } else
      interruptingMessage.run(dt);
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
    this.fontSize = Math.min((int)((float)height * (1f/6f)), (int)(width / 6f));
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
  public void setMenuState(MenuDrawable origin, Menu.MenuState state) { menu.setState(origin, state); }
  public void setMenuStateToPlayerOptions(int playerIndex) {
    menu.setPlayerOptionsIndex(playerIndex);
    menu.setState(this.menu.getCurrentMenuItems()[playerIndex], Menu.MenuState.PLAYERSOPTIONS);
  }
  public float getScreenWidth() { return this.screenWidth; }
  public float getScreenHeight() { return this.screenHeight; }

  public void startGame(Player[] players) {
    if (originActivity.isGuest())
      game = new GuestGame(this, screenWidth, screenHeight, players);
    else if (originActivity.isHost()) {
      game = new OnlineHostGame(this, screenWidth, screenHeight, players);
      if (originActivity.acceptThread != null) {
        originActivity.acceptThread.cancel();
        originActivity.acceptThread = null;
      }
    } else
      game = new Game(this, screenWidth, screenHeight, players);
  }
  public void quitGame() {
    game = null;
  }
  public void triggerStats() {
    // TODO Opens game stats
  }

  public void setPointerDown() {
    this.pointerIsDown = true;
  }

  public void setPointerUp() {
    this.pointerIsDown = false;
    this.pointerDownTime = 0;
  }

  public double getPointerDownTime() { return this.pointerDownTime; }

  public void setInterruptingMessage(FullscreenMessage interruptingMessage) {
    this.interruptingMessage = interruptingMessage;
    menu.updateState();
  }

  public FullscreenMessage getInterruptingMessage() { return interruptingMessage; }

  public void handleInputBytes(byte[] bytes, ConnectedThread sourceThread) {
    // Universal input byte handlers.
    switch (bytes[0]) {
      case Protocol.DISCONNECT_REQUEST:
        if (originActivity.isHost()) {
          // Approve the request.
          sourceThread.write(new byte[] {Protocol.DISCONNECT});

          // Add the thread to the awaiting disconnect list in case it doesn't answer anymore.
          originActivity.awaitingDisconnectThreads.add(sourceThread);
        }
        break;
      case Protocol.DISCONNECT:
        if (originActivity.isGuest()) {
          // Answer the call first.
          originActivity.writeBytesAuto(new byte[] {Protocol.WILL_DISCONNECT});

          // Disconnect afterwards.
          originActivity.connectedThread.cancel();
          originActivity.connectedThread = null;
          this.game = null;
          this.menu.endGuest();
        }
        break;
      case Protocol.WILL_DISCONNECT:
        if (originActivity.isHost()) {
          // Remove it from the awaiting disconnect list.
          originActivity.awaitingDisconnectThreads.remove(sourceThread);

          // Stop and remove the thread.
          originActivity.closeConnectedGuestThread(sourceThread);
        }
        break;
      case Protocol.REQUEST_NUMBER_OF_DEVICES:
        sourceThread.write(new byte[]
            {Protocol.NUMBER_OF_DEVICES, (byte) originActivity.getNumberOfRemoteDevices()});
        break;
      case Protocol.REQUEST_NUMBER_OF_READY:
        sourceThread.write(new byte[]
            {Protocol.NUMBER_OF_READY, (byte) originActivity.getNumberOfReadyRemoteDevices()});
        break;
      case Protocol.IS_READY:
        if (!originActivity.isGuest())
          sourceThread.setReady(true);
        break;
      case Protocol.IS_NOT_READY:
        if (!originActivity.isGuest())
          sourceThread.setReady(false);
        break;

      case Protocol.NUMBER_OF_READY:
        if (originActivity.isGuest()) {
          originActivity.numberOfReadyRemoteDevices = bytes[1];
        }
        break;
      case Protocol.NUMBER_OF_DEVICES:
        if (originActivity.isGuest()) {
          originActivity.numberOfRemoteDevices = bytes[1];
        }
        break;
      case Protocol.READY_STATUS:
        if (originActivity.isGuest()) {
          boolean receivedReady = bytes[1] == (byte) 1;

          // Set the ready status without requesting anything further.
          originActivity.forceSetReady(receivedReady);
        }
        break;
      case Protocol.READY_NUMBER_AND_STATUS:
        if (originActivity.isGuest()) {
          originActivity.numberOfReadyRemoteDevices = bytes[1];
          originActivity.forceSetReady(bytes[2] == 1);
        }
        break;
      case Protocol.PING:
        sourceThread.write(new byte[] {Protocol.PING_ANSWER});
        break;
    }

    // Pass to the menu and game.
    menu.handleInputBytes(bytes, sourceThread);
    if (game != null) game.handleInputBytes(bytes, sourceThread);
  }
}
