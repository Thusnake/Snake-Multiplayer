package thusnake.snakemultiplayer;

/**
 * Any non-snake object that appears in-game and has some position on the game field and optionally
 * some conditional functionality.
 */
public abstract class Entity {
  int x, y;
  private float[] colors;
  private int[] textureMapCoordinates;
  final Game game;

  public Entity(Game game, int x, int y, float[] colors, int[] textureMapCoordinates) {
    if (!(x >= 0 && x < game.horizontalSquares && y >= 0 && y < game.verticalSquares))
      throw new IndexOutOfBoundsException("Entity coordinates exceed game bounds.");

    this.game = game;
    this.x = x;
    this.y = y;
    this.colors = colors;
    this.textureMapCoordinates = textureMapCoordinates;
  }

  /**
   * Performs the on-game-move procedures of this entity.
   * To be called at every game move.
   */
  public void onMove() {
    // Check if it's being hit by any player and call onHit() in such case.
    for (Player player : game.getPlayers())
      if (isHitBy(player))
        onHit(player);

    drawToMesh();
  }

  /**
   * Performs the on-every-frame procedures of this entity.
   * To be called at every frame.
   */
  public void onDraw() {}

  /** Applies this entity's color and texture to its position on the game Mesh. */
  public void drawToMesh() {
    game.getBoardSquares().updateColors(x, y, colors);
    game.getBoardSquares().updateTextures(x, y, textureMapCoordinates);
  }

  /**
   * Clears this entity's color and texture from its position on the game Mesh.
   * To be called before moving to another position or removing it from the Mesh altogether.
   */
  public void clearFromMesh() {
    game.getBoardSquares().updateColors(x, y, new float[] {1f, 1f, 1f, 1f});
    game.getBoardSquares().updateTextures(x, y, new int[] {31, 0, 31, 0});
  }

  /**
   * Checks if a snake's head is currently on top of this entity.
   * @param player The player to check for.
   */
  public boolean isHitBy(Player player) {
    return player != null && player.isAlive() && player.getX() == x && player.getY() == y;
  }

  public abstract void onHit(Player player);

  /** Sets this entity's position to a new one. */
  public void setPosition(int x, int y) {
    clearFromMesh();
    this.x = x;
    this.y = y;
    drawToMesh();
  }

  public void setColors(float[] colors) { this.colors = colors; }
  public void setTextureMapCoordinates(int[] coordinates) {textureMapCoordinates = coordinates; }
}
