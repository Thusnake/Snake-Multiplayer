package thusnake.snakemultiplayer;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a visual alteration to the default design of the snake.
 * Has nothing to do with the actual skin of snakes, which is gross.
 */
class SnakeSkin {
  private float[] headColors = new float[4];
  private float[] tailColors = new float[4];
  enum TextureType {HEAD, BODY, TURN, TAIL}

  class BodyPartTileCoordinates {
    final int downX, downY, rightX, rightY, upX, upY, leftX, leftY, tileSize;

    BodyPartTileCoordinates(int downwardTileX, int downwardTileY, int tileSize) {
      downX = downwardTileX;
      downY = downwardTileY;
      rightX = downwardTileX + tileSize;
      rightY = downwardTileY;
      upX = downwardTileX + tileSize * 2;
      upY = downwardTileY;
      leftX = downwardTileX + tileSize * 3;
      leftY = downwardTileY;
      this.tileSize = tileSize;
    }

    int[] getCoordinates(@NonNull Player.Direction direction) {
      int x, y;
      switch(direction) {
        case UP: x = upX; y = upY; break;
        case DOWN: x = downX; y = downY; break;
        case LEFT: x = leftX; y = leftY; break;
        case RIGHT: x = rightX; y = rightY; break;
        default: throw new
            RuntimeException("Passed null direction to BodyPartTileCoordinates.getCoordinates()");
      }
      return new int[] {x, y, x + tileSize - 1, y + tileSize - 1};
    }
  }

  class NonDirectionalBodyPartTileCoordinates extends BodyPartTileCoordinates {
    NonDirectionalBodyPartTileCoordinates(int tileX, int tileY, int tileSize) {
      super(tileX, tileY, tileSize);
    }

    /**
     * Direction passed is not important, as it always calls the super method with DOWN as an
     * argument. It's not the best solution, but it works and I'm lazy.
     */
    @Override
    int[] getCoordinates(@NonNull Player.Direction direction) {
      return super.getCoordinates(Player.Direction.DOWN);
    }
  }

  final BodyPartTileCoordinates headTiles, bodyTiles, turnTiles, tailTiles;

  SnakeSkin(float[] headColors, float[] tailColors) {
    System.arraycopy(headColors, 0, this.headColors, 0, 4);
    System.arraycopy(tailColors, 0, this.tailColors, 0, 4);
    headTiles = new NonDirectionalBodyPartTileCoordinates(0, 5, 4);
    bodyTiles = headTiles;
    turnTiles = headTiles;
    tailTiles = headTiles;
  }

  SnakeSkin(float[] colors, int bottomLeftTileX, int bottomLeftTileY, int tileSize) {
    System.arraycopy(colors, 0, headColors, 0, 4);
    System.arraycopy(colors, 0, tailColors, 0, 4);
    headTiles = new BodyPartTileCoordinates(bottomLeftTileX, bottomLeftTileY, tileSize);
    bodyTiles = new BodyPartTileCoordinates(bottomLeftTileX, bottomLeftTileY + 1, tileSize);
    turnTiles = new BodyPartTileCoordinates(bottomLeftTileX, bottomLeftTileY + 2, tileSize);
    tailTiles = new BodyPartTileCoordinates(bottomLeftTileX, bottomLeftTileY + 3, tileSize);
  }
  
  float[] headColors() { return headColors; }
  float[] tailColors() { return tailColors; }

  /**
   * Returns coordinates in the tilemap of a specific texture.
   * @param textureType The type of texture required (head, body, tail or turn).
   * @param direction The direction this texture is going.
   * @return An array of integers for these values respectively :
   * bottomLeftX, bottomLeftY, topRightX, topRightY.
   */
  int[] texture(TextureType textureType, Player.Direction direction) {
    BodyPartTileCoordinates tileset;
    switch(textureType) {
      case HEAD: tileset = headTiles; break;
      case BODY: tileset = bodyTiles; break;
      case TURN: tileset = turnTiles; break;
      case TAIL: tileset = tailTiles; break;
      default: throw new RuntimeException("Passed null textureType to SnakeSkin.texture()");
    }
    return tileset.getCoordinates(direction);
  }

  Mesh previewMesh(GameRenderer renderer, float x, float y, MenuDrawable.EdgePoint alignPoint,
                   float totalHeight, int squares) {
    Mesh previewMesh = new Mesh(renderer, x, y, alignPoint, (totalHeight - squares + 1) / squares,
                                1, squares);
    previewMesh.updateColors(0, headColors());
    for (int index = 1; index < squares; index++)
      previewMesh.updateColors(index, tailColors());

    previewMesh.updateTextures(0, 0, texture(TextureType.HEAD, Player.Direction.DOWN));
    for (int index = 1; index < squares - 1; index++)
      previewMesh.updateTextures(0, index, texture(TextureType.BODY, Player.Direction.DOWN));
    previewMesh.updateTextures(0, squares - 1, texture(TextureType.TAIL, Player.Direction.DOWN));

    return previewMesh;
  }

  @NonNull
  public static float[] hslToRgba(float h, float s, float l) {
    s /= 100;
    l /= 100;
    float c = (1 - Math.abs(2f * l - 1f)) * s;
    float h_ = h / 60f;
    float h_mod2 = h_ % 2;

    float x = c * (1 - Math.abs(h_mod2 - 1));
    float r_, g_, b_;
    if (h_ < 1)      { r_ = c; g_ = x; b_ = 0; }
    else if (h_ < 2) { r_ = x; g_ = c; b_ = 0; }
    else if (h_ < 3) { r_ = 0; g_ = c; b_ = x; }
    else if (h_ < 4) { r_ = 0; g_ = x; b_ = c; }
    else if (h_ < 5) { r_ = x; g_ = 0; b_ = c; }
    else             { r_ = c; g_ = 0; b_ = x; }

    float m = l - (0.5f * c);
    float r = (r_ + m);
    float g = (g_ + m);
    float b = (b_ + m);
    return new float[] {r, g, b, 1f};
  }

  static SnakeSkin white = new SnakeSkin(hslToRgba(0, 0, 100), hslToRgba(0, 0, 50));
  static SnakeSkin red = new SnakeSkin(hslToRgba(0, 100, 50), hslToRgba(0, 50, 33));
  static SnakeSkin orange = new SnakeSkin(hslToRgba(40, 100, 50), hslToRgba(40, 50, 33));
  static SnakeSkin yellow = new SnakeSkin(hslToRgba(60, 100, 50), hslToRgba(60, 50, 33));
  static SnakeSkin green = new SnakeSkin(hslToRgba(120, 100, 50), hslToRgba(120, 50, 33));
  static SnakeSkin teal = new SnakeSkin(hslToRgba(160, 100, 50), hslToRgba(160, 50, 33));
  static SnakeSkin blue = new SnakeSkin(hslToRgba(240, 100, 50), hslToRgba(240, 50, 33));
  static SnakeSkin purple = new SnakeSkin(hslToRgba(290, 100, 50), hslToRgba(290, 50, 33));
  static SnakeSkin coral = new SnakeSkin(hslToRgba(330, 100, 50), hslToRgba(330, 50, 33));
  static SnakeSkin oldSchool = new SnakeSkin(new float[] {1f, 1f, 1f, 1f}, 0, 9, 1);


  static List<SnakeSkin> allSkins = Arrays.asList(white, red, orange, yellow, green, teal, blue,
                                                  purple, coral, oldSchool);
}
