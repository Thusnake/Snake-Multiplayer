package thusnake.snakemultiplayer;

/**
 * Basically a Pair, but much more elegant when used for coordinates.
 * @param <T>
 */
public class Coordinates<T> {
  public final T x, y;

  public Coordinates(T x, T y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Coordinates)
      return x == ((Coordinates) obj).x && y == ((Coordinates) obj).y;
    else
      return super.equals(obj);
  }
}
