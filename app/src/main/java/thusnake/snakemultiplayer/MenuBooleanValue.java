package thusnake.snakemultiplayer;

import java.util.concurrent.atomic.AtomicBoolean;

public class MenuBooleanValue extends MenuFlexContainer {
  private AtomicBoolean value;
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

    this.value = new AtomicBoolean();
    setValue(value);
    addItem(item);
  }

  public MenuBooleanValue(GameRenderer renderer, AtomicBoolean value, float x, float y,
                          EdgePoint alignPoint) {
    this(renderer, value.get(), x, y, alignPoint);
    this.value = value;
  }

  public void switchValue() {
    value.set(!value.get());
    onValueChange(value.get());
  }

  public void setValue(boolean newValue) {
    value.set(newValue);
    onValueChange(value.get());
  }

  public void onValueChange(boolean newValue) {
    item.setText(newValue ? "Y" : "N");
  }
}
