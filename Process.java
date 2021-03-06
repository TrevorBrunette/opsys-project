import java.util.ArrayList;
import java.util.Arrays;

public class Process {

    public final int id;
    private long arrival_time;
    private ArrayList<Long> bursts;
    public long tau;

    public int current_burst;
    public int elapsed_time;
    public int turnaround_time;
    public int wait_time;

    public boolean waiting = false;

    public Process(int id, long arrival_time, ArrayList<Long> bursts) {
        this.id = id;
        this.arrival_time = arrival_time;
        this.bursts = bursts;

        this.current_burst = 0;
        this.elapsed_time = 0;
        tau = (long) Math.ceil(1 / Project.lambda);
    }

    // https://en.wikipedia.org/wiki/Moving_average under the exponential average section
    //TODO I thought the equation was (1-alpha)(tau) + (alpha)(last burst)
    public void recalculateTau() {
        tau = (long) Math.ceil(tau + Project.alpha * (bursts.get(current_burst-1) - tau));
    }

    public long getArrivalTime() {
        return arrival_time;
    }

    public ArrayList<Long> getBursts() {
        return bursts;
    }

    public long getCurrentBurst() { return bursts.get(current_burst); }

    @Override
    public String toString() {
        return "arrival time = " + arrival_time + "; current = " + current_burst +
                "; bursts=" + Arrays.toString(bursts.toArray());
    }

    public String name() {
        assert id < 26;
        return "" + (char)(id + 'A');
    }

}
