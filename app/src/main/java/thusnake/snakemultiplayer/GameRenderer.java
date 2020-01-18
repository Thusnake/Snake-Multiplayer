package thusnake.snakemultiplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.SparseArray;

import com.android.texample.GLText;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 22/12/2017.
 */

public class GameRenderer implements GLSurfaceView.Renderer {
  private Context context;
  private OpenGLActivity originActivity;
  private int fontSize;
  private int screenWidth, screenHeight;
  private GL10 gl;
  private GLText glText;
  private SharedPreferences scores;
  private SharedPreferences.Editor scoresEditor;

  /** The activity stack. Can only have 1 Menu element and 1 Game element at a given time. */
  private Stack<Activity> activityStack = new Stack<>();

  private long previousTime = System.nanoTime();
  private boolean pointerIsDown = false;
  private double pointerDownTime = 0;
  private SparseArray<Bitmap> resourceIDMap = new SparseArray<>();
  private Map<Integer, Bitmap> glTexturePointerMap = new LinkedHashMap<>();

  public GameRenderer(Context context) {
    super();
    this.context = context;
    this.originActivity = (OpenGLActivity) context;
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
          getMenu().endGuest();
        }
        else if (originActivity.connectedThread.getLastActivityTimer().getTime() > 5)
          originActivity.connectedThread.write(new byte[]{Protocol.PING});
      }
    }

    gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
    gl.glMatrixMode(GL10.GL_MODELVIEW);
    gl.glLoadIdentity();

    getCurrentActivity().run(dt);
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
    reloadAllTextures();

    this.scoresEditor = scores.edit();

    this.screenWidth = width;
    this.screenHeight = height;

    // If it's the first onSurfaceChanged() call then we'll set up a Menu and a loading screen.
    if (getMenu() == null) {
      activityStack.push(new Menu(this, width, height));

      activityStack.push(new FullscreenMessage(this, "Loading...") {
        private Square loadingBar;
        private final Thread loadingThread = new Thread() {
          int loadedResources = 0;
          @Override
          public void run() {
            Field[] fields = R.drawable.class.getFields();
            for (Field field : fields) {
              try {
                loadTextureBitmap(field.getInt(null));
              } catch (IllegalAccessException exception) {
                System.out.println("Couldn't access resource " + field.getName());
              } finally {
                loadingBar.setCoordinates(renderer.getScreenWidth() / 4f,
                    message.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f - 40,
                    ++loadedResources * screenWidth / 2f / fields.length, 80);
              }
            }
            quit();
          }
        };
        private void quit() { renderer.cancelActivity(this); }

        // I'm using this method to do the constructor calls as I have no access to the constructor
        // itself.
        @Override
        public FullscreenMessage withLoadingSnake(boolean loading) {
          loadingBar = new Square(renderer, renderer.getScreenWidth() / 4f,
                                  message.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f - 40, 0,
                                  80);
          loadingThread.start();
          cancelButton.setDrawable(false);
          return this;
        }

        @Override
        public void run(double dt) {
          super.run(dt);
          loadingBar.draw(gl);
        }
      }.withLoadingSnake(true));
    }
  }

  public OpenGLActivity getOriginActivity() { return originActivity; }
  public GL10 getGl() { return this.gl; }
  public GLText getGlText() { return glText; }
  public Context getContext() { return this.context; }
  public boolean isInGame() { return getCurrentActivity() instanceof Game; }

  public Menu getMenu() {
    Menu lastMenu = null;
    for (Activity activity : activityStack)
      if (activity instanceof Menu)
        lastMenu = (Menu) activity;
    return lastMenu;
  }

  public Game getGame() {
    Game lastGame = null;
    for (Activity activity : activityStack)
      if (activity instanceof Game)
        lastGame = (Game) activity;
    return lastGame;
  }

  public float getScreenWidth() { return this.screenWidth; }
  public float getScreenHeight() { return this.screenHeight; }
  public float smallDistance() { return screenHeight / 72f; }

  public void startGame(Game game) {
    activityStack.push(game);
  }

  /** Finds the game in the activity stack and replaces it with a new one of the same class. */
  public void restartGame(Game game) {
    if (game == null || !activityStack.contains(game)) return;
    int gameIndex = activityStack.indexOf(game);

    activityStack.setElementAt(getMenu().getSetupBuffer().createGame(), gameIndex);
  }

  public void quitGame() {
    activityStack.remove(getGame());
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

  /**
   * Attempts to find a resource's bitmap in the cache from its ID. If it can't find it, the bitmap
   * is instead decoded and saved in the cache, along with a generated OpenGL texture pointer.
   * @param id The Android resource ID of the texture image.
   * @return The OpenGL generated texture pointer for that resource id's bitmap.
   */
  public int loadTextureBitmapToPointer(int id) {
    // If its bitmap has not been loaded (should not be possible) then load its bitmap into the
    // cache.
    if (getTextureFromCache(id) == null)
      loadTextureBitmap(id);

    Bitmap bitmap = getTextureFromCache(id);
    if (bitmap == null) return 0;

    // If the pointer map doesn't have a pointer for that bitmap then we'll create one.
    if (!glTexturePointerMap.containsValue(bitmap)) {
      int pointer = bindTextureAndGeneratePointer(bitmap);

      // Cache the texture and pointer for next time.
      cacheTextureAndGLPointer(bitmap, pointer);
    }

    for (Integer integer : glTexturePointerMap.keySet()) {
      if (glTexturePointerMap.get(integer) == resourceIDMap.get(id))
        return integer;
    }
    return 0;
  }

  /**
   * Binds a given Bitmap image to a generated texture pointer.
   * @param bitmap The Bitmap image to use.
   * @return The generated pointer.
   */
  public int bindTextureAndGeneratePointer(Bitmap bitmap) {
    int[] texturePointers = new int[1];

    gl.glGenTextures(1, texturePointers, 0);
    gl.glBindTexture(GL10.GL_TEXTURE_2D, texturePointers[0]);
    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);

    GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

    return texturePointers[0];
  }

  /**
   * Caches a Bitmap and an OpenGL pointer pair manually.
   * This pair will be used to refresh textures whenever appropriate.
   * @param bitmap The texture's bitmap.
   * @param GLPointer The (already allocated) OpenGL pointer to that texture.
   */
  public void cacheTextureAndGLPointer(Bitmap bitmap, int GLPointer) {
    glTexturePointerMap.put(GLPointer, bitmap);
  }

  /**
   * Removes an OpenGL pointer from the cache manually.
   * @param GLPointer The OpenGL pointer to be removed.
   */
  public void recycleGLPointer(int GLPointer) {
    // Delete the texture binding.
    gl.glDeleteTextures(1, new int[] {GLPointer}, 0);

    // Remove the pair from the cache.
    glTexturePointerMap.remove(GLPointer);
  }

  /**
   * Simply loads a texture by its resource ID and puts it in the resourceID map.
   * @return The bitmap associated with the given ID.
   */
  public Bitmap loadTextureBitmap(int id) {
    Bitmap bitmap;
    if ((bitmap = getTextureFromCache(id)) == null) {
      try {
        InputStream is = null;
        try {
          is = originActivity.getResources().openRawResource(id);
          bitmap = BitmapFactory.decodeStream(is);
        } catch (Resources.NotFoundException exception) { /* Ignore it. */ }
        finally {
          try {
            if (is != null) is.close();
          } catch(IOException e) {
            // Ignore.
          }
        }
        resourceIDMap.put(id, bitmap);
      } catch (OutOfMemoryError error) {
        System.out.println("Couldn't load resource with id " + id + " : " + error.getMessage());
      }
    }
    return bitmap;
  }

  /**
   * Checks the cache for a bitmap corresponding to a given resource ID.
   * @return The found bitmap. Returns null if no bitmap matches the ID.
   */
  public Bitmap getTextureFromCache(int id) {
    return resourceIDMap.get(id);
  }

  /**
   * Reloads all the textures that have been bound via texImage2D().
   * To be called after textures have been lost (e.g. app is out of focus).
   */
  public void reloadAllTextures() {
    for (int glPointer : glTexturePointerMap.keySet()) {
      Bitmap bitmap = glTexturePointerMap.get(glPointer);
      gl.glBindTexture(GL10.GL_TEXTURE_2D, glPointer);
      gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
      gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
      GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
    }
  }

  public void setInterruptingMessage(FullscreenMessage interruptingMessage) {
    activityStack.push(interruptingMessage);
  }

  public void cancelActivity(Activity activity) {
    activityStack.remove(activity);
  }

  public Activity getCurrentActivity() { return activityStack.peek(); }

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
          quitGame();
          getMenu().endGuest();
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

          if (bytes[3] == 1 && getGame() == null) {
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
                  cancelActivity(this);
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
    getMenu().handleInputBytes(bytes, sourceThread);
    if (getGame() != null) getGame().handleInputBytes(bytes, sourceThread);
  }
}
