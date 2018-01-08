package thusnake.snakemultiplayer;

/**
 * Created by Nick on 12/12/2017.
 */

// Will create a timer which counts down.
public class SimpleTimer {
  private double time, initialTime, endTime, duration;
  private int countDirection;

  public SimpleTimer(double initialTime) {
    this.time = initialTime;
    this.initialTime = initialTime;
    this.endTime = initialTime;
    this.countDirection = 1;
  }

  public SimpleTimer(double initialTime, double endTime) {
    this.time = initialTime;
    this.initialTime = initialTime;
    this.endTime = endTime;
    this.countDirection = (endTime - initialTime > 0) ? 1 : -1;
    this.duration = (endTime - initialTime > 0) ? endTime - initialTime : initialTime - endTime;
  }

  public double getTime() {
    return time;
  }

  public boolean countDown(double time) {
    try {
      this.time -= time;
    } catch (Exception exception) {
      return false;
    }
    return true;
  }

  public boolean countUp(double time) {
    try{
      this.time += time;
    } catch (Exception exception) {
      return false;
    }
    return true;
  }

  public boolean count(double time) {
    try {
      this.time += time * countDirection;
    } catch (Exception exception) {
      return false;
    }
    return true;
  }

  public void countEaseOut(double time, double easeMultiplier, double inertia) {
    if (this.initialTime != this.endTime) {
      double remaining = Math.abs(this.time - this.endTime);
      if (this.countDirection == 1)
        this.time = Math.min(this.time + (remaining * easeMultiplier + inertia) * time,
            this.endTime);
      else
        this.time = Math.max(this.time - (remaining * easeMultiplier + inertia) * time,
            this.endTime);
    }
  }

  public boolean reset() {
    try {
      this.time = this.initialTime;
    } catch (Exception exception) {
      return false;
    }
    return true;
  }

  public void setEndTime(double time) {
    this.endTime = time;
    this.countDirection = (endTime - initialTime > 0) ? 1 : -1;
    this.duration = (endTime - initialTime > 0) ? endTime - initialTime : initialTime - endTime;
  }

  public void setEndTimeFromNow (double time) {
    this.initialTime = this.time;
    this.endTime = time;
    this.countDirection = (endTime - initialTime > 0) ? 1 : -1;
    this.duration = (endTime - initialTime > 0) ? endTime - initialTime : initialTime - endTime;
  }

  public boolean isDone() {
    return this.time == this.endTime;
  }
}
