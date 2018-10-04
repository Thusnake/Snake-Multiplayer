package thusnake.snakemultiplayer;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Vibrator;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class MultiplayerSnakeOverviewScreen extends MenuScreen {
  private final MenuButton nextButton, readyButton;
  private final MenuButton connectButton;
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
        setupScreen.addGameModeItem(R.drawable.gamemode_custom, "Custom",
                                    OptionsBuilder.defaultOptions(renderer),
                                    GameSetupBuffer.GameMode.CUSTOM);
        setupScreen.gameModeCarousel.noBoundaries();
        setupScreen.gameModeCarousel.confirmChoices();

        menu.setScreen(setupScreen);
      }
    }.withBackgroundImage(R.drawable.next_button);

    readyButton = new MenuButton(renderer, nextButton.getX(), nextButton.getY(),
        nextButton.getWidth(), nextButton.getHeight(), nextButton.alignPoint) {
      @Override
      public void performAction() {
        super.performAction();
        if (originActivity.isGuest())
          originActivity.writeBytesAuto(new byte[] {Protocol.IS_READY});
        else if (originActivity.isHost()) {
          originActivity.setReady(true);
          originActivity.acceptThread.cancel();
          originActivity.acceptThread = null;
          nextButton.performAction();
        }
      }
    }.withBackgroundImage(R.drawable.ready_button);

    connectButton = new MenuButton(renderer,
        backButton.getX(MenuDrawable.EdgePoint.RIGHT_CENTER)
            + (nextButton.getX(MenuDrawable.EdgePoint.LEFT_CENTER)
            - backButton.getX(MenuDrawable.EdgePoint.RIGHT_CENTER)) / 2f,
        backButton.getY(MenuDrawable.EdgePoint.CENTER),
        nextButton.getX(MenuDrawable.EdgePoint.LEFT_CENTER)
            - backButton.getX(MenuDrawable.EdgePoint.RIGHT_CENTER) - renderer.smallDistance() * 8,
        backButton.getHeight(),
        MenuDrawable.EdgePoint.CENTER) {
      private MenuItem defaultText, hostName;
      private MenuImage[] deviceStatusIcons;

      @Override
      public void onButtonCreated() {
        super.onButtonCreated();

        withBackgroundImage(R.drawable.connect_button_background);

        defaultText = new MenuItem(renderer, "HOST/JOIN",
            getX(EdgePoint.CENTER), getY(EdgePoint.CENTER), EdgePoint.CENTER);

        hostName = new MenuItem(renderer, "",
            getX(EdgePoint.LEFT_CENTER) + renderer.smallDistance() * 2, getY(EdgePoint.LEFT_CENTER),
            EdgePoint.LEFT_CENTER);

        defaultText.scaleToFit(0, getHeight() / 2.4f);
        hostName.scaleToFit(0, getHeight() / 2.4f);

        defaultText.setColors(0.35f, 0.55f, 0.59f);
        hostName.setColors(0.35f, 0.55f, 0.59f);

        deviceStatusIcons = new MenuImage[4];
        float iconsHeight = getHeight(), iconsWidth = iconsHeight * 81f / 168f;
        for (int index = 0; index < deviceStatusIcons.length; index++) {
          deviceStatusIcons[index] = new MenuImage(renderer,
              this.getX(EdgePoint.RIGHT_CENTER) - iconsWidth * (3 - index)
                                  - renderer.smallDistance() * (4 - index),
              this.getY(EdgePoint.CENTER), iconsWidth, iconsHeight, EdgePoint.RIGHT_CENTER);

          addItem(deviceStatusIcons[index]);
        }

        addItem(defaultText);
        addItem(hostName);
      }

      @Override
      public void move(double dt) {
        super.move(dt);
        // Text.
        defaultText.setDrawable(!originActivity.isGuest() && !originActivity.isHost());
        hostName.setDrawable(originActivity.isGuest() || originActivity.isHost());

        if (originActivity.isGuest())
          hostName.setText(originActivity.connectedThread.device.getName());
        else if (originActivity.isHost())
          hostName.setText(originActivity.acceptThread != null ? "Host Open" : "Host Locked");

        if (originActivity.isGuest() || originActivity.isHost())
          hostName.scaleToFit(0, getHeight() / 2.4f);

        // Device status icons.
        for (MenuImage icon : deviceStatusIcons)
          icon.setDrawable(originActivity.isGuest() || originActivity.isHost());

        if (originActivity.isGuest() || originActivity.isHost()) {
          for (MenuImage icon : deviceStatusIcons)
            icon.setTexture(R.drawable.phone_icon_free);

          deviceStatusIcons[0].setTexture(originActivity.isReady()
              ? R.drawable.phone_icon_ready : R.drawable.phone_icon_joined);

          for (int index = 1; index < originActivity.getNumberOfRemoteDevices(); index++)
            deviceStatusIcons[index].setTexture(R.drawable.phone_icon_joined);

          int readyExternalDevices = originActivity.getNumberOfReadyRemoteDevices()
                                        - (originActivity.isReady() ? 1 : 0);
          for (int index = 1; index <= readyExternalDevices; index++)
            deviceStatusIcons[index].setTexture(R.drawable.phone_icon_ready);
        }
      }

      @Override
      public void performAction() {
        super.performAction();
        if (originActivity.isGuest());
        else if (originActivity.isHost()) menu.setScreen(new HostManagementScreen(menu));
        else menu.setScreen(new ConnectScreen(menu));
      }
    };

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
    drawables.add(readyButton);
    drawables.add(connectButton);
    drawables.addAll(overviewButtons);
    drawables.add(trashIconLeft);
    drawables.add(trashIconRight);

    // Getting to this screen sets your ready status to false, as there is a ready button on it.
    originActivity.setReady(false);

    // Restart the accepting thread if you're a host.
    if (originActivity.isHost() && originActivity.acceptThread == null) {
      originActivity.acceptThread = new AcceptThread(originActivity);
      originActivity.acceptThread.start();
    }
  }

  @Override
  public void moveAll(double dt) {
    boolean isOnline = originActivity.isGuest() || originActivity.isHost();
    nextButton.setDrawable(!isOnline);
    readyButton.setDrawable(isOnline);
    readyButton.setEnabled(!originActivity.isHost()
                           || originActivity.getNumberOfReadyRemoteDevices()
                               >= originActivity.getNumberOfRemoteDevices() - 1
                                && originActivity.getNumberOfRemoteDevices() > 1);
    super.moveAll(dt);
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
      menu.getSetupBuffer().cornerMap.swapCorners(button.getCorner(), closestButton.getCorner());
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

    MenuImage background = new MenuImage(renderer, getX(EdgePoint.CENTER), getY(EdgePoint.CENTER),
                                         getWidth(), height, EdgePoint.CENTER);
    nameItem = new MenuItem(renderer, "",
                            getX(EdgePoint.TOP_LEFT) + renderer.smallDistance(),
                            getY(EdgePoint.TOP_LEFT) - renderer.smallDistance(),
                            EdgePoint.TOP_LEFT, EdgePoint.TOP_LEFT);
    skinPreview = new Mesh(renderer,
                           getX(EdgePoint.TOP_RIGHT) - renderer.smallDistance(),
                           getY(EdgePoint.TOP_RIGHT) - renderer.smallDistance(),
                           EdgePoint.TOP_RIGHT, (height - 2 - renderer.smallDistance()*2)/3f, 1, 3);
    nameItem.scaleToFit(skinPreview.getLeftX() - nameItem.getLeftX() - renderer.smallDistance(),
                        getHeight() / 3f);
    plusIcon = new MenuItem(renderer, "+", getX(EdgePoint.CENTER), getY(EdgePoint.CENTER),
                            EdgePoint.CENTER);
    background.setColors(new float[] {0.2f, 0.2f, 0.2f, 0.75f});
    addItem(background);
    addItem(nameItem);
    addItem(skinPreview);
    addItem(plusIcon);
  }

  @Override
  public void move(double dt) {
    super.move(dt);

    Player player = getPlayer();

    plusIcon.setDrawable(player == null);
    nameItem.setDrawable(player != null);
    skinPreview.setDrawable(player != null);
    if (player != null) {
      nameItem.setText(player.getName());
      nameItem.scaleToFit(skinPreview.getLeftX() - nameItem.getLeftX() - renderer.smallDistance(),
                          getHeight() / 3f);
      skinPreview.updateColors(0, player.getSkin().headColors());
      skinPreview.updateColors(1, player.getSkin().tailColors());
      skinPreview.updateColors(2, player.getSkin().tailColors());
      skinPreview.updateTextures(0, 0, player.getSkin().texture(SnakeSkin.TextureType.HEAD,
                                                                Player.Direction.DOWN));
      skinPreview.updateTextures(0, 1, player.getSkin().texture(SnakeSkin.TextureType.BODY,
                                                                Player.Direction.DOWN));
      skinPreview.updateTextures(0, 2, player.getSkin().texture(SnakeSkin.TextureType.TAIL,
                                                                Player.Direction.DOWN));
    }
  }

  private class MultiplayerSnakeCustomizationScreen extends SnakeCustomizationScreen {
    private final MenuItem nameItem;
    private MultiplayerSnakeCustomizationScreen(Menu menu, Player player) {
      super(menu, player);
      nameItem = new MenuItem(renderer, player.getName(),
                              renderer.getScreenWidth() - renderer.smallDistance(),
                              backButton.getY(EdgePoint.CENTER),
                              EdgePoint.RIGHT_CENTER) {
        @Override
        public void move(double dt) {
          super.move(dt);
          setText(player.getName());
        }

        @Override
        public void performAction() {
          super.performAction();
          // Create a dialog box for the player to set their name.
          AlertDialog.Builder alertDialogBuilder =  new AlertDialog.Builder(originActivity);
          EditText input = new EditText(originActivity);
          input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

          // Create a container for the input in order to have some padding.
          FrameLayout container = new FrameLayout(originActivity);
          FrameLayout.LayoutParams params = new FrameLayout.LayoutParams
              (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
          params.setMargins((int) renderer.smallDistance()*5,
                            (int) renderer.smallDistance()*3,
                            (int) renderer.smallDistance()*5,
                            (int) renderer.smallDistance());
          input.setLayoutParams(params);
          container.addView(input);

          // Add it all to the dialog box.
          alertDialogBuilder.setTitle("Name:");
          alertDialogBuilder.setView(container);
          alertDialogBuilder.setPositiveButton("Confirm",
                                               (dialogInterface, i)
                                                    -> player.setName(input.getText().toString()));
          alertDialogBuilder.setOnDismissListener(dialogInterface -> originActivity.hideUI());
          alertDialogBuilder.setOnCancelListener(dialogInterface -> originActivity.hideUI());
          AlertDialog dialog = alertDialogBuilder.create();
          // Make the dialog non-focusable so that we can set up a fix for leaving immersive mode.
          Objects.requireNonNull(dialog.getWindow())
              .setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
          dialog.show();
          // We'll set the dialog's visibility to match our app's after we've shown it.
          dialog.getWindow().getDecorView().setSystemUiVisibility(
              originActivity.getWindow().getDecorView().getSystemUiVisibility());
          // And once we're done with that we'll make the dialog focusable again.
          dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
      };
      drawables.add(nameItem);
    }

    @Override
    public void goBack() { parentScreen.menu.setScreen(parentScreen); }
  }

  @Override
  public void performAction() {
    super.performAction();

    Player player = getPlayer();
    if (!isHeld && player != null) {
      parentScreen.menu
          .setScreen(new MultiplayerSnakeCustomizationScreen(parentScreen.menu, player));
    } else {
      if (parentScreen.originActivity.isGuest()) {
        parentScreen.originActivity.writeBytesAuto(new byte[] {Protocol.REQUEST_ADD_SNAKE,
                                                               Protocol.encodeCorner(corner)});
      } else {
        GameSetupBuffer setupBuffer = parentScreen.menu.getSetupBuffer();
        setupBuffer.cornerMap.addPlayer(new Player(renderer).defaultPreset(setupBuffer), corner);
      }
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
  public boolean isClicked(float x, float y) {
    return !isHeld && super.isClicked(x, y);
  }

  @Override
  public void draw(float[] parentColors) {
    gl.glPushMatrix();
    gl.glTranslatef(holdOffsetX, holdOffsetY, 0);
    super.draw(parentColors);
    gl.glPopMatrix();
  }

  public void removePlayer() {
    parentScreen.menu.getSetupBuffer().cornerMap.emptyCorner(corner);
  }

  public Player getPlayer() {
    return parentScreen.menu.getSetupBuffer().cornerMap.getPlayer(corner);
  }

  public PlayerController.Corner getCorner() { return corner; }
}
