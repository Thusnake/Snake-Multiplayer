package thusnake.snakemultiplayer;

import com.android.texample.GLText;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Nick on 08/01/2018.
 */

public class MenuValue extends MenuDrawable {
  private int valueInteger;
  private boolean valueBoolean;
  private String valueString;
  private final MenuItem.Alignment align;
  public enum Type {INTEGER, BOOLEAN, STRING}
  private Type type;
  private boolean expanded, visible = true;
  private MenuItem plusButton, minusButton;
  private SimpleTimer plusMinusOpacity = new SimpleTimer(0.0, 1.0);
  private GLText glText;
  private float width, height;
  private enum Holding {NOTHING, PLUS, MINUS}
  private Holding holding = Holding.NOTHING;
  private SimpleTimer holdTimer = new SimpleTimer(0.0, 2.0);
  private double valueIntegerMinute;
  private int minimumValueInteger = 0, maximumValueInteger = 0;

  // Constructors for all types.
  public MenuValue(GameRenderer renderer, int initialValue, float x, float y,
                   MenuItem.Alignment align) {
    this(renderer, x, y, align);
    this.valueInteger = initialValue;
    this.type = Type.INTEGER;
    this.initDimensions(x, y);
    this.plusButton = new MenuItem(renderer, "+", x, y, align);
    this.minusButton = new MenuItem(renderer, "-", x - this.plusButton.getWidth(), y, align);
  }

  public MenuValue(GameRenderer renderer, boolean initialValue, float x, float y,
                   MenuItem.Alignment align) {
    this(renderer, x, y, align);
    this.valueBoolean = initialValue;
    this.type = Type.BOOLEAN;
    this.initDimensions(x, y);
  }

  public MenuValue(GameRenderer renderer, String initialValue, float x, float y,
                   MenuItem.Alignment align) {
    this(renderer, x, y, align);
    this.valueString = initialValue;
    this.type = Type.STRING;
    this.initDimensions(x, y);
  }

  // All constructors share some code, so have a private one handle the shared code.
  private MenuValue(GameRenderer renderer, float x, float y, MenuItem.Alignment align) {
    super(renderer, x, y);
    this.align = align;
    this.glText = renderer.getGlText();
  }

  private void initDimensions(float x, float y) {
    this.width = glText.getLength(this.getValueToString());
    this.height = glText.getCharHeight() * 0.65f;
    if (align == MenuItem.Alignment.LEFT) this.setX(x);
    else this.setX(x - this.width);
    this.setY(y);
  }

  // Setters for all types.
  public void setValue(int newValue) {
    if (this.type == Type.INTEGER) {
      if (this.minimumValueInteger != this.maximumValueInteger) {
        if (newValue < this.minimumValueInteger) newValue = this.minimumValueInteger;
        else if (newValue > this.maximumValueInteger) newValue = this.maximumValueInteger;
      }

      if (this.align == MenuItem.Alignment.RIGHT)
        this.getXTimer().countDown(glText.getLength(this.getValueToString(newValue))
                                 - glText.getLength(this.getValueToString()));
      this.width = glText.getLength(this.getValueToString(newValue));
      this.valueInteger = newValue;
      this.renderer.getMenu().syncValues();
    }
  }
  public void setValue(boolean newValue) {
    if (this.type == Type.BOOLEAN) {
      if (this.align == MenuItem.Alignment.RIGHT)
        this.getXTimer().countDown(glText.getLength(this.getValueToString(newValue))
                                 - glText.getLength(this.getValueToString()));
      this.width = glText.getLength(this.getValueToString(newValue));
      this.valueBoolean = newValue;
      this.renderer.getMenu().syncValues();
    }
  }
  public void setValue(String newValue) {
    if (this.type == Type.STRING) {
      if (this.align == MenuItem.Alignment.RIGHT)
        this.getXTimer().countDown(glText.getLength(newValue)
                                 - glText.getLength(this.getValueToString()));
      this.width = glText.getLength(newValue);
      this.valueString = newValue;
      this.renderer.getMenu().syncValues();
    }
  }

  public void draw() {
    if (this.visible) {
      gl.glColor4f(this.getColors()[0],this.getColors()[1],this.getColors()[2],this.getColors()[3]);
      glText.draw(this.getValueToString(), this.getX(), this.getY());
      if (this.type == Type.INTEGER && this.expanded) {
        minusButton.draw();
        plusButton.draw();
      }
      glText.end();
      glText.begin();
    }
  }

  public void move(double dt) {
    if (!this.getXTimer().isDone()) this.getXTimer().countEaseOut(dt, 8, this.height * 2);
    if (!this.getYTimer().isDone()) this.getYTimer().countEaseOut(dt, 8, this.height * 2);
    if (this.type == Type.INTEGER) {
      plusButton.move(dt);
      minusButton.move(dt);
    }

    // These are not movement related, but consider the "move" methods as "update" methods as well.
    // Plus/minus buttons hold handling.
    if (this.holding != Holding.NOTHING) {
      if (!this.holdTimer.isDone()) this.holdTimer.count(dt);
      if (this.holding == Holding.PLUS)
        this.increaseValueMinutely(Math.pow(2, this.holdTimer.getTime() + 2) * dt);
      else if (this.holding == Holding.MINUS)
        this.decreaseValueMinutely(Math.pow(2, this.holdTimer.getTime() + 2) * dt);
    }
    // Plus/minus buttons opacity.
    if (this.expanded && !this.plusMinusOpacity.isDone()) {
      this.plusMinusOpacity.countEaseOut(dt,8,1);
      this.plusButton.setOpacity((float) this.plusMinusOpacity.getTime());
      this.minusButton.setOpacity((float) this.plusMinusOpacity.getTime());
    }
  }

  // Getters for all types.
  public int getValueInteger() { return this.valueInteger; }
  public boolean getValueBoolean() { return this.valueBoolean; }
  public String getValueString() { return this.valueString; }
  public String getValueToString() {
    switch (this.type) {
      case INTEGER: return "" + this.valueInteger;
      case BOOLEAN: return (this.valueBoolean) ? "y" : "n";
      case STRING: return this.valueString;
      default: return "";
    }
  }
  public String getValueToString(int value) { return "" + value; }
  public String getValueToString(boolean value) { return (value) ? "y" : "n"; }

  // Integer-specific methods.
  public void increaseValueMinutely(double amount) {
    if (this.type == Type.INTEGER) {
      this.valueIntegerMinute += amount;
      while(this.valueIntegerMinute > 1) {
        this.setValue(this.valueInteger + 1);
        this.valueIntegerMinute -= 1;
      }
    }
  }
  public void decreaseValueMinutely(double amount) {
    if (this.type == Type.INTEGER) {
      this.valueIntegerMinute += amount;
      while(this.valueIntegerMinute > 1) {
        this.setValue(this.valueInteger - 1);
        this.valueIntegerMinute -= 1;
      }
    }
  }
  public void setExpanded(boolean expanded) {
    if(this.type == Type.INTEGER) {
      this.expanded = expanded;
      if (expanded) {
        this.setDestinationToInitial();
        this.minusButton.setDestinationYFromOrigin(-this.height);
        this.plusButton.setDestinationYFromOrigin(-this.height);
      } else {
        this.setDestinationToInitial();
        this.minusButton.setDestinationYFromOrigin(0);
        this.plusButton.setDestinationYFromOrigin(0);
        this.plusMinusOpacity.reset();
      }
    }
  }
  public void setBoundaries(int min, int max) {
    if (this.type == Type.INTEGER) {
      this.minimumValueInteger = min;
      this.maximumValueInteger = max;
    }
  }

  // Plus and minus buttons handlers.
  public void handleButtonsDown(float x, float y) {
    if (this.plusButton.isClicked(x, y)) {
      this.setValue(this.valueInteger + 1);
      this.holding = Holding.PLUS;
    } else if (this.minusButton.isClicked(x, y)) {
      this.setValue(this.valueInteger - 1);
      this.holding = Holding.MINUS;
    }
  }
  public void handleButtonsMove(float x, float y) {
    if (this.holding == Holding.PLUS && !this.plusButton.isClicked(x, y) ||
        this.holding == Holding.MINUS && !this.minusButton.isClicked(x, y)) {
      this.holding = Holding.NOTHING;
      this.holdTimer.reset();
      this.valueIntegerMinute = 0;
    }
  }
  public void handleButtonsUp() {
    this.holding = Holding.NOTHING;
    this.holdTimer.reset();
    this.valueIntegerMinute = 0;
  }

  // Movement methods.
  public void setDestinationYFromOrigin(double offsetY) {
    super.setDestinationYFromOrigin(offsetY);
    if (this.type == Type.INTEGER) {
      this.minusButton.setDestinationYFromOrigin(offsetY);
      this.plusButton.setDestinationYFromOrigin(offsetY);
    }
  }

  // Other getters.
  public boolean isExpanded() { return this.expanded; }
  public MenuItem getPlusButton() { return this.plusButton; }
  public MenuItem getMinusButton() { return this.minusButton; }
  public Type getType() { return this.type; }

  public void setVisible(boolean visible) { this.visible = visible; }

}
