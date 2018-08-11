package thusnake.snakemultiplayer;

final class GameSetupBuffer {
  protected enum GameMode {CLASSIC, SPEEDY, VS_AI, CUSTOM}
  protected GameMode gameMode;
  protected int horizontalSquares, verticalSquares, speed;
  protected int difficulty;
  protected Player[] players;
  protected boolean stageBorders;

  public void adjustToDifficultyAndGameMode() {
    switch(gameMode) {

      case CLASSIC:
        horizontalSquares = 10 + difficulty * 5;
        verticalSquares = 10 + difficulty * 5;
        speed = 6 + difficulty * 3;
        stageBorders = true;
        break;

      case SPEEDY:
        horizontalSquares = 10 + difficulty * 5;
        verticalSquares = 10 + difficulty * 5;
        speed = 10 + difficulty * 5;
        stageBorders = false;
        break;

      case VS_AI:
        horizontalSquares = 20;
        verticalSquares = 20;
        speed = 10;
        stageBorders = true;
        break;

      default:
        break;
    }
  }

  public Game createGame(GameRenderer renderer) {
    switch(gameMode) {
      case CLASSIC:
      case SPEEDY:
        adjustToDifficultyAndGameMode();
        return new Game(renderer, this);
      case CUSTOM:
        return new Game(renderer, this);
      default:
        throw new RuntimeException("This game mode is not supported.");
    }
  }

  public static String gameModeToString(GameMode gameMode) {
    switch (gameMode) {
      case CLASSIC: return "Classic";
      case SPEEDY: return "Speedy";
      case VS_AI: return "AI Battle";
      case CUSTOM: return "Custom";
      default: return "Error";
    }
  }

  public static String difficultyToString(int difficulty) {
    switch (difficulty) {
      case 0: return "Mild";
      case 1: return "Fair";
      case 2: return "Tough";
      case 3: return "Bonkers";
      case 4: return "Ultimate";
      default: return "Error";
    }
  }
}
