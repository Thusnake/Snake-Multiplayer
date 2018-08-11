package thusnake.snakemultiplayer;

import java.util.LinkedList;
import java.util.List;

public class MenuAnimation {
  final List<Keyframe> keyframes = new LinkedList<>();
  private Keyframe currentKeyframe;
  private MenuDrawable drawable;
  private boolean done = false;

  public MenuAnimation(MenuDrawable drawable) {
    this.drawable = drawable;
  }

  public MenuAnimation addKeyframe(Keyframe keyframe) { keyframes.add(keyframe); return this; }

  /**
   * Runs the animation's keyframes.
   * Will do nothing if there are no keyframes set yet.
   * It is recommended that you do not add frames during the runtime of the animation, because if it
   * ever runs out of keyframes it will be labelled as done.
   * @param dt The delta time of the current in-engine frame.
   * @return True if the animation is done and is disposable.
   */
  public boolean run(double dt) {
    if (keyframes.size() > 0 && !done) {
      // If run hasn't been called before then currentKeyframe will be null. In that case give it
      // the first keyframe to start things off.
      if (currentKeyframe == null)
        currentKeyframe = keyframes.get(0);

      // Count the current keyframe.
      if (currentKeyframe.count(dt, drawable)) {
        // If we're done with it check if there's a next one and set that one as the current one.
        if (keyframes.indexOf(currentKeyframe) + 1 < keyframes.size())
          currentKeyframe = keyframes.get(keyframes.indexOf(currentKeyframe) + 1);
        else {
          // In case we've ran out of keyframes return true as to signal that the animation is done.
          done = true;
          return true;
        }
      }
    }
    return false;
  }
}

/**
 * Acts as an empty/wait keyframe for a given duration.
 */
class Keyframe {
  double duration;
  private SimpleTimer timer;

  Keyframe(double duration) {
    this.duration = duration;
    timer = new SimpleTimer(0.0, duration);
  }

  boolean count(double dt, MenuDrawable drawable) {
    return timer.count(dt);
  }
}

/**
 * Moves the drawable to a desired position in the given time frame.
 */
class MoveKeyframe extends Keyframe {
  private BezierTimer timerX, timerY;
  private double destinationX, destinationY;
  private double[] bezierParameters;

  MoveKeyframe(double duration, float destinationX, float destinationY,
               double[] bezierParameters) {
    super(duration);
    this.destinationX = destinationX;
    this.destinationY = destinationY;
    this.bezierParameters = bezierParameters;
  }

  @Override
  boolean count(double dt, MenuDrawable drawable) {
    if (timerX == null || timerY == null) {
      timerX = new BezierTimer(duration, drawable.getX(), destinationX, bezierParameters);
      timerY = new BezierTimer(duration, drawable.getY(), destinationY, bezierParameters);
    }

    timerX.count(dt);
    timerY.count(dt);

    drawable.setX(timerX.getValue());
    drawable.setY(timerY.getValue());

    return timerX.isDone();
  }
}
