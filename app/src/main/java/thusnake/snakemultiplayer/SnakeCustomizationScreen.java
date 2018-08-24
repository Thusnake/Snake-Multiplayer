package thusnake.snakemultiplayer;

import java.util.LinkedList;
import java.util.List;

public abstract class SnakeCustomizationScreen extends MenuScreen {
  final Player player;
  private final MenuCarousel snakeSelectionCarousel;
  private final SnakeCustomizationScreen reference = this;

  public SnakeCustomizationScreen(Menu menu, Player player) {
    super(menu);
    this.player = player;

    snakeSelectionCarousel = new MenuCarousel(renderer, 0, backButton.getBottomY(),
                                              renderer.getScreenWidth(),
                                              renderer.getScreenHeight() / 2f,
                                              MenuDrawable.EdgePoint.TOP_LEFT);
    snakeSelectionCarousel.addChoice(meshAsCarouselItem(0, "White"));
    snakeSelectionCarousel.addChoice(meshAsCarouselItem(1, "Red"));
    snakeSelectionCarousel.addChoice(meshAsCarouselItem(2, "Orange"));
    snakeSelectionCarousel.addChoice(meshAsCarouselItem(3, "Yellow"));
    snakeSelectionCarousel.addChoice(meshAsCarouselItem(4, "Green"));
    snakeSelectionCarousel.addChoice(meshAsCarouselItem(5, "Teal"));
    snakeSelectionCarousel.addChoice(meshAsCarouselItem(6, "Blue"));
    snakeSelectionCarousel.addChoice(meshAsCarouselItem(7, "Pink"));
    snakeSelectionCarousel.setDefaultChoice(player.getColorIndex());
    snakeSelectionCarousel.noBoundaries();
    snakeSelectionCarousel.notChosenOpacity = 0.5f;
    snakeSelectionCarousel.confirmChoices();

    List<String> possibleControlTypes = new LinkedList<>();
    possibleControlTypes.add("Corner");
    possibleControlTypes.add("Swipe");
    possibleControlTypes.add("Gamepad");
    possibleControlTypes.add("Keyboard");
    MenuCustomValue controlType
        = new MenuCustomValue(renderer, possibleControlTypes,
                              renderer.getScreenWidth() / 2f,
                              snakeSelectionCarousel.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER)/2f,
                              MenuDrawable.EdgePoint.CENTER) {
      @Override
      public void move(double dt) {
        super.move(dt);
        setValue(player.getPlayerController().identifier());
      }

      @Override
      public void onValueChange(String newValue) {
        super.onValueChange(newValue);
        switch(newValue) {
          case "Corner":
            player.setController(new CornerLayoutController(renderer, player));
            break;
          case "Swipe":
            player.setController(new SwipeController(renderer, player));
            break;
          case "Gamepad":
            player.setController(new GamepadController(renderer, player));
            break;
          default: break;
        }
      }
    };

    MenuButton cornerSettingsButton, controllerSettingsButton;
    cornerSettingsButton
        = new MenuButton(renderer, 10, controlType.getY(MenuDrawable.EdgePoint.CENTER),
                         controlType.getHeight(), controlType.getHeight(),
                         MenuDrawable.EdgePoint.LEFT_CENTER) {
      @Override
      public void performAction() {
        // menu.setScreen(new CornerSelectionScreen(menu));
      }
    }.withBackgroundImage(R.drawable.lowerleft);

    controllerSettingsButton
        = new MenuButton(renderer, renderer.getScreenWidth() - 10,
                         controlType.getY(MenuDrawable.EdgePoint.CENTER),
                         controlType.getHeight(), controlType.getHeight(),
                         MenuDrawable.EdgePoint.RIGHT_CENTER) {
      @Override
      public void performAction() {
        menu.setScreen(new SettingsScreen(menu,
                                          player.getPlayerController().toString() + " Options") {
          @Override
          public List<MenuDrawable> createListOfOptions() {
            return player.getPlayerController().optionsList(renderer);
          }

          @Override
          public void goBack() {
            menu.setScreen(reference);
          }
        });
      }
    }.withBackgroundImage(R.drawable.options_icon);

    drawables.add(snakeSelectionCarousel);
    drawables.add(controlType);
    drawables.add(cornerSettingsButton);
    drawables.add(controllerSettingsButton);
  }

  private CarouselItem meshAsCarouselItem(int colorIndex, String name) {
    Mesh snake = new Mesh(renderer, 0, 0, MenuDrawable.EdgePoint.CENTER,
                          snakeSelectionCarousel.getHeight() / 3f, 1, 3);

    float[] colors = Menu.getColorFromIndex(colorIndex);
    float[] tailColors = new float[colors.length];
    for (int i = 0; i < colors.length - 1; i++)
      tailColors[i] = colors[i] / 2f;
    tailColors[3] = 1f;

    snake.updateColors(0, colors);
    snake.updateColors(1, tailColors);
    snake.updateColors(2, tailColors);

    return new CarouselItem(snakeSelectionCarousel, snake, name) {
      @Override
      public void onChosen() {
        super.onChosen();
        player.setColors(colorIndex);
      }
    };
  }
}
