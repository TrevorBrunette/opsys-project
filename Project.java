import java.util.ArrayList;
import java.util.Comparator;

public class Project {

    public static long n;
    public static long seed;
    public static double lambda;
    public static long upper_bound;
    public static long tcs;
    public static double alpha;
    public static long tslice;

    private static Comparator<Process> arrivalComparator = new Comparator<Process>(){
        @Override
        public int compare(Process o1, Process o2) {
            return Long.compare(o1.getArrivalTime(), o2.getArrivalTime());
        }
    };

    private static Comparator<Process> runtimeComparator = new Comparator<Process>(){
        @Override
        public int compare(Process o1, Process o2) {
            return Long.compare(o1.tau, o2.tau);
        }
    };

    private static Comparator<Process> idComparator = new Comparator<Process>(){
        @Override
        public int compare(Process o1, Process o2) {
            return Long.compare(o1.id, o2.id);
        }
    };


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
            alpha = Double.parseDouble(args[5]);
            tslice = Long.parseLong(args[6]);

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

        ArrayList<Process> processes = rng.processes(n);

//        for (Process p : processes)
//            System.out.println(p);

        //SJF((cloneProcesses(processes)));
        RR(tslice, cloneProcesses(processes));
        //print_process(0, processes.get(0).getArrivalTime(), (long)(1/lambda), processes.get(0).getBursts());
        //print_stats("test", 1.2345, 3.456789, 5.01, 6, 9, 1.2);
    }

    public static String queueString(ArrayList<Process> queue) {
        String out = "[Q:";
        if(queue.size() > 0) for(Process ps : queue) out += (" " + ps.name());
        else out += (" empty");
        out += ("]");
        return out;
    }

}
