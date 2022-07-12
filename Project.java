import java.util.ArrayList;

public class Project {

    private static long n;
    private static long seed;
    private static double lambda;
    private static long upper_bound;
    private static long tcs;
    private static double tau;
    private static long tsclice;


    public static void main(String[] args) {
        if ( args.length != 7 ) {
            System.err.println("ERROR: incorrect Number of Arguments.");
            System.exit(1);
        }

        try {
            n = Long.parseLong(args[0]);
            seed = Long.parseLong(args[1]);
            lambda = Double.parseDouble(args[2]);
            upper_bound = Long.parseLong(args[3]);
            tcs = Long.parseLong(args[4]);
            tau = Double.parseDouble(args[5]);
            tsclice = Long.parseLong(args[6]);

        }
        catch (Exception e) {
            System.err.println("ERROR: error parsing arguments");
            System.exit(1);
        }

        if (n > 26) {
            System.err.println("ERROR: too many processes");
            System.exit(1);
        }

        test();
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

    public static void print_process(int proc, long arrival, long tau, ArrayList<Long> bursts) {
        System.out.printf("Process %s: arrival time: %dms; tau %dms; %d CPU bursts\n", proc_to_name(proc), arrival, tau, (bursts.size()+1)/2);
        for(int i = 0; i < bursts.size() - 1; i += 2) {
            System.out.printf("--> CPU burst %dms --> I/O burst %dms\n", bursts.get(i), bursts.get(i+1));
        }
        System.out.printf("--> CPU burst %dms\n", bursts.get(bursts.size()-1));
    }

    private static String proc_to_name(int proc) {
        assert proc < 26;
        return "" + (char)(proc + 'A');
    }

    public static void test() {
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
        print_process(0, arrival_times.get(0), (long)(1/lambda), bursts.get(0));
        print_stats("test", 1.2345, 3.456789, 5.01, 6, 9, 1.2);
    }

}
