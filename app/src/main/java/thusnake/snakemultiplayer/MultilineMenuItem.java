package thusnake.snakemultiplayer;

import com.android.texample.GLText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultilineMenuItem extends MenuDrawable {
  private String text;
  private MenuItem.Alignment alignment;
  private float wrapWidth, x, y;
  private final GLText glText;
  private final List<MenuItem> lines = new ArrayList<>();

  public MultilineMenuItem(GameRenderer renderer, String text, float x, float y,
                           MenuItem.Alignment align, float wrapWidth) {
    super(renderer, x, y);
    this.text = text;
    this.x = x;
    this.y = y;
    this.alignment = align;
    this.wrapWidth = wrapWidth;

    this.glText = renderer.getGlText();

    this.generateLines();
  }

  public void draw() {
    for (MenuItem line : lines)
      line.draw();
  }

  public void move(double dt) {
    for (MenuItem line : lines)
      line.move(dt);
  }

  public void setText(String text) {
    this.text = text;
    this.generateLines();
  }

  public void setAction(MenuAction action) {
    for (MenuItem line : lines)
      line.setAction(action);
  }

  public void performAction() {
    for (MenuItem line : lines)
      line.performAction();
  }

  public void setDestinationX(double destinationX) {
    for (MenuItem line : lines)
      line.setDestinationX(destinationX);
  }

  public void setDestinationY(double destinationY) {
    for (MenuItem line : lines)
      line.setDestinationY(destinationY - lines.indexOf(line) * glText.getHeight() * 0.65f);
  }

  public void setDestinationXFromOrigin(double offsetX) {
    for (MenuItem line : lines)
      line.setDestinationXFromOrigin(offsetX);
  }

  public void setDestinationYFromOrigin(double offsetY) {
    for (MenuItem line : lines)
      line.setDestinationYFromOrigin(offsetY);
  }

  public void setDestinationToInitial() {
    for (MenuItem line : lines)
      line.setDestinationToInitial();
  }

  public float getY() {
    return lines.get(lines.size()-1).getY();
  }

  private void generateLines() {
    // Clear the list to start over.
    lines.clear();

    // Create a list of words.
    List<String> remainingWords = new ArrayList<>();
    Collections.addAll(remainingWords, text.split(" "));

    while(!remainingWords.isEmpty()) {
      // Each line has at least 1 word.
      String currentLine = remainingWords.remove(0);

      // Add more if possible.
      while (!remainingWords.isEmpty() &&
             glText.getLength(currentLine + " " + remainingWords.get(0)) <= wrapWidth) {
        currentLine += " " + remainingWords.remove(0);
      }

      // Add the line as a MenuItem to an array.
      lines.add(new MenuItem(renderer,
                             currentLine,
                             x,
                             y - lines.size() * glText.getHeight() * 0.65f,
                             alignment));
    }

    // Figure out the width of the whole.
    float maxWidth = 0;
    for (MenuItem line : lines)
      if (line.getWidth() > maxWidth)
        maxWidth = line.getWidth();

    this.setWidth(maxWidth);

    // Figure out the height of the whole.
    this.setHeight(lines.get(0).getY() + lines.get(0).getHeight()
                   - lines.get(lines.size()-1).getY());
  }
}
