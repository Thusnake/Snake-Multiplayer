package thusnake.snakemultiplayer;

/**
 * Created by Nick on 31/12/2017.
 */

public interface MenuAction {
  void perform(GameRenderer renderer, MenuButton origin);
}
