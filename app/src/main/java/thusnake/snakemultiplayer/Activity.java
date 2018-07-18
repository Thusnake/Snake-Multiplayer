package thusnake.snakemultiplayer;

interface Activity {
  /**
   * Called at every frame while this activity is active.
   */
  void run(double dt);

  /**
   * Refreshes all resources, as if they've all been lost.
   * Called every time the app is brought back to focus.
   */
  void refresh();
}
