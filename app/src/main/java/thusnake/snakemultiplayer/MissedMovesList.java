package thusnake.snakemultiplayer;

import java.util.ArrayList;
import java.util.List;

public class MissedMovesList {
  private ArrayList<byte[]> movesList = new ArrayList<>();
  private int firstMoveIndex;

  public MissedMovesList(int currentLastIndex, int receivedLastIndex, byte[] receivedMove) {
    this.firstMoveIndex = currentLastIndex + 1;
    this.expand(receivedLastIndex, receivedMove);
  }

  public void expand(int expandToIndex) {
    for (int index = firstMoveIndex + movesList.size(); index < expandToIndex; index++)
      this.movesList.add(null);
  }

  public void expand(int expandToIndex, byte[] lastMove) {
    for (int index = firstMoveIndex + movesList.size(); index < expandToIndex - 1; index++)
      this.movesList.add(null);
    this.movesList.add(lastMove);
  }

  public byte[] extractFirst() { return movesList.remove(0); }

  public boolean firstIsReady() { return movesList.get(0) != null; }

  public boolean isFull() {
    for (byte[] move : movesList)
      if (move == null)
        return false;
    return true;
  }

  public int size() { return movesList.size(); }

  public List<Integer> missingMovesIndeces() {
    List<Integer> indeces = new ArrayList<>();
    for (int index = 0; index < movesList.size(); index++)
      if (movesList.get(index) == null)
        indeces.add(firstMoveIndex + index);
    return indeces;
  }

}
