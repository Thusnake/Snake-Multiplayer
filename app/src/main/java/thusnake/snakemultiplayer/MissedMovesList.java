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

  public byte extractFirst() { return movesList.remove(0)[3]; }

  public boolean firstIsReady() { return movesList.get(0) != null; }

  public boolean isFull() {
    for (byte[] move : movesList)
      if (move == null)
        return false;
    return true;
  }

  public int size() { return movesList.size(); }

  public List<Integer> missingMovesIndices() {
    List<Integer> indices = new ArrayList<>();
    for (int index = 0; index < movesList.size(); index++)
      if (movesList.get(index) == null)
        indices.add(firstMoveIndex + index);
    return indices;
  }

  public boolean insert(int moveNumber, byte[] move) {
    int relativeMoveNumber = moveNumber + firstMoveIndex;
    if (movesList.get(relativeMoveNumber) == null) {
      movesList.set(relativeMoveNumber, move);
      return true;
    }
    return false;
  }
}
