package thusnake.snakemultiplayer;

import java.util.List;

public class MenuCustomValue extends MenuFlexContainer {
  private List<String> possibleValues;
  private String value;
  private MenuItem item;

  public MenuCustomValue(GameRenderer renderer, List<String> possibleValues, float x, float y,
                         EdgePoint alignPoint) {
    super(renderer, alignPoint);
    this.possibleValues = possibleValues;

    value = possibleValues.get(0);
    item = new MenuItem(renderer, value, x, y, alignPoint) {
      @Override
      public void performAction() {
        super.performAction();
        cycleValue();
      }
    };

    addItem(item);
  }

  private void cycleValue() {
    int currentIndex = possibleValues.indexOf(value);
    if (++currentIndex >= possibleValues.size()) currentIndex = 0;
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
    item.setText(newValue);
  }
}
