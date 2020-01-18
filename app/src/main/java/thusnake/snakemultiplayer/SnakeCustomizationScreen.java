package thusnake.snakemultiplayer;

import java.util.LinkedList;
import java.util.List;

import thusnake.snakemultiplayer.controllers.ControllerBuffer;
import thusnake.snakemultiplayer.controllers.ControllerBuffer.Corner;
import thusnake.snakemultiplayer.controllers.CornerLayoutControllerBuffer;
import thusnake.snakemultiplayer.controllers.GamepadControllerBuffer;
import thusnake.snakemultiplayer.controllers.SwipeControllerBuffer;

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
    snakeSelectionCarousel.addChoice(skinAsCarouselItem(SnakeSkin.white, "White"));
    snakeSelectionCarousel.addChoice(skinAsCarouselItem(SnakeSkin.red, "Red"));
    snakeSelectionCarousel.addChoice(skinAsCarouselItem(SnakeSkin.orange, "Orange"));
    snakeSelectionCarousel.addChoice(skinAsCarouselItem(SnakeSkin.yellow, "Yellow"));
    snakeSelectionCarousel.addChoice(skinAsCarouselItem(SnakeSkin.green, "Green"));
    snakeSelectionCarousel.addChoice(skinAsCarouselItem(SnakeSkin.teal, "Teal"));
    snakeSelectionCarousel.addChoice(skinAsCarouselItem(SnakeSkin.blue, "Blue"));
    snakeSelectionCarousel.addChoice(skinAsCarouselItem(SnakeSkin.purple, "Purple"));
    snakeSelectionCarousel.addChoice(skinAsCarouselItem(SnakeSkin.coral, "Black"));
    snakeSelectionCarousel.addChoice(skinAsCarouselItem(SnakeSkin.oldSchool, "Old School"));
    snakeSelectionCarousel.setDefaultChoice(player.getSkinIndex());
    snakeSelectionCarousel.noBoundaries();
    snakeSelectionCarousel.notChosenOpacity = 0.5f;
    snakeSelectionCarousel.confirmChoices();

    List<ControllerBuffer> controllerBufferChoices = ControllerBuffer.getControllerChoiceList(player);
    List<String> possibleControlTypes = new LinkedList<>();
    for (ControllerBuffer controllerBuffer : controllerBufferChoices)
      possibleControlTypes.add(controllerBuffer.identifier());

    MenuCustomValue controlType
        = new MenuCustomValue(renderer, possibleControlTypes,
                              renderer.getScreenWidth() / 2f,
                              renderer.smallDistance(),
                              MenuDrawable.EdgePoint.BOTTOM_CENTER) {
      @Override
      public void move(double dt) {
        super.move(dt);
        setValue(player.getControllerBuffer().identifier());
      }

      @Override
      public void onValueChange(String newValue) {
        super.onValueChange(newValue);

        for (ControllerBuffer controllerBuffer : controllerBufferChoices)
          if (controllerBuffer.identifier().equals(newValue))
            player.setController(controllerBuffer);
      }
    }.setLabel("Control type:");

    MenuButton cornerSettingsButton, controllerSettingsButton;
    float buttonsSize = renderer.getGlText().getHeight() * 1.2f;
    cornerSettingsButton
        = new MenuButton(renderer, 10, controlType.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER),
                         buttonsSize, buttonsSize, MenuDrawable.EdgePoint.BOTTOM_LEFT) {
      @Override
      public void move(double dt) {
        super.move(dt);
        int id;
        switch (player.getControlCorner()) {
          case LOWER_LEFT: id = R.drawable.lowerleft; break;
          case UPPER_LEFT: id = R.drawable.upperleft; break;
          case UPPER_RIGHT: id = R.drawable.upperright; break;
          case LOWER_RIGHT: id = R.drawable.lowerright; break;
          default: throw new RuntimeException(player + "'s corner is null");
        }
        withBackgroundImage(id);
      }

      @Override
      public void performAction() {
        MenuScreen cornerSelectScreen = new MenuScreen(menu) {
          @Override
          public void goBack() {
            menu.setScreen(reference);
          }
        };

        MultilineMenuItem title
            = new MultilineMenuItem(renderer, "Select corner",
                                    renderer.getScreenWidth() / 2f,
                                    renderer.getScreenHeight() - 10, EdgePoint.TOP_CENTER,
                                    renderer.getScreenWidth() - backButton.getWidth()  * 2f - 20);

        cornerSelectScreen.drawables.add(title);

        float size = renderer.getScreenHeight() / 4f, centerX = renderer.getScreenWidth() / 2f,
              centerY = title.getY(EdgePoint.BOTTOM_CENTER) / 2f;

        cornerSelectScreen.drawables.add(
            new MenuImage(renderer, centerX, centerY, size, size, EdgePoint.TOP_RIGHT) {
              @Override
              public void move(double dt) {
                super.move(dt);
                setOpacity(player.getControlCorner().equals(Corner.LOWER_LEFT) ? 1 : 0.25f);
              }

              @Override
              public void performAction() {
                super.performAction();
                menu.getSetupBuffer().cornerMap.setPlayerCorner(player, Corner.LOWER_LEFT);
              }
            });

        cornerSelectScreen.drawables.add(
            new MenuImage(renderer, centerX, centerY, size, size, EdgePoint.BOTTOM_RIGHT) {
              @Override
              public void move(double dt) {
                super.move(dt);
                setOpacity(player.getControlCorner().equals(Corner.UPPER_LEFT) ? 1 : 0.25f);
              }

              @Override
              public void performAction() {
                super.performAction();
                menu.getSetupBuffer().cornerMap.setPlayerCorner(player, Corner.UPPER_LEFT);
              }
            });

        cornerSelectScreen.drawables.add(
            new MenuImage(renderer, centerX, centerY, size, size, EdgePoint.BOTTOM_LEFT) {
              @Override
              public void move(double dt) {
                super.move(dt);
                setOpacity(player.getControlCorner().equals(Corner.UPPER_RIGHT) ? 1 : 0.25f);
              }

              @Override
              public void performAction() {
                super.performAction();
                menu.getSetupBuffer().cornerMap.setPlayerCorner(player, Corner.UPPER_RIGHT);
              }
            });

        cornerSelectScreen.drawables.add(
            new MenuImage(renderer, centerX, centerY, size, size, EdgePoint.TOP_LEFT) {
              @Override
              public void move(double dt) {
                super.move(dt);
                setOpacity(player.getControlCorner().equals(Corner.LOWER_RIGHT) ? 1 : 0.25f);
              }

              @Override
              public void performAction() {
                super.performAction();
                menu.getSetupBuffer().cornerMap.setPlayerCorner(player, Corner.LOWER_RIGHT);
              }
            });

        menu.setScreen(cornerSelectScreen);
      }
    }.withBackgroundImage(R.drawable.lowerleft);

    controllerSettingsButton
        = new MenuButton(renderer, renderer.getScreenWidth() - 10,
                         controlType.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER),
                         buttonsSize, buttonsSize, MenuDrawable.EdgePoint.BOTTOM_RIGHT) {
      @Override
      public void performAction() {
        menu.setScreen(new SettingsScreen(menu,
                                          player.getControllerBuffer().toString() + " Options") {
          @Override
          public List<MenuDrawable> createListOfOptions() {
            return player.getControllerBuffer().optionsList();
          }

          @Override
          public void goBack() {
            menu.setScreen(reference);
          }
        });
      }
    }.withBackgroundImage(R.drawable.wrench);

    drawables.add(snakeSelectionCarousel);
    drawables.add(controlType);
    drawables.add(controllerSettingsButton);
    drawables.add(cornerSettingsButton);
  }

  private CarouselItem skinAsCarouselItem(SnakeSkin skin, String name) {
    Mesh snake = skin.previewMesh(renderer, 0, 0, MenuDrawable.EdgePoint.CENTER,
                                  snakeSelectionCarousel.getHeight(), 3);

    return new CarouselItem(snakeSelectionCarousel, snake, name) {
      @Override
      public void onChosen() {
        super.onChosen();
        player.setSkin(skin);
      }
    };
  }
}
