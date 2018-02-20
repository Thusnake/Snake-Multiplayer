package thusnake.snakemultiplayer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 22/01/2018.
 */

public class MenuImage extends MenuDrawable {
  private SimpleTimer scaleX, scaleY;
  private float width, height;
  private Square drawable;
  private MenuAction action;

  public MenuImage(GameRenderer renderer, float x, float y, float width, float height) {
    super(renderer, x, y);
    this.scaleX = new SimpleTimer(1.0);
    this.scaleY = new SimpleTimer(1.0);
    this.width = width;
    this.height = height;
    this.drawable = new Square(-width/2, -height/2, width, height);
  }

  // Drawing methods
  public void draw() {
    gl.glPushMatrix();
    gl.glTranslatef(this.getX() + this.width/2f,
                    this.getY() + this.height/2f, 0);
    gl.glScalef((float) this.scaleX.getTime(), (float) this.scaleY.getTime(), 1);
    gl.glColor4f(this.getColors()[0],this.getColors()[1],this.getColors()[2],this.getColors()[3]);
    this.drawable.draw(this.gl);
    gl.glPopMatrix();
  }

  public void setGraphic(int id) {
    this.drawable.loadGLTexture(this.gl, this.renderer.getContext(), id);
  }


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

  // Getters
  public float getScaleX() { return (float) this.scaleX.getTime(); }
}
