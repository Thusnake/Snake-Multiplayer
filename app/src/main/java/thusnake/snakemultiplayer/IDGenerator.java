package thusnake.snakemultiplayer;

import java.util.ArrayList;
import java.util.List;

import thusnake.snakemultiplayer.controllers.ControllerBuffer;
import thusnake.snakemultiplayer.gamemodes.GameMode;

public class IDGenerator {
  private static final List<Class<? extends ControllerBuffer>> controllerIDs = new ArrayList<>();

  public static void registerControllerBuffer(Class<? extends ControllerBuffer> controllerClass) {
    controllerIDs.add(controllerClass);
  }

  public static int getControllerBufferID(Class<? extends ControllerBuffer> controllerClass) {
    return controllerIDs.indexOf(controllerClass);
  }

  public static Class<? extends ControllerBuffer> getControllerBufferClass(int id) {
    return controllerIDs.get(id);
  }


  private static final List<Class<? extends GameMode>> gamemodeIDs = new ArrayList<>();

  public static void registerGameMode(Class<? extends GameMode> gameModeClass) {
    gamemodeIDs.add(gameModeClass);
  }

  public static int getGameModeID(Class<? extends GameMode> controllerClass) {
    return gamemodeIDs.indexOf(controllerClass);
  }

  public static Class<? extends GameMode> getGameModeClass(int id) {
    return gamemodeIDs.get(id);
  }
}
