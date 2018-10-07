package thusnake.snakemultiplayer;

import android.view.MotionEvent;

import com.android.texample.GLText;

import java.util.concurrent.atomic.AtomicInteger;

public class MenuNumericalValue extends MenuFlexContainer {
  private GLText glText;
  private final MenuItem item;
  private AtomicInteger value;
  private double valueMinuteOffset = 0;
  private int maxValue, minValue;
  private final MenuButton plusButton, minusButton;
  private boolean expanded = false;
  private SimpleTimer expansionTimer = new SimpleTimer(0.0);

  public MenuNumericalValue(GameRenderer renderer, int value, float x, float y,
                            EdgePoint alignPoint) {
    super(renderer, alignPoint);
    this.value = new AtomicInteger(value);
    this.glText = renderer.getGlText();

    item = new MenuItem(renderer, Integer.toString(value), x, y, alignPoint) {
      @Override
      public void performAction() {
        if (!expanded) expand();
      }
    };

    plusButton = new MenuButton(renderer,
                                item.getX(EdgePoint.TOP_CENTER),
                                item.getY(EdgePoint.TOP_CENTER),
                                glText.getHeight(), glText.getHeight(), EdgePoint.TOP_RIGHT) {
      @Override
      public void onButtonCreated() {
        addItem(new MenuItem(renderer, "+", getX(EdgePoint.CENTER), getY(EdgePoint.CENTER),
                             EdgePoint.CENTER));
      }

      @Override
      public void move(double dt) {
        super.move(dt);
        setX(item.getX(EdgePoint.TOP_CENTER));
        setY(item.getY(EdgePoint.TOP_CENTER) - item.getHeight() * expansionTimer.getTime());
      }

      @Override
      public void performAction() {
        if (getHoldDuration() < 1) offsetValue(1);
      }
    };

    minusButton = new MenuButton(renderer,
                                 item.getX(EdgePoint.TOP_CENTER),
                                 item.getY(EdgePoint.TOP_CENTER),
                                 glText.getHeight(), glText.getHeight(), EdgePoint.TOP_LEFT) {
      @Override
      public void onButtonCreated() {
        addItem(new MenuItem(renderer, "-", getX(EdgePoint.CENTER), getY(EdgePoint.CENTER),
                             EdgePoint.CENTER));
      }

      @Override
      public void move(double dt) {
        super.move(dt);
        setX(item.getX(EdgePoint.TOP_CENTER));
        setY(item.getY(EdgePoint.TOP_CENTER) - item.getHeight() * expansionTimer.getTime());
      }

      @Override
      public void performAction() {
        if (getHoldDuration() < 1) offsetValue(-1);
      }
    };

    plusButton.setEnabled(false);
    minusButton.setEnabled(false);

    plusButton.scale.setTime(0);
    minusButton.scale.setTime(0);

    addItem(item);
    addItem(plusButton);
    addItem(minusButton);
  }

  public MenuNumericalValue(GameRenderer renderer, AtomicInteger value, float x, float y,
                            EdgePoint alignPoint) {
    this(renderer, value.get(), x, y, alignPoint);
    this.value = value;
  }

  @Override
  public void move(double dt) {
    super.move(dt);

    item.setText(value.toString());

    if (!expansionTimer.isDone()) expansionTimer.countEaseOut(dt, 8, dt);

    // Update value when holding down the plus or minus buttons.
    if (plusButton.getHoldDuration() > 0.3)
      offsetValue(Math.min(Math.pow(2, plusButton.getHoldDuration() + 2) * dt, 0.5));
    else if (minusButton.getHoldDuration() > 0.3)
      offsetValue(Math.max(-Math.pow(2, minusButton.getHoldDuration() + 2) * dt, -0.5
      ));
  }

  @Override
  public void onMotionEvent(MotionEvent event, float[] pointerX, float[] pointerY) {
    super.onMotionEvent(event, pointerX, pointerY);

    // Retract automatically if something else has been clicked.
    if (event.getActionMasked() == MotionEvent.ACTION_UP && !isClicked(pointerX[0], pointerY[0]))
      retract();

    // Round the value on plus/minus button release.
    if (event.getActionMasked() == MotionEvent.ACTION_UP)
      valueMinuteOffset = 0;
  }

  public void setValue(int value) {
    double prevValue = this.value.get();

    if (minValue != maxValue) {
      if (value < minValue) value = minValue;
      if (value > maxValue) value = maxValue;
    }

    this.value.set(value);
    item.setText(Integer.toString(value));
    if (prevValue != value) onValueChange(value);
  }

  private void offsetValue(double offset) {
    valueMinuteOffset += offset;
    if (valueMinuteOffset >= 1 || valueMinuteOffset <= 1) {
      int wholeNumberTransfer = valueMinuteOffset >= 1 ? (int) Math.floor(valueMinuteOffset)
                                                       : (int) Math.ceil(valueMinuteOffset);
      valueMinuteOffset -= wholeNumberTransfer;
      setValue(value.get() + wholeNumberTransfer);
    }
  }

  public void onValueChange(int newValue) {}

  /**
   * Sets a restriction for the value via upper and lower boundaries.
   * Setting two equivalent boundary values removes restrictions on the value of the item.
   * @param minValue The lowest value this item can have.
   * @param maxValue The highest value this item can have.
   */
  public void setValueBoundaries(int minValue, int maxValue) {
    this.minValue = minValue;
    this.maxValue = maxValue;
  }

  public int getValue() { return value.get(); }

  public void expand() {
    if (!expanded) {
      expanded = true;

      plusButton.setEnabled(true);
      minusButton.setEnabled(true);

      plusButton.scale.setEndTimeFromNow(1);
      minusButton.scale.setEndTimeFromNow(1);

      expansionTimer.setEndTimeFromNow(1);
    }
  }

  public void retract() {
    if (expanded) {
      expanded = false;

      plusButton.setEnabled(false);
      minusButton.setEnabled(false);

      plusButton.scale.setEndTimeFromNow(0);
      minusButton.scale.setEndTimeFromNow(0);

      expansionTimer.setEndTimeFromNow(0);
    }
  }
}
