package thusnake.snakemultiplayer;

/**
 * Created by Nick on 12/12/2017.
 */

// Will create a timer which counts down.
public class SimpleTimer {
  private double time, initialTime, endTime, duration;
  private boolean goalSet;
  private int countDirection;

  public SimpleTimer(double initialTime) {
    this.time = initialTime;
    this.initialTime = initialTime;
    this.countDirection = 1;
    goalSet = false;
  }

  public SimpleTimer(double initialTime, double endTime) {
    this.time = initialTime;
    this.initialTime = initialTime;
    this.endTime = endTime;
    this.countDirection = (endTime - initialTime > 0) ? 1 : -1;
    this.duration = (endTime - initialTime > 0) ? endTime - initialTime : initialTime - endTime;
    goalSet = true;
  }

  public double getTime() {
    return this.time;
  }
  public double getInitialTime() { return this.initialTime; }
  public double getEndTime() { return this.endTime; }

  public void countDown(double time) {
    this.time -= time;
  }

  public void countUp(double time) { this.time += time; }

  /**
   * Counts towards the end by a given amount.
   * @param time Amount of time to count.
   * @return Whether the timer has reached the end or not.
   */
  public boolean count(double time) {
    if (!isDone()) {
      this.time += time * countDirection;
      if (this.countDirection > 0 && this.time >= this.endTime
          || this.countDirection < 0 && this.time <= this.endTime) {
        this.time = this.endTime;
        onDone();
        return true;
      }
    }
    return false;
  }

  /**
   * Counts towards the end smoothly, using an ease-out function.
   * @param time A base amount of time to count.
   * @param easeMultiplier Determines the smoothness of the function.
   * @param inertia Determines the minimum speed of the function.
   */
  public void countEaseOut(double time, double easeMultiplier, double inertia) {
    if (!isDone()) {
      double remaining = Math.abs(this.time - this.endTime);
      if (this.countDirection == 1)
        this.time = Math.min(this.time + (remaining * easeMultiplier + inertia) * time,
            this.endTime);
      else
        this.time = Math.max(this.time - (remaining * easeMultiplier + inertia) * time,
            this.endTime);

      if (isDone()) onDone();
    }
  }

  public void reset() {
    this.time = this.initialTime;
  }

  public void setTime(double time) { this.time = time; this.endTime = time; }
  public void setCurrentTime(double time) { this.time = time; }
  public void offsetTime(double offset) {
    this.time += offset;
    if (goalSet) this.endTime += offset;
  }

  /**
   * Sets a new end time of the timer, retaining the current time and initial time.
   * @param time The new end time value.
   */
  public void setEndTime(double time) {
    this.endTime = time;
    this.countDirection = (endTime - initialTime > 0) ? 1 : -1;
    this.duration = (endTime - initialTime > 0) ? endTime - initialTime : initialTime - endTime;
    goalSet = true;
  }

  /**
   * Starts a new timer with the timer's current time as initial and a user-determined end time.
   * @param time The new end time value.
   */
  public void setEndTimeFromNow (double time) {
    this.initialTime = this.time;
    this.endTime = time;
    this.countDirection = (endTime - initialTime > 0) ? 1 : -1;
    this.duration = (endTime - initialTime > 0) ? endTime - initialTime : initialTime - endTime;
    goalSet = true;
  }

  public boolean isDone() {
    return !goalSet || time == endTime;
  }

  public void onDone() {}

  public double getDuration() { return this.duration; }
}
