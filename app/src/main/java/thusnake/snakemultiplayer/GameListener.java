package thusnake.snakemultiplayer;

import thusnake.snakemultiplayer.netplay.ConnectedThread;

/**
 * Listens for signals sent from a Game and handles them.
 */
public interface GameListener {
  void onRegistered(Game game);

  void onGameCreated();
  void onGameStart();
  void onGameOver();

  void beforeMoveExecuted();
  void afterMoveExecuted();

  void onSnakeCreation(Snake snake);
  void onSnakeDeath(Snake snake);

  void onEntityCreation(Entity entity);
  void onEntityDestroyed(Entity entity);

  void onInputBytesReceived(byte[] bytes, ConnectedThread source);
}
