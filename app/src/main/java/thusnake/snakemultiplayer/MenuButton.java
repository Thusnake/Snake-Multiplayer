package thusnake.snakemultiplayer;

/**
 * Created by Nick on 23/01/2018.
 */

public interface MenuButton {
  float getX();
  float getY();
  boolean isClicked(float x, float y);
  void setOpacity(float opacity);
}
