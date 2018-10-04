package thusnake.snakemultiplayer;

import android.util.Pair;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by vency on 4.3.2016.
 */
public class Apple {
  int number, x, y;
  private Integer nextX, nextY;
  private double[] colors = {1.0, 1.0, 1.0, 1.0};
  private final Game game;

  public Apple(Game game){
    this.game = game;

    Pair<Integer, Integer> randomEmptySpace = getRandomEmptySpace();
    this.x = randomEmptySpace.first;
    this.y = randomEmptySpace.second;
  }

  public boolean check(Player player) {
    if (player.isAlive() && player.getBodyPart(0).getX() == this.x
        && player.getBodyPart(0).getY() == this.y) {
      feed(player);
      die(this.x, this.y);
      return true;
    }
    return false;
  }

  public void feed(Player player) {
    player.expandBody();
    player.increaseScore(1);
  }

  public void die(int x, int y){
    if (nextX != null && nextY != null) {
      // If there is a predetermined next position, don't bother searching for a new one.
      this.x = nextX;
      this.y = nextY;
      nextX = null;
      nextY = null;
    } else {
      Pair<Integer, Integer> randomEmptySpace = getRandomEmptySpace();
      this.x = randomEmptySpace.first;
      this.y = randomEmptySpace.second;
    }
    game.onAppleEaten(this);
  }

  private Pair<Integer, Integer> getRandomEmptySpace() {
    List<Pair<Integer,Integer>> emptySpaces = new LinkedList<>();
    for (int y = 0; y < game.getVerticalSquares(); y++)
      for (int x = 0; x < game.getHorizontalSquares(); x++)
        emptySpaces.add(new Pair<>(x, y));

    for (Player player : game.getPlayers())
      if (player != null && player.isAlive())
        for (BodyPart bodyPart : player.getBodyParts())
          emptySpaces.remove(new Pair<>(bodyPart.getX(), bodyPart.getY()));

    return emptySpaces.get((int) Math.floor(Math.random() * emptySpaces.size()));
  }

  public void updateColors() {
    this.game.getBoardSquares().updateColors(this.x, this.y, this.colors);
    game.getBoardSquares().updateTextures(x, y, 0, 0, 4, 4);
  }

  public int getX() { return x; }
  public int getY() { return y; }

  public void setX(int x) {
    game.getBoardSquares().updateColors(this.x, this.y, 0.125f, 0.125f, 0.125f, 1f);
    game.getBoardSquares().updateTextures(this.x, this.y, 0, 0, 1, 1);
    this.x = x;
  }
  public void setY(int y) {
    game.getBoardSquares().updateColors(this.x, this.y, 0.125f, 0.125f, 0.125f, 1f);
    game.getBoardSquares().updateTextures(this.x, this.y, 0, 0, 1, 1);
    this.y = y;
  }

  public void setNextPosition(int x, int y) { this.nextX = x; this.nextY = y; }

  public double[] getColors() {
    return this.colors;
  }
}
