package thusnake.snakemultiplayer;

import android.content.Context;
import android.view.MotionEvent;

import java.util.LinkedList;
import java.util.List;

public class BluetoothController extends PlayerController {
  public BluetoothController(GameRenderer renderer, Player player) {
    super(renderer, player);
  }

  @Override
  public void setTexture(Context context) {}

  @Override
  public void onMotionEvent(MotionEvent event) {}

  @Override
  public String identifier() { return "Bluetooth"; }

  @Override
  public String toString() { return "Via Bluetooth"; }

  @Override
  public List<MenuDrawable> optionsList(GameRenderer renderer) {
    return new LinkedList<>();
  }
}
