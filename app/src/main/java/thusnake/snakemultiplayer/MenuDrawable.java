package thusnake.snakemultiplayer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 22/01/2018.
 */

public class MenuDrawable implements MenuButton {
  private GameRenderer renderer;
  private GL10 gl;
  private SimpleTimer x, y, scaleX, scaleY;
  private float width, height;
  private float[] colors = {1f, 1f, 1f, 1f};
  private Square drawable;
  private MenuAction action;

  public MenuDrawable(GameRenderer renderer, float x, float y, float width, float height) {
    this.renderer = renderer;
    this.gl = renderer.getGl();
    this.x = new SimpleTimer(x);
    this.y = new SimpleTimer(y);
    this.scaleX = new SimpleTimer(1.0);
    this.scaleY = new SimpleTimer(1.0);
    this.width = width;
    this.height = height;
    this.drawable = new Square(-width/2, -height/2, width, height);
  }

  // Drawing methods
  public void draw() {
    gl.glPushMatrix();
    gl.glTranslatef((float) this.x.getTime() + this.width/2f,
                    (float) this.y.getTime() + this.height/2f, 0);
    gl.glScalef((float) this.scaleX.getTime(), (float) this.scaleY.getTime(), 1);
    gl.glColor4f(this.colors[0], this.colors[1], this.colors[2], this.colors[3]);
    this.drawable.draw(this.gl);
    gl.glPopMatrix();
  }

  public void setGraphic(int id) {
    this.drawable.loadGLTexture(this.gl, this.renderer.getContext(), id);
  }

  public void setColors(float[] rgba) {
    if (rgba.length == 4)
      for (int index = 0; index < 4; index++)
        this.colors[index] = rgba[index];
  }
  public void setOpacity(float opacity) { this.colors[3] = opacity; }

  // Position manipulation methods
  public void move(double dt) {
    if (!this.scaleX.isDone()) this.scaleX.countEaseOut(dt, 8, 5*dt);
    if (!this.scaleY.isDone()) this.scaleY.countEaseOut(dt, 8, 5*dt);
  }

  public void setScale(float scale) {
    this.scaleX.setTime(scale);
    this.scaleY.setTime(scale);
  }
  public void setScaleDestination(float scale) {
    this.scaleX.setEndTimeFromNow(scale);
    this.scaleY.setEndTimeFromNow(scale);
  }

  // Functional methods.
  public void setAction(MenuAction action) { this.action = action; }
  public void performAction() { this.action.perform(this.renderer, this); }

  // Getters
  public boolean isClicked(float x, float y) {
    return (x > this.x.getTime() && x < this.x.getTime() + this.width
        && renderer.getScreenHeight() - y > this.y.getTime()
        && renderer.getScreenHeight() - y < this.y.getTime() + this.height);
  }
  public float getScaleX() { return (float) this.scaleX.getTime(); }
  public float getX() { return (float) this.x.getTime(); }
  public float getY() { return (float) this.y.getTime(); }
}
