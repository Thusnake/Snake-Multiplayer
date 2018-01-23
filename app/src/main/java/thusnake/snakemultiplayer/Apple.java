package thusnake.snakemultiplayer;

/**
 * Created by vency on 4.3.2016.
 */
public class Apple {
  int number, x, y;
  private double[] colors = {1.0, 1.0, 1.0, 1.0};
  private final Game game;

  public Apple(Game game){
    /*if (MyGLRenderer.playingBluetooth) {
      byte[] send = {5,(byte)this.x,(byte)this.y};
      MyGLRenderer.writeHost(send);
    } */

    this.game = game;
    this.x = OpenGLES20Activity.random(1, game.getHorizontalSquares()) - 1;
    this.y = OpenGLES20Activity.random(1, game.getVerticalSquares()) - 1;

    for (Player player : game.getPlayers()) {
      this.check(player);
    }
  }

  public boolean check(Player player) {
    if (player.isAlive() && player.getBodyPart(0).getX() == this.x
        && player.getBodyPart(0).getY() == this.y) {
      player.expandBody();
      player.increaseScore(1);
      this.die(this.x, this.y);
      return true;
    }
    return false;
  }

  public void die(int x, int y){
    this.x = OpenGLES20Activity.random(1, game.getHorizontalSquares()) - 1;
    this.y = OpenGLES20Activity.random(1, game.getVerticalSquares()) - 1;
    if (this.x == x && this.y == y) {
      this.die(this.x, this.y);
      return;
    }
    for (Player player : game.getPlayers())
      if (player.isAlive())
        for (BodyPart bodyPart : player.getBodyParts())
          if (this.x == bodyPart.getX() && this.y == bodyPart.getY()) {
            this.die(this.x, this.y);
            return;
          }
    /*
    if (MyGLRenderer.playingBluetooth) {
      byte[] send = {5,(byte)this.x,(byte)this.y};
      MyGLRenderer.writeHost(send);
    }*/
  }

  public void updateColors() {
    this.game.getBoardSquares().updateColors(this.x, this.y, this.colors);
  }

  public double[] getColors() {
    return this.colors;
  }
}
