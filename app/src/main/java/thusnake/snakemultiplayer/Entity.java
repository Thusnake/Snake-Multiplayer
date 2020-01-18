package thusnake.snakemultiplayer;

import android.support.annotation.NonNull;

import thusnake.snakemultiplayer.textures.TextureMapCoordinates;

/**
 * Any non-snake object that appears in-game and has some position on the game field and optionally
 * some conditional functionality.
 * Classes which extend Entity must have a constructor method with just one Game parameter.
 */
public abstract class Entity {
  int x, y;
  private float[] colors;
  public final int textureId;
  final Game game;

  public Entity(Game game, float[] colors, int textureId) {
    this.game = game;
    this.colors = colors;
    this.textureId = textureId;

    Coordinates<Integer> coordinates = getNextPosition();
    this.x = coordinates.x;
    this.y = coordinates.y;
    drawToMesh();
  }

  public Coordinates<Integer> getNextPosition() {
    return game.getRandomEmptySpace();
  }

  /**
   * Performs the on-game-move procedures of this entity.
   * To be called at every game move.
   */
  public void onMove() {
    // Check if it's being hit by any player and call onHit() in such case.
    for (Snake snake : game.getAliveSnakes())
      if (isHitBy(snake))
        onHit(snake);

    drawToMesh();
  }

  /**
   * Performs the on-every-frame procedures of this entity.
   * To be called at every frame.
   */
  public void onDraw() {}

  /** Applies this entity's color and texture to its position on the game Mesh. */
  public void drawToMesh() {
    if (game.getBoardSquares() != null) {
      game.getBoardSquares().updateColors(x, y, colors);
      game.getBoardSquares().updateTextures(x, y, game.getBoardSquares().textureMap.getTexture(this));
    }
  }

  /**
   * Clears this entity's color and texture from its position on the game Mesh.
   * To be called before moving to another position or removing it from the Mesh altogether.
   */
  public void clearFromMesh() {
    if (game.getBoardSquares() != null) {
      game.getBoardSquares().updateColors(x, y, new float[]{1f, 1f, 1f, 1f});
      game.getBoardSquares().updateTextures(x, y, (TextureMapCoordinates) null);
    }
  }

  /**
   * Checks if a snake's head is currently on top of this entity.
   * @param snake The snake to check for.
   */
  public boolean isHitBy(@NonNull Snake snake) {
    return snake.isAlive() && snake.getX() == x && snake.getY() == y;
  }

  public abstract void onHit(Snake snake);

  /** Sets this entity's position to a new one. */
  public void setPosition(int x, int y) {
    clearFromMesh();
    this.x = x;
    this.y = y;
    drawToMesh();
  }

  public void setColors(float[] colors) { this.colors = colors; }
}
