package thusnake.snakemultiplayer.gamemodes;

import thusnake.snakemultiplayer.R;

public class Speedy extends Classic {
  @Override
  public void setDifficulty(Difficulty difficulty) {
    super.setDifficulty(difficulty);
    horizontalSquares.set(10 + difficulty.index * 5);
    verticalSquares.set(10 + difficulty.index * 5);
    speed.set(10 + difficulty.index * 5);
    stageBorders.set(false);
  }

  @Override
  public int getThumbnailResourceID() { return R.drawable.gamemode_speedy; }

  @Override
  public String toString() { return "speedy"; }
}
