import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    // for process queue to know when processes arrive
    private static Comparator<Process> arrivalComparator = new Comparator<Process>(){
        @Override
        public int compare(Process o1, Process o2) {
            return Long.compare(o1.getArrivalTime(), o2.getArrivalTime());
        }
    };

    // for SRT
    private static Comparator<Process> runtimeComparator = new Comparator<Process>(){
        @Override
        public int compare(Process o1, Process o2) {
            return Long.compare(o1.tau, o2.tau);
        }
    };
    // may need to make a new one for SJF

    // for addressing ties
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

    public static String get_stats(String algorithm, double burst, double wait, double turnaround,
                                   int switches, int preemptions, double utilization) {
        String out = "";
        out += String.format("Algorithm " + algorithm);
        out += String.format("-- average CPU burst time: %.3f ms\n", format_double(burst));
        out += String.format("-- average wait time: %.3f ms\n", format_double(wait));
        out += String.format("-- average turnaround time: %.3f ms\n", format_double(turnaround));
        out += String.format("-- total number of context switches: %d\n", switches);
        out += String.format("-- total number of preemptions: %d\n", preemptions);
        out += String.format("-- CPU utilization: %.3f%%\n", format_double(utilization));
        return out;
    }

    private static double format_double(double d) {
        return Math.ceil(d*1000)/1000;
    }

    public static void print_process(Process process) {
        System.out.printf("Process %s: arrival time %dms; tau %dms; %d CPU bursts:\n", process.name(), process.getArrivalTime(), process.tau, (process.getBursts().size()+1)/2);
        for(int i = 0; i < process.getBursts().size() - 1; i += 2) {
            System.out.printf("--> CPU burst %dms --> I/O burst %dms\n", process.getBursts().get(i), process.getBursts().get(i+1));
        }
        System.out.printf("--> CPU burst %dms\n", process.getBursts().get(process.getBursts().size()-1));
    }

    public static ArrayList<Process> cloneProcesses(ArrayList<Process> processes) {
        ArrayList<Process> out = new ArrayList<>();
        for (Process p : processes) {
            Process newp = new Process(p.id, p.getArrivalTime(), (ArrayList<Long>) p.getBursts().clone());
            out.add(newp);
        }
        return out;
    }

    public static String queueString(ArrayList<Process> queue) {
        String out = "[Q:";
        if(queue.size() > 0) for(Process ps : queue) out += (" " + ps.name());
        else out += (" empty");
        out += ("]");
        return out;
    }

    public static long FCFS(ArrayList<Process> processes, BufferedWriter writer) {
        return RR(0, processes, writer);
    }

    public static long SJF(ArrayList<Process> processes, BufferedWriter writer){
        processes.sort(arrivalComparator);
        ArrayList<Process> queue = new ArrayList<Process>();
        for(Process p : processes) {
            System.out.println(p);
        }


        long time = 0;
        while (!processes.isEmpty()){
            if(processes.get(0).getArrivalTime() == time) {
                queue.add(processes.get(0));
                queue.sort(runtimeComparator);
            }
            time++;
        }

        return time;
    }

    public static long RR(long time_limit, ArrayList<Process> processes, BufferedWriter writer){
        long time = 0;
        int context_switches = 0;
        int preemptions = 0;
        int utilized = 0;
        int burst_sum = 0;
        int burst_count = 0;
        for(Process p : processes){
            ArrayList<Long> bursts = p. getBursts();
            for(int i = 0; i < bursts.size(); i+=2){
                burst_sum+=bursts.get(i);
                burst_count++;
            }
        }
        double average_burst_time = burst_sum / (double) burst_count;
        int turnaround_time = 0;
        int wait_time = 0;


        processes.sort(arrivalComparator);
        ArrayList<Process> queue = new ArrayList<>();
        ArrayList<Process> waiting = new ArrayList<>();

        long elapsed = 0;
        long context_wait = 0;
        Process curr_proc = null;
        Process to_add = null;
        Process assigned = null;
        while (!queue.isEmpty() || curr_proc!=null || !processes.isEmpty() || !waiting.isEmpty() || to_add != null || assigned != null) {

            //1 compare current process
           if(curr_proc != null && context_wait == 0) {
               if(curr_proc.getBursts().get(curr_proc.current_burst) == curr_proc.elapsed_time) {
                   // we are done with the current process before it reaches its allotted time
                   curr_proc.elapsed_time = 0;
                   curr_proc.nextBurst(); // use that method instead of curr_proc.current_burst++;

                   turnaround_time += curr_proc.turnaround_time;
                   curr_proc.turnaround_time = 0;
                   wait_time += curr_proc.wait_time;
                   curr_proc.wait_time = 0;

                   if(curr_proc.current_burst == curr_proc.getBursts().size()){
                       // the current process is done with its bursts and terminates
                       System.out.println("time " + time + "ms: Process " + curr_proc.name() + " terminated " + queueString(queue));

                   } else {
                       // the current process has more bursts and goes to the back of the queue

                       int remaining_bursts =  ((curr_proc.getBursts().size()/2) - curr_proc.current_burst/2);
                       String s = remaining_bursts == 1 ? "" : "s";
                       System.out.println("time " + time + "ms: Process " + curr_proc.name() + " completed a CPU burst; " + remaining_bursts +
                                            " burst" + s + " to go " + queueString(queue));
                       System.out.println("time " + time + "ms: Process " + curr_proc.name() + " switching out of CPU; will block on I/O until time " +
                                            (time + curr_proc.getBursts().get(curr_proc.current_burst) + tcs / 2) + "ms " + queueString(queue));
                       curr_proc.waiting = true;
                       long iotime = curr_proc.getBursts().get(curr_proc.current_burst);
                       curr_proc.getBursts().set(curr_proc.current_burst, iotime + tcs/2);
                       waiting.add(curr_proc);
                       waiting.sort(idComparator);
                   }
                   curr_proc = null;
                   context_wait += tcs / 2;
                   context_switches++;
                   elapsed = 0;

               } else if(time_limit != 0 && elapsed == time_limit) {
                   // we have reached the allotted time for the round-robin: switch this process out
                   if(queue.isEmpty()){
                       System.out.println("time " + time + "ms: Time slice expired; no preemption because ready queue is empty " + queueString(queue));
                   } else {
                       context_wait += tcs / 2;
                       long remaining_time = curr_proc.getBursts().get(curr_proc.current_burst) - curr_proc.elapsed_time;
                       System.out.println("time " + time + "ms: Time slice expired; process " + curr_proc.name() +
                               " preempted with " + remaining_time + "ms remaining " + queueString(queue));
                       //queue.add(curr_proc);
                       preemptions++;
                       to_add = curr_proc;
                       curr_proc = null;

                   }
                   elapsed = 0;
               }
           }

            //2 compare waiting processes
            for(int i = 0; i < waiting.size(); i++){
                Process p = waiting.get(i);
                if(p.elapsed_time == p.getBursts().get(p.current_burst)){
                    // process is done waiting on I/O
                    p.waiting = false;
                    p.elapsed_time = 0;
                    p.current_burst++;
                    waiting.remove(p);
                    queue.add(p);
                    i--;
                    System.out.println("time " + time + "ms: Process " + p.name() + " completed I/O; added to ready queue " + queueString(queue));
                }
            }

            //3 arrival check
            if(!processes.isEmpty() && processes.get(0).getArrivalTime() == time) {
                Process p = processes.get(0);
                processes.remove(0);
                queue.add(p);
                System.out.println("time " + time + "ms: Process " + p.name() + " arrived; added to ready queue " + queueString(queue));
            }

            //4 assign cpu
            if(curr_proc == null && context_wait == 0){
                if(queue.size() > 0){
                    // the cpu is free assign a ready process to
                    //curr_proc = queue.remove(0);
                    assigned = queue.remove(0);
                    context_wait += tcs/2;
                    context_switches++;
                }
            }

            //5 run current
            if(context_wait == 0  && curr_proc != null) {
                curr_proc.elapsed_time++;
                utilized++;
                elapsed++;
            }
            for(Process p : queue){
                if(p.elapsed_time != 0) p.turnaround_time++;
                p.wait_time++;
            }
            if(assigned != null) assigned.turnaround_time++;
            if(curr_proc != null) curr_proc.turnaround_time++;

            //6 run waiting
            for(Process p : waiting) p.elapsed_time++;


            if(context_wait > 0) {
                context_wait--;
                if(context_wait == 0 && to_add != null) {
                    //context just switched
                        queue.add(to_add);
                        to_add = null;
                }
                if(context_wait == 0 && assigned != null) {
                    long burst_length = assigned.getBursts().get(assigned.current_burst);
                    String message = "time " + (time + 1) + "ms: Process " + assigned.name() +
                            " started using the CPU for ";
                    if(assigned.elapsed_time != 0) message += "remaining " + (burst_length - assigned.elapsed_time) + "ms of ";
                    message += burst_length + "ms burst " + queueString(queue);
                    System.out.println(message);
                    curr_proc = assigned;
                    assigned = null;
                }
            }
            time++;
        }

        //1 compare current process
        //2 compare waiting processes
        //3 arrival check
        //4 assign cpu
        //5 run current
        //6 run waiting
        
        double average_turnaround = turnaround_time / (double) burst_count;
        double average_wait = wait_time / (double) burst_count;
        double cpu_utilization = utilized / (double) time;

        // write stats to file
        try {
            String algorithm = (time_limit != 0) ? "RR" : "FCFS";
            writer.write(get_stats(algorithm, average_burst_time, average_wait, average_turnaround, context_switches, preemptions, cpu_utilization));
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return time + context_wait;
    }

    public static void test() {
        RNG rng = new RNG(seed, lambda, upper_bound);

        ArrayList<Process> processes = rng.processes(n);

        File file = new File("simout.txt");
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            System.err.println("ERROR: could not create file 'simout.txt'");
            System.exit(1);
        }

        for (Process p : processes) {
            print_process(p);
        }
        System.out.println();

        run_processes(processes, writer);
    }

    public static void run_processes(ArrayList<Process> processes, BufferedWriter writer) {
        System.out.println("\ntime " + 0 + "ms: Simulator started for FCFS [Q: empty]");
        long FCFS_time = FCFS(cloneProcesses(processes), writer);
        System.out.println("time " + FCFS_time + "ms: Simulator ended for FCFS [Q: empty]");

        //System.out.println("\ntime " + 0 + "ms: Simulator started for SJF [Q: empty]");
        //long SJF_time = SJF(cloneProcesses(processes), writer);
        //System.out.println("time " + SJF_time + "ms: Simulator ended for SJF [Q: empty]");

        //System.out.println("\ntime " + 0 + "ms: Simulator started for SRT [Q: empty]");
        //long SRT_time = SRT(cloneProcesses(processes), writer);
        //System.out.println("time " + SRT_time + "ms: Simulator ended for SRT [Q: empty]");

        System.out.println("\ntime " + 0 + "ms: Simulator started for RR with time slice " + tslice +"ms [Q: empty]");
        long RR_time = RR(tslice, cloneProcesses(processes), writer);
        System.out.println("time " + RR_time + "ms: Simulator ended for RR [Q: empty]");
    }

}
