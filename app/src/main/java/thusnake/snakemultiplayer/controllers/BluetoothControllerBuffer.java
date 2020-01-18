package thusnake.snakemultiplayer.controllers;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.MotionEvent;

import java.util.LinkedList;
import java.util.List;

import thusnake.snakemultiplayer.ConnectedThread;
import thusnake.snakemultiplayer.Game;
import thusnake.snakemultiplayer.GameRenderer;
import thusnake.snakemultiplayer.GameSetupBuffer;
import thusnake.snakemultiplayer.MenuDrawable;
import thusnake.snakemultiplayer.Player;
import thusnake.snakemultiplayer.Snake;

public class BluetoothControllerBuffer extends ControllerBuffer {
  public final ConnectedThread controllerThread;

  public BluetoothControllerBuffer(Player player, ConnectedThread thread) {
    super(player);
    controllerThread = thread;
  }

  @Override
  public String identifier() { return "Bluetooth"; }

  @Override
  public String toString() { return "Via Bluetooth"; }

  @Override
  public List<MenuDrawable> optionsList() {
    return new LinkedList<>();
  }

  @Override
  public void saveSettings(SharedPreferences.Editor settings, GameSetupBuffer setupBuffer) {}

  @Override
  public ControllerBuffer loadSettings(Context context, GameSetupBuffer setupBuffer) { return null;}

  @Override
  public Controller constructController(Game game, Snake snake) {
    return new BluetoothController(game, snake, controllerThread);
  }
}
