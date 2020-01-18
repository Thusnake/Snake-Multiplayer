package thusnake.snakemultiplayer.controllers;

import android.content.Context;
import android.view.MotionEvent;

import thusnake.snakemultiplayer.ConnectedThread;
import thusnake.snakemultiplayer.Game;
import thusnake.snakemultiplayer.Snake;

public class BluetoothController extends Controller {
  BluetoothController(Game game, Snake snake, ConnectedThread thread) {
    super(game, snake);
  }

  @Override
  public void draw() {}

  @Override
  public void setTexture(Context context) {}

  @Override
  public void onMotionEvent(MotionEvent event) {}
}
