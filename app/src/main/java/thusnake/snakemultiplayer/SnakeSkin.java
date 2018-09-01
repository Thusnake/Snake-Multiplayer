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
  SnakeSkin(float[] headColors, float[] tailColors) {
    System.arraycopy(headColors, 0, this.headColors, 0, 4);
    System.arraycopy(tailColors, 0, this.tailColors, 0, 4);
  }
  
  float[] headColors() { return headColors; }
  float[] tailColors() { return tailColors; }

  Mesh previewMesh(GameRenderer renderer, float x, float y, MenuDrawable.EdgePoint alignPoint,
                   float totalHeight, int squares) {
    Mesh previewMesh = new Mesh(renderer, x, y, alignPoint, (totalHeight - squares + 1) / squares,
                                1, squares);
    previewMesh.updateColors(0, headColors());
    for (int index = 1; index < squares; index++)
      previewMesh.updateColors(index, tailColors());

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
  static SnakeSkin red = new SnakeSkin(hslToRgba(0, 100, 50), hslToRgba(0, 50, 50));
  static SnakeSkin orange = new SnakeSkin(hslToRgba(40, 100, 50), hslToRgba(40, 50, 50));
  static SnakeSkin yellow = new SnakeSkin(hslToRgba(60, 100, 50), hslToRgba(60, 50, 50));
  static SnakeSkin green = new SnakeSkin(hslToRgba(120, 100, 50), hslToRgba(120, 50, 50));
  static SnakeSkin teal = new SnakeSkin(hslToRgba(160, 100, 50), hslToRgba(160, 50, 50));
  static SnakeSkin blue = new SnakeSkin(hslToRgba(240, 100, 50), hslToRgba(240, 50, 50));
  static SnakeSkin purple = new SnakeSkin(hslToRgba(290, 100, 50), hslToRgba(290, 50, 50));
  static SnakeSkin black = new SnakeSkin(hslToRgba(0, 0, 0), hslToRgba(0, 0, 30));


  static List<SnakeSkin> allSkins = Arrays.asList(white, red, orange, yellow, green, teal, blue,
                                                  purple, black);
}
