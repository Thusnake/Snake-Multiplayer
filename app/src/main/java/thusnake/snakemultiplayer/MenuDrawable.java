package thusnake.snakemultiplayer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 22/01/2018.
 */

public class MenuDrawable {
  private GameRenderer renderer;
  private GL10 gl;
  private SimpleTimer x, y;
  private float width, height;
  private float scaleX = 1, scaleY = 1;
  private float[] colors = {1f, 1f, 1f, 1f};
  private Square drawable;

  public MenuDrawable(GameRenderer renderer, float x, float y, float width, float height) {
    this.renderer = renderer;
    this.gl = renderer.getGl();
    this.x = new SimpleTimer(x);
    this.y = new SimpleTimer(y);
    this.width = width;
    this.height = height;
    this.drawable = new Square(x, y, width, height);
  }

  // Drawing methods
  public void draw() {
    gl.glPushMatrix();
    gl.glScalef(this.scaleX, this.scaleY, 1);
    gl.glColor4f(this.colors[0], this.colors[1], this.colors[2], this.colors[3]);
    this.drawable.draw(this.gl);
    gl.glPopMatrix();
  }

  public void setGraphic(int id) {
    this.drawable.loadGLTexture(this.gl, this.renderer.getContext(), id);
  }

  public void setColor(float[] rgba) {
    if (rgba.length == 4)
      this.colors = rgba;
  }

  // Position manipulation methods
  public void move(double dt) {

  }

  public void setScale(float scale) {
    this.scaleX = scale;
    this.scaleY = scale;
  }

  // Getters
  public boolean isClicked(float x, float y) {
    return (x > this.x.getTime() && x < this.x.getTime() + this.width
        && renderer.getScreenHeight() - y > this.y.getTime()
        && renderer.getScreenHeight() - y < this.y.getTime() + this.height);
  }
  public float getScaleX() { return this.scaleX; }
}
