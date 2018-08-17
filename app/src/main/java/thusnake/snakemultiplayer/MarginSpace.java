package thusnake.snakemultiplayer;

public final class MarginSpace extends MenuDrawable {
  public MarginSpace(GameRenderer renderer, float x, float y, EdgePoint alignPoint, float width,
                     float height) {
    super(renderer, x, y, alignPoint);
    setWidth(width);
    setHeight(height);
  }

  public void draw(float[] parentColors) {}

  public static MarginSpace makeHorizontal(GameRenderer renderer, float bottomY, float topY) {
    return new MarginSpace(renderer, 0, topY, EdgePoint.TOP_LEFT, renderer.getScreenWidth(),
                           topY - bottomY);
  }
}
