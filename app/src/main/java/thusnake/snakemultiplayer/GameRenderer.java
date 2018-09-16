package thusnake.snakemultiplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.util.SparseArray;

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
  private Activity currentActivity;

  private long previousTime = System.nanoTime();
  private boolean pointerIsDown = false;
  private double pointerDownTime = 0;
  private SparseArray<Bitmap> textureCacheMap = new SparseArray<>();

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
    // Enable 2D textures in case something has disabled it. (like minimizing the app)
    gl.glEnable(GL10.GL_TEXTURE_2D);

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
    } else if (originActivity.isGuest()) {
      // Update the last activity tracker and ping the host if it's been inactive.
      if (originActivity.connectedThread != null) {
        originActivity.connectedThread.getLastActivityTimer().countUp(dt);

        if (originActivity.connectedThread.getLastActivityTimer().getTime() > 10) {
          originActivity.connectedThread.cancel();
          originActivity.connectedThread = null;
          menu.endGuest();
        }
        else if (originActivity.connectedThread.getLastActivityTimer().getTime() > 5)
          originActivity.connectedThread.write(new byte[]{Protocol.PING});
      }
    }

    gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
    gl.glMatrixMode(GL10.GL_MODELVIEW);
    gl.glLoadIdentity();

    // Determine and run the current activity based on a priority order.
    if (interruptingMessage != null) currentActivity = interruptingMessage;
    else if (game != null)           currentActivity = game;
    else                             currentActivity = menu;

    currentActivity.run(dt);
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
    this.fontSize = Math.min((int)(height / 8f), (int)(width / 8f));

    // Reload the glText.
    if (glText == null) this.glText = new GLText(gl, context.getAssets());
    this.glText.load("PressStart2P.ttf", this.fontSize, 2, 2);

    // Reload all active textures.
    if (currentActivity != null) currentActivity.refresh();

    this.scoresEditor = scores.edit();

    this.screenWidth = width;
    this.screenHeight = height;

    if (menu == null) menu = new Menu(this, width, height);
  }

  public OpenGLES20Activity getOriginActivity() { return originActivity; }
  public GL10 getGl() { return this.gl; }
  public GLText getGlText() { return glText; }
  public Context getContext() { return this.context; }
  public boolean isInGame() { return this.game != null; }
  public Menu getMenu() { return this.menu; }
  public Game getGame() { return this.game; }
  public float getScreenWidth() { return this.screenWidth; }
  public float getScreenHeight() { return this.screenHeight; }
  public float smallDistance() { return screenHeight / 72f; }

  public void startGame(Game game) {
    /*
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
    */

    if (originActivity.isHost())
      this.game = new OnlineHostGame(this, menu.getSetupBuffer());
    else
      this.game = game;
  }

  public void restartGame() {
    try {
      this.game = game.getClass().getConstructor(GameRenderer.class, GameSetupBuffer.class).newInstance(this, menu.getSetupBuffer());
    } catch (Exception exception) {
      throw new RuntimeException("Could not restart game: " + exception.getMessage());
    }
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

  public void cacheTexture(Bitmap textureBitmap, int id) {
    textureCacheMap.append(id, textureBitmap);
  }

  /**
   * Attempts to a texture's bitmap in the cache from its ID. If it can't find it, the bitmap is
   * instead decoded and saved in the cache.
   * @param id The Android resource ID of the texture image.
   * @return The loaded or decoded Bitmap.
   */
  public Bitmap loadTextureBitmap(int id) {
    Bitmap bitmap;
    if ((bitmap = getTextureFromCache(id)) == null) {
      bitmap = BitmapFactory.decodeResource(originActivity.getResources(), id);

      // Cache the texture for next time.
      cacheTexture(bitmap, id);
    }

    return bitmap;
  }

  public Bitmap getTextureFromCache(int id) {
    return textureCacheMap.get(id);
  }

  public void setInterruptingMessage(FullscreenMessage interruptingMessage) {
    this.interruptingMessage = interruptingMessage;
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
          game = null;
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
      case Protocol.NUM_DEVICES_AND_READY_WITH_STATUS:
        if (originActivity.isGuest()) {
          originActivity.numberOfRemoteDevices = bytes[1];
          originActivity.numberOfReadyRemoteDevices = bytes[2];
          originActivity.forceSetReady(bytes[3] == 1);

          if (bytes[3] == 1 && game == null) {
            setInterruptingMessage(new FullscreenMessage(this, "Waiting for "
                + originActivity.connectedThread.device.getName() + " to start the game...") {
              @Override
              public void onCancel() {
                originActivity.writeBytesAuto(new byte[] {Protocol.IS_NOT_READY});
              }

              @Override
              public void run(double dt) {
                super.run(dt);
                if (!originActivity.isReady() || !originActivity.isGuest())
                  setInterruptingMessage(null);
              }
            });
          }
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
