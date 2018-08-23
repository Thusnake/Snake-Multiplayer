package thusnake.snakemultiplayer;

import java.util.List;

public class MenuCustomValue extends MenuFlexContainer {
  private List<String> possibleValues;
  private String value;
  private final MenuItem textItem, leftButton, rightButton;
  private boolean cyclic;

  public MenuCustomValue(GameRenderer renderer, List<String> possibleValues, float x, float y,
                         EdgePoint alignPoint) {
    super(renderer, alignPoint);
    this.possibleValues = possibleValues;

    value = possibleValues.get(0);

    // Assess the widest string value.
    String widestValue = "";
    for (String value : possibleValues)
      if (renderer.getGlText().getLength(value) > renderer.getGlText().getLength(widestValue))
        widestValue = value;

    // When creating the text item set its text to the widest value at first. This is so we can
    // easily decide the left and right arrows' positions.
    textItem = new MenuItem(renderer, widestValue, x, y, alignPoint);
    leftButton = new MenuItem(renderer, "<", textItem.getX(EdgePoint.LEFT_CENTER) - 20,
                              textItem.getY(EdgePoint.CENTER), EdgePoint.RIGHT_CENTER) {
      @Override
      public void performAction() {
        x.setTime(x.getInitialTime());
        setAnimation(new MenuAnimation(leftButton)
                    .addKeyframe(new MoveKeyframe(0.05, getX() - 20, getY(), BezierTimer.easeInOut))
                    .addKeyframe(new MoveKeyframe(0.05, getX(), getY(), BezierTimer.easeInOut)));
        cycleValue(-1);
      }

      @Override
      public void move(double dt) {
        super.move(dt);
        if (!cyclic && possibleValues.indexOf(value) == 0)
          setEnabled(false);
        else if (!cyclic)
          setEnabled(true);
      }
    };
    rightButton = new MenuItem(renderer, ">", textItem.getX(EdgePoint.RIGHT_CENTER) + 20,
                               textItem.getY(EdgePoint.CENTER), EdgePoint.LEFT_CENTER) {
      @Override
      public void performAction() {
        x.setTime(x.getInitialTime());
        setAnimation(new MenuAnimation(rightButton)
            .addKeyframe(new MoveKeyframe(0.05, getX() + 20, getY(), BezierTimer.easeInOut))
            .addKeyframe(new MoveKeyframe(0.05, getX(), getY(), BezierTimer.easeInOut)));
        cycleValue(1);
      }

      @Override
      public void move(double dt) {
        super.move(dt);
        if (!cyclic && possibleValues.indexOf(value) == possibleValues.size() - 1)
          setEnabled(false);
        else if (!cyclic)
          setEnabled(true);
      }
    };

    // After we're done creating the left and right arrows we can set the text item's value to the
    // proper default one.
    textItem.setText(possibleValues.get(0));

    setWidth(rightButton.getX(EdgePoint.RIGHT_CENTER) - leftButton.getX(EdgePoint.LEFT_CENTER));
    setHeight(renderer.getGlText().getHeight());

    addItem(textItem);
    addItem(leftButton);
    addItem(rightButton);

    move(0);
  }

  public MenuCustomValue cyclic() { cyclic = true; return this;}

  private void cycleValue(int steps) {
    int currentIndex = possibleValues.indexOf(value);
    if (steps > 0) {
      for (int i = 0; i < steps; i++)
        if (++currentIndex >= possibleValues.size())
          currentIndex = cyclic ? 0 : possibleValues.size() - 1;
    } else {
      for (int i = 0; i < -steps; i++)
        if (--currentIndex < 0)
          currentIndex = cyclic ? possibleValues.size() - 1 : 0;
    }
    setValue(possibleValues.get(currentIndex));
  }

  public void setValue(String newValue) {
    for (String someValue : possibleValues)
      if (someValue.equals(newValue)) {
        value = newValue;
        onValueChange(newValue);
        return;
      }
  }

  public void onValueChange(String newValue) {
    textItem.setText(newValue);
  }
}
