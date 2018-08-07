package thusnake.snakemultiplayer;

public class MenuBooleanValue extends MenuFlexContainer {
  private boolean value;
  private final MenuItem item;

  public MenuBooleanValue(GameRenderer renderer, boolean value, float x, float y,
                          EdgePoint alignPoint) {
    super(renderer, alignPoint);

    item = new MenuItem(renderer, Boolean.toString(value), x, y, alignPoint) {
      @Override
      public void performAction() {
        super.performAction();
        switchValue();
      }
    };

    setValue(value);
    addItem(item);
  }

  public void switchValue() {
    value = !value;
    onValueChange(value);
  }

  public void setValue(boolean newValue) {
    value = newValue;
    onValueChange(value);
  }

  public void onValueChange(boolean newValue) {
    item.setText(newValue ? "Y" : "N");
  }
}
