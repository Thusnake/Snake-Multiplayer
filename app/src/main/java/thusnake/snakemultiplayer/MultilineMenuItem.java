package thusnake.snakemultiplayer;

import com.android.texample.GLText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultilineMenuItem extends MenuDrawable {
  private String text;
  private float wrapWidth;
  private final GLText glText;
  private final List<MenuItem> lines = new ArrayList<>();

  public MultilineMenuItem(GameRenderer renderer, String text, float x, float y,
                           EdgePoint alignPoint, EdgePoint originPoint, float wrapWidth) {
    super(renderer, x, y, alignPoint, originPoint);
    this.text = text;
    this.wrapWidth = wrapWidth;

    this.glText = renderer.getGlText();

    this.generateLines();
  }

  public MultilineMenuItem(GameRenderer renderer, String text, float x, float y,
                           EdgePoint alignPoint, float wrapWidth) {
    this(renderer, text, x, y, alignPoint, EdgePoint.CENTER, wrapWidth);
  }

  public void draw(float[] parentColors) {
    if (isDrawable()) {
      gl.glPushMatrix();
      gl.glTranslatef(getX(originPoint), getY(originPoint), 0);
      gl.glScalef((float) scale.getTime(), (float) scale.getTime(), 0);
      gl.glTranslatef(-getX(originPoint), -getY(originPoint), 0);

      for (MenuItem line : lines)
        line.draw(parentColors);

      gl.glPopMatrix();
    }
  }

  public void move(double dt) {
    super.move(dt);
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
      line.setDestinationY(destinationY - lines.indexOf(line) * glText.getHeight());
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

  public float getBottomY() {
    return lines.get(lines.size()-1).getBottomY();
  }

  private void generateLines() {
    // Clear the list to start over.
    lines.clear();

    // Create a list of words.
    List<String> remainingWords = new ArrayList<>();
    Collections.addAll(remainingWords, text.split(" "));

    List<String> generatedLines = new ArrayList<>();

    while(!remainingWords.isEmpty()) {
      // Each line has at least 1 word.
      String currentLine = remainingWords.remove(0);

      // Add more if possible.
      while (!remainingWords.isEmpty() &&
             glText.getLength(currentLine + " " + remainingWords.get(0)) <= wrapWidth) {
        currentLine += " " + remainingWords.remove(0);
      }

      // Add the line to an array.
      generatedLines.add(currentLine);
    }

    // Figure out the width of the whole.
    float maxWidth = 0;
    for (String line : generatedLines)
      if (glText.getLength(line) > maxWidth)
        maxWidth = glText.getLength(line);

    this.setWidth(maxWidth);

    // Figure out the height of the whole.
    this.setHeight(glText.getHeight() * generatedLines.size());

    // Create all the MenuItems so that they fit this container's width and height.
    for (String line : generatedLines)
      /* Adding MenuItems is done by getting from the raw originPoint to the raw alignPoint and
      creating a new MenuItem there with the same alignPoint, except shifted to the top. That
      way we can draw multiple lines of top-aligned MenuItems within the boundaries of this item. */

      lines.add(new MenuItem
          (renderer,
           line,
           getX(alignPoint),
           getY(EdgePoint.TOP_CENTER) - lines.size() * glText.getHeight(),
           combineEdgeHalves(EdgePoint.TOP_CENTER, alignPoint)));
  }
}
