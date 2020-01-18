package thusnake.snakemultiplayer;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import thusnake.snakemultiplayer.gamemodes.GameMode;

public final class OptionsBuilder {
  public static List<MenuDrawable> defaultOptions(GameMode gameMode) {
    GameRenderer renderer = OpenGLActivity.current.getRenderer();
    List<MenuDrawable> list = new LinkedList<>();
    list.add(MarginSpace.makeHorizontal(renderer, -renderer.getScreenHeight() / 18f, 0));
    list.add(horizontalSquares(gameMode.horizontalSquares));
    list.add(verticalSquares(gameMode.verticalSquares));
    list.add(speed(gameMode.speed));
    list.add(stageBorders(gameMode.stageBorders));
    return list;
  }

  public static List<MenuDrawable> justDifficulty() {
    GameRenderer renderer = OpenGLActivity.current.getRenderer();
    List<MenuDrawable> list = new LinkedList<>();
    list.add(MarginSpace.makeHorizontal(renderer, -renderer.getScreenHeight() / 18f, 0));
    list.add(difficulty());
    return list;
  }


  static MenuNumericalValue horizontalSquares(AtomicInteger mutableValue) {
    GameRenderer renderer = OpenGLActivity.current.getRenderer();
    MenuNumericalValue value = new MenuNumericalValue(renderer,
                                              mutableValue,
                                              renderer.getScreenWidth() - renderer.smallDistance(),
                                              0, MenuDrawable.EdgePoint.TOP_RIGHT);
    value.setValueBoundaries(1, 100);
    addDescriptionItem(value, "Hor Squares");
    return value;
  }

  static MenuNumericalValue verticalSquares(AtomicInteger mutableValue) {
    GameRenderer renderer = OpenGLActivity.current.getRenderer();
    MenuNumericalValue value = new MenuNumericalValue(renderer,
                                              mutableValue,
                                              renderer.getScreenWidth() - renderer.smallDistance(),
                                              0, MenuDrawable.EdgePoint.TOP_RIGHT);
    value.setValueBoundaries(1, 100);
    addDescriptionItem(value, "Ver Squares");
    return value;
  }

  static MenuNumericalValue speed(AtomicInteger mutableValue) {
    GameRenderer renderer = OpenGLActivity.current.getRenderer();
    MenuNumericalValue value = new MenuNumericalValue(renderer,
                                              mutableValue,
                                              renderer.getScreenWidth() - renderer.smallDistance(),
                                              0, MenuDrawable.EdgePoint.TOP_RIGHT);
    value.setValueBoundaries(1, 64);
    addDescriptionItem(value, "Speed");
    return value;
  }

  static MenuBooleanValue stageBorders(AtomicBoolean mutableValue) {
    GameRenderer renderer = OpenGLActivity.current.getRenderer();
    MenuBooleanValue value = new MenuBooleanValue(renderer,
                                              mutableValue,
                                              renderer.getScreenWidth() - renderer.smallDistance(),
                                              0, MenuDrawable.EdgePoint.TOP_RIGHT);
    addDescriptionItem(value, "Stage Borders");
    return value;
  }

  static MenuCustomValue difficulty() {
    GameRenderer renderer = OpenGLActivity.current.getRenderer();
    List<String> strings = new LinkedList<>();
    strings.add(GameSetupBuffer.difficultyToString(0));
    strings.add(GameSetupBuffer.difficultyToString(1));
    strings.add(GameSetupBuffer.difficultyToString(2));
    strings.add(GameSetupBuffer.difficultyToString(3));
    strings.add(GameSetupBuffer.difficultyToString(4));
    MenuCustomValue value = new MenuCustomValue(renderer, strings, renderer.getScreenWidth() / 2f,
        0, MenuDrawable.EdgePoint.TOP_CENTER) {
      @Override
      public void move(double dt) {
        super.move(dt);
        setValue(renderer.getMenu().getSetupBuffer().gameMode.getDifficulty().toString());
      }

      @Override
      public void onValueChange(String newValue) {
        super.onValueChange(newValue);
        switch(newValue) {
          case "Mild":
            renderer.getMenu().getSetupBuffer().gameMode.setDifficulty(GameMode.Difficulty.MILD);
            break;
          case "Fair":
            renderer.getMenu().getSetupBuffer().gameMode.setDifficulty(GameMode.Difficulty.FAIR);
            break;
          case "Tough":
            renderer.getMenu().getSetupBuffer().gameMode.setDifficulty(GameMode.Difficulty.TOUGH);
            break;
          case "Bonkers":
            renderer.getMenu().getSetupBuffer().gameMode.setDifficulty(GameMode.Difficulty.BONKERS);
            break;
          case "Ultimate":
            renderer.getMenu().getSetupBuffer().gameMode.setDifficulty(GameMode.Difficulty.ULTIMATE);
            break;
        }
      }
    }.setLabel("Difficulty:");
    addDescriptionItem(value, "");

    return value;
  }



  public static MenuFlexContainer addDescriptionItem(MenuFlexContainer value, String description) {
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

    return value;
  }
}
