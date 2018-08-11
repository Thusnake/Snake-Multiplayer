package thusnake.snakemultiplayer;

import java.util.LinkedList;
import java.util.List;

final class OptionsBuilder {
  static List<MenuDrawable> defaultOptions(GameRenderer renderer) {
    List<MenuDrawable> list = new LinkedList<>();
    list.add(horizontalSquares(renderer));
    list.add(verticalSquares(renderer));
    list.add(speed(renderer));
    list.add(stageBorders(renderer));
    return list;
  }

  static List<MenuDrawable> justDifficulty(GameRenderer renderer) {
    List<MenuDrawable> list = new LinkedList<>();
    list.add(difficulty(renderer));
    return list;
  }


  static MenuNumericalValue horizontalSquares(GameRenderer renderer) {
    MenuNumericalValue value = new MenuNumericalValue(renderer,
                                              renderer.getMenu().getSetupBuffer().horizontalSquares,
                                              renderer.getScreenWidth() - 10,
                                              0, MenuDrawable.EdgePoint.TOP_RIGHT) {
      @Override
      public void onValueChange(int newValue) {
        super.onValueChange(newValue);
        renderer.getMenu().getSetupBuffer().horizontalSquares = newValue;
      }
    };
    value.setValueBoundaries(1, 100);
    addDescriptionItem(value, "hor squares");
    return value;
  }

  static MenuNumericalValue verticalSquares(GameRenderer renderer) {
    MenuNumericalValue value = new MenuNumericalValue(renderer,
                                                renderer.getMenu().getSetupBuffer().verticalSquares,
                                                renderer.getScreenWidth() - 10,
                                                0, MenuDrawable.EdgePoint.TOP_RIGHT) {
      @Override
      public void onValueChange(int newValue) {
        super.onValueChange(newValue);
        renderer.getMenu().getSetupBuffer().verticalSquares = newValue;
      }
    };
    value.setValueBoundaries(1, 100);
    addDescriptionItem(value, "ver squares");
    return value;
  }

  static MenuNumericalValue speed(GameRenderer renderer) {
    MenuNumericalValue value = new MenuNumericalValue(renderer,
                                                      renderer.getMenu().getSetupBuffer().speed,
                                                      renderer.getScreenWidth() - 10,
                                                      0, MenuDrawable.EdgePoint.TOP_RIGHT) {
      @Override
      public void onValueChange(int newValue) {
        super.onValueChange(newValue);
        renderer.getMenu().getSetupBuffer().speed = newValue;
      }
    };
    value.setValueBoundaries(1, 64);
    addDescriptionItem(value, "speed");
    return value;
  }

  static MenuBooleanValue stageBorders(GameRenderer renderer) {
    MenuBooleanValue value = new MenuBooleanValue(renderer,
                                                  renderer.getMenu().getSetupBuffer().stageBorders,
                                                  renderer.getScreenWidth() - 10,
                                                  0, MenuDrawable.EdgePoint.TOP_RIGHT) {
      @Override
      public void onValueChange(boolean newValue) {
        super.onValueChange(newValue);
        renderer.getMenu().getSetupBuffer().stageBorders = newValue;
      }
    };
    addDescriptionItem(value, "wall death");
    return value;
  }

  static MenuCustomValue difficulty(GameRenderer renderer) {
    List<String> strings = new LinkedList<>();
    strings.add(GameSetupBuffer.difficultyToString(0));
    strings.add(GameSetupBuffer.difficultyToString(1));
    strings.add(GameSetupBuffer.difficultyToString(2));
    strings.add(GameSetupBuffer.difficultyToString(3));
    strings.add(GameSetupBuffer.difficultyToString(4));
    MenuCustomValue value = new MenuCustomValue(renderer, strings, renderer.getScreenWidth() - 10,
        0, MenuDrawable.EdgePoint.TOP_RIGHT) {
      @Override
      public void onValueChange(String newValue) {
        super.onValueChange(newValue);
        switch(newValue) {
          case "Mild":
            renderer.getMenu().getSetupBuffer().difficulty = 0; break;
          case "Fair":
            renderer.getMenu().getSetupBuffer().difficulty = 1; break;
          case "Tough":
            renderer.getMenu().getSetupBuffer().difficulty = 2; break;
          case "Bonkers":
            renderer.getMenu().getSetupBuffer().difficulty = 3; break;
          case "Ultimate":
            renderer.getMenu().getSetupBuffer().difficulty = 4; break;
        }
      }
    };
    addDescriptionItem(value, "");

    return value;
  }



  private static void addDescriptionItem(MenuFlexContainer value, String description) {
    value.addItem(new MenuItem(value.renderer, description, 10,
                               value.getY(MenuDrawable.EdgePoint.CENTER),
                               MenuDrawable.EdgePoint.LEFT_CENTER) {
      @Override
      public void move(double dt) {
        super.move(dt);
        y.setTime(value.getY(EdgePoint.CENTER));
      }

      @Override
      public void performAction() {
        super.performAction();

        if (value instanceof MenuNumericalValue)
          ((MenuNumericalValue) value).expand();
      }
    });
  }
}
