package thusnake.snakemultiplayer;

/**
 * Akin to a SimpleTimer, but uses a bezier curve formula to give a progression value, which is
 * a curve, unlike the linear time value.
 * BezierTimer always takes a given amount of time to execute, unlike the countEaseOut() function
 * of the SimpleTimer, which is fairly unpredictable.
 * On the other hand, the BezierTimer is much less flexible and not to be used for animations which
 * can be manipulated (i.e. offset, stopped and continued, etc).
 */
public class BezierTimer {
  private double time = 0, progression = 0, duration, beginValue, endValue;
  private double[] parameters;

  public BezierTimer(double duration, double beginValue, double endValue, double[] parameters) {
    this.duration = duration;
    this.beginValue = beginValue;
    this.endValue = endValue;
    if (parameters.length == 4) this.parameters = parameters;
    else throw new RuntimeException("BezierTimer provided with an incorrect number of parameters.");
  }

  /**
   * Increments the timer and updates the progression. To be called at every active frame.
   * @param dt The delta time of that frame.
   * @return Whether the timer has finished with this invocation of count() or not.
   */
  public boolean count(double dt) {
    if (!isDone()) {
      time += dt;

      if (time >= duration)
        time = duration;

      updateBezier();

      return isDone();
    }
    return false;
  }

  private void updateBezier() {
    double t = time / duration;
    progression = calculateBezier(getTForX(t), parameters[1], parameters[3]);
  }

  /** Returns x(t) given t, a1, and a2, where a is either x or y. */
  private double calculateBezier(double t, double a1, double a2) {
    return (((1.0 - 3.0 * a2 + 3.0 * a1)*t + 3.0 * a2 - 6.0 * a1)*t + 3.0 * a1)*t;
  }

  /** Returns delta a given t, a1, and a2, where a is either x or y. */
  private double getSlope(double t, double a1, double a2) {
    return 3.0 * (1.0 - 3.0 * a2 + 3.0 * a1)*t*t + 2.0 * (3.0 * a2 - 6.0 * a1) * t + 3 * a1;
  }

  /** Returns the adjusted t of the bezier curve. */
  private double getTForX(double x) {
    double guessT = x;
    for (int i = 0; i < 4; ++i) {
      double currentSlope = getSlope(guessT, parameters[0], parameters[2]);
      if (currentSlope == 0.0) return guessT;
      double currentX = calculateBezier(guessT, parameters[0], parameters[2]) - x;
      guessT -= currentX / currentSlope;
    }
    return guessT;
  }

  public double getValue() { return beginValue + progression * (endValue - beginValue); }

  public boolean isDone() { return time == duration; }

  // Ready function parameters.
  public static double[] easeIn = new double[] {0.55, 0.055, 0.675, 0.19};
  public static double[] easeOut = new double[] {0.215, 0.61, 0.355, 1};
  public static double[] easeInOut = new double[] {0.645, 0.045, 0.355, 1};
  public static double[] easeInBack = new double[] {0.6, -0.28, 0.735, 0.045};
  public static double[] easeOutBack = new double[] {0.175, 0.885, 0.32, 1.275};
  public static double[] easeInOutBack = new double[] {0.68, -0.55, 0.265, 1.55};

}
