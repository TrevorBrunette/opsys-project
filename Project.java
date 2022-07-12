import java.util.ArrayList;

public class Project {

    static long n;
    static long seed;
    static double lambda;
    static long upper_bound;
    static long tcs;
    static long tau;
    static long tsclice;


    public static void main(String[] args) {
        if ( args.length != 8 ) {
            System.err.println("ERROR: incorrect Number of Arguments.");
            System.exit(1);
        }

        try {
            n = Long.parseLong(args[1]);
            seed = Long.parseLong(args[2]);
            lambda = Double.parseDouble(args[3]);
            upper_bound = Long.parseLong(args[4]);
            tcs = Long.parseLong(args[5]);
            tau = Long.parseLong(args[6]);
            tsclice = Long.parseLong(args[7]);

        }
        catch (Exception e) {
            System.err.println("ERROR: error parsing arguments");
            System.exit(1);
        }

        if (n > 26) {
            System.err.println("ERROR: too many processes");
            System.exit(1);
        }

    }

    public static void print_stats(String algorithm, double burst, double wait, double turnaround,
                                   int switches, int preemptions, double utilization) {
        System.out.println("Algorithm " + algorithm);
        System.out.printf("-- average CPU burst time: %.3f ms\n", format_double(burst));
        System.out.printf("-- average wait time: %.3f ms\n", format_double(wait));
        System.out.printf("-- average turnaround time: %.3f ms\n", format_double(turnaround));
        System.out.printf("-- total number of context switches: %d\n", switches);
        System.out.printf("-- total number of preemptions: %d\n", preemptions);
        System.out.printf("-- CPU utilization: %.3f%%\n", format_double(utilization));
    }

    private static double format_double(double d) {
        return Math.ceil(d*1000)/1000;
    }

    public static void test() {
        n = 3;
        seed = 19;
        lambda = 0.01;
        upper_bound = 4096;
        RNG rng = new RNG(seed, lambda, upper_bound);
        ArrayList<Long> arrival_times = rng.arrival_times(n);
        ArrayList<ArrayList<Long>> bursts = rng.bursts(n);
        for (Long t : arrival_times)
            System.out.print(t.toString() + " ");
        System.out.println("");
        for (ArrayList<Long> ar : bursts) {
            for (Long b : ar)
                System.out.print(b.toString() + " ");
            System.out.println("");
        }

        print_stats("test", 1.2345, 3.456789, 5.01, 6, 9, 1.2);
    }

}
