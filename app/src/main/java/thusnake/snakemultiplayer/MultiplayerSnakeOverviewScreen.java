package thusnake.snakemultiplayer;

import android.content.Context;
import android.os.Vibrator;
import android.view.MotionEvent;

import java.util.LinkedList;
import java.util.List;

public class MultiplayerSnakeOverviewScreen extends MenuScreen {
  private final MenuButton nextButton;
  private final List<SnakeOverviewButton> overviewButtons = new LinkedList<>();
  private final MenuImage trashIconLeft, trashIconRight;

  public MultiplayerSnakeOverviewScreen(Menu menu) {
    super(menu);

    nextButton
        = new MenuButton(renderer,
                         renderer.getScreenWidth() - 10,
                         renderer.getScreenHeight() - 10,
                         (renderer.getScreenHeight() * 0.2f - 30) * 2,
                         renderer.getScreenHeight() * 0.2f - 30,
                         MenuDrawable.EdgePoint.TOP_RIGHT) {
      @Override
      public void performAction() {
        super.performAction();
        GameSetupScreen setupScreen = new GameSetupScreen(menu) {
          @Override
          public void goBack() {
            menu.setScreen(new MultiplayerSnakeOverviewScreen(menu));
          }
        };

        setupScreen.addGameModeItem(R.drawable.gamemode_classic, "Classic", null,
                                    GameSetupBuffer.GameMode.CLASSIC);
        setupScreen.addGameModeItem(R.drawable.gamemode_placeholder, "Custom",
                                    OptionsBuilder.defaultOptions(renderer),
                                    GameSetupBuffer.GameMode.CUSTOM);
        setupScreen.gameModeCarousel.noBoundaries();
        setupScreen.gameModeCarousel.confirmChoices();

        menu.setScreen(setupScreen);
      }
    }.withBackgroundImage(R.drawable.next_button);

    trashIconLeft = new MenuImage(renderer, -50 - 10, renderer.getScreenHeight() / 2f, 50, 100,
                                  MenuDrawable.EdgePoint.LEFT_CENTER);
    trashIconRight = new MenuImage(renderer, renderer.getScreenWidth() + 50 + 10,
                                   renderer.getScreenHeight() / 2f, 50, 100,
                                   MenuDrawable.EdgePoint.RIGHT_CENTER);
    trashIconLeft.setTexture(R.drawable.trash_bin);
    trashIconRight.setTexture(R.drawable.trash_bin);

    overviewButtons.add(new SnakeOverviewButton(this,
        renderer.getScreenWidth() / 2f - renderer.smallDistance()/2,
        backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f - renderer.smallDistance()/2,
        backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f - 20,
        MenuDrawable.EdgePoint.TOP_RIGHT, PlayerController.Corner.LOWER_LEFT));
    overviewButtons.add(new SnakeOverviewButton(this,
        renderer.getScreenWidth() / 2f - renderer.smallDistance()/2,
        backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f + renderer.smallDistance()/2,
        backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f - 20,
        MenuDrawable.EdgePoint.BOTTOM_RIGHT, PlayerController.Corner.UPPER_LEFT));
    overviewButtons.add(new SnakeOverviewButton(this,
        renderer.getScreenWidth() / 2f + renderer.smallDistance()/2,
        backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f + renderer.smallDistance()/2,
        backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f - 20,
        MenuDrawable.EdgePoint.BOTTOM_LEFT, PlayerController.Corner.UPPER_RIGHT));
    overviewButtons.add(new SnakeOverviewButton(this,
        renderer.getScreenWidth() / 2f + renderer.smallDistance()/2,
        backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f - renderer.smallDistance()/2,
        backButton.getY(MenuDrawable.EdgePoint.BOTTOM_CENTER) / 2f - 20,
        MenuDrawable.EdgePoint.TOP_LEFT, PlayerController.Corner.LOWER_RIGHT));

    drawables.add(nextButton);
    drawables.addAll(overviewButtons);
    drawables.add(trashIconLeft);
    drawables.add(trashIconRight);
  }

  @Override
  public void goBack() {
    menu.setScreen(new MenuMainScreen(menu));
  }

  public void onSnakeOverviewButtonHeld() {
    trashIconLeft.setAnimation(new MenuAnimation(trashIconLeft)
        .addKeyframe(new MoveKeyframe(0.25, 10, trashIconLeft.getY(), BezierTimer.easeOutBack)));
    trashIconRight.setAnimation(new MenuAnimation(trashIconRight)
        .addKeyframe(new MoveKeyframe(0.25, renderer.getScreenWidth() - 10,
                                      trashIconRight.getY(), BezierTimer.easeOutBack)));
  }

  public void onSnakeOverviewButtonReleased(SnakeOverviewButton button, float x, float y) {
    float closestButtonDistance = buttonDistance(x, y, button);
    SnakeOverviewButton closestButton = button;

    // Find the closest button.
    for (SnakeOverviewButton otherButton : overviewButtons) {
      if (otherButton.equals(button)) continue;

      if (closestButtonDistance > buttonDistance(x, y, otherButton)) {
        closestButtonDistance = buttonDistance(x, y, otherButton);
        closestButton = otherButton;
      }
    }

    float distanceToWall = Math.min(x, renderer.getScreenWidth() - x);
    if (distanceToWall < closestButtonDistance) {
      // If its near a wall then the action is snake removal.
      button.removePlayer();
    } else if (closestButton != button) {
      // Otherwise do the swap.
      menu.getSetupBuffer().setPlayerCorner(button.getPlayer(), closestButton.getCorner());
    }

    // Retract the trash icon.
    trashIconLeft.setAnimation(new MenuAnimation(trashIconLeft)
        .addKeyframe(new MoveKeyframe(0.25, -10 - trashIconLeft.getWidth(), trashIconLeft.getY(),
                                      BezierTimer.easeIn)));
    trashIconRight.setAnimation(new MenuAnimation(trashIconRight)
        .addKeyframe(new MoveKeyframe(0.25,
                                      renderer.getScreenWidth() + 10 + trashIconRight.getWidth(),
                                      trashIconRight.getY(), BezierTimer.easeIn)));
  }

  private static float buttonDistance(float x, float y, SnakeOverviewButton button) {
    float buttonX = button.getX(MenuDrawable.EdgePoint.CENTER);
    float buttonY = button.getY(MenuDrawable.EdgePoint.CENTER);
    return (float) Math.sqrt(Math.pow(x - buttonX, 2) + Math.pow(y - buttonY, 2));
  }
}

class SnakeOverviewButton extends MenuButton {
  private MultiplayerSnakeOverviewScreen parentScreen;
  private PlayerController.Corner corner;
  private MenuItem nameItem, plusIcon;
  private Mesh skinPreview;
  private boolean isHeld = false;
  private float holdOffsetX = 0, holdOffsetY = 0;
  private Vibrator vibrator = (Vibrator) renderer.getContext()
                                                 .getSystemService(Context.VIBRATOR_SERVICE);

  SnakeOverviewButton(MultiplayerSnakeOverviewScreen parentScreen, float x, float y, float height,
                      EdgePoint alignPoint, PlayerController.Corner corner) {
    super(parentScreen.renderer, x, y, height * 1.5f, height, alignPoint);
    this.parentScreen = parentScreen;
    this.corner = corner;

    MenuImage background = new MenuImage(renderer, 0, 0, getWidth(), height, EdgePoint.CENTER);
    nameItem = new MenuItem(renderer, "",
                            -getWidth() / 2f,
                            getHeight() / 2f,
                            EdgePoint.TOP_LEFT, EdgePoint.TOP_LEFT);
    skinPreview = new Mesh(renderer, getWidth() / 2f,
                           getHeight() / 2f,
                           EdgePoint.TOP_RIGHT, height / 3f, 1, 3);
    nameItem.scaleToFit(skinPreview.getLeftX() - nameItem.getLeftX(), 0);
    plusIcon = new MenuItem(renderer, "+", 0, 0, EdgePoint.CENTER);
    background.setColors(new float[] {0.2f, 0.2f, 0.2f, 0.75f});
    addItem(background);
    addItem(nameItem);
    addItem(skinPreview);
    addItem(plusIcon);
  }

  @Override
  public void move(double dt) {
    super.move(dt);
    plusIcon.setDrawable(getPlayer() == null);
    nameItem.setDrawable(getPlayer() != null);
    skinPreview.setDrawable(getPlayer() != null);

    if (getPlayer() != null) {
      nameItem.setText(getPlayer().getName());
      nameItem.scaleToFit(skinPreview.getLeftX() - nameItem.getLeftX(), 0);
      skinPreview.updateColors(0, getPlayer().getSkin().headColors());
      skinPreview.updateColors(1, getPlayer().getSkin().tailColors());
      skinPreview.updateColors(2, getPlayer().getSkin().tailColors());
    }
  }

  @Override
  public void performAction() {
    super.performAction();

    if (!isHeld && getPlayer() != null) {
      parentScreen.menu.setScreen(new SnakeCustomizationScreen(parentScreen.menu, getPlayer()) {
        @Override
        public void goBack() {
          parentScreen.menu.setScreen(parentScreen);
        }
      });
    } else {
      GameSetupBuffer setupBuffer = parentScreen.menu.getSetupBuffer();
      setupBuffer.addPlayer(new Player(renderer, setupBuffer.getNumberOfPlayers()).defaultPreset(),
                            corner);
    }
  }

  @Override
  public void onHeld() {
    super.onHeld();
    isHeld = true;
    vibrator.vibrate(50);
    parentScreen.onSnakeOverviewButtonHeld();
  }

  @Override
  public void onMotionEvent(MotionEvent event, float[] pointerX, float[] pointerY) {
    if (!isHeld)
      super.onMotionEvent(event, pointerX, pointerY);
    else {
      if (event.getActionMasked() == MotionEvent.ACTION_MOVE && event.getHistorySize() > 0) {
        holdOffsetX += event.getX() - event.getHistoricalX(0);
        holdOffsetY -= event.getY() - event.getHistoricalY(0);
      } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
        parentScreen.onSnakeOverviewButtonReleased(this, getX(EdgePoint.CENTER) + holdOffsetX,
                                                         getY(EdgePoint.CENTER) + holdOffsetY);
        holdOffsetX = 0;
        holdOffsetY = 0;
        super.onMotionEvent(event, pointerX, pointerY);
        isHeld = false;
      }
    }
  }

  @Override
  public void draw(float[] parentColors) {
    gl.glPushMatrix();
    gl.glTranslatef(holdOffsetX, holdOffsetY, 0);
    super.draw(parentColors);
    gl.glPopMatrix();
  }

  public void removePlayer() {
    parentScreen.menu.getSetupBuffer().emptyCorner(corner);
  }

  public Player getPlayer() {
    return parentScreen.menu.getSetupBuffer().getPlayer(corner);
  }

  public PlayerController.Corner getCorner() { return corner; }
}
