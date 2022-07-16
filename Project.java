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
    private static Comparator<Process> tauComparator = new Comparator<Process>(){
        @Override
        public int compare(Process o1, Process o2) {
            int i = Long.compare(o1.tau - o1.elapsed_time, o2.tau - o2.elapsed_time);
            if(i == 0) return idComparator.compare(o1, o2);
            else return i;
        }
    };

    // for process queue to know when processes arrive
    private static Comparator<Process> arrivalComparator = new Comparator<Process>(){
        @Override
        public int compare(Process o1, Process o2) {
            int i = Long.compare(o1.getArrivalTime(), o2.getArrivalTime());
            if(i == 0) return idComparator.compare(o1, o2);
            else return i;
        }
    };

    // for SRT
    private static Comparator<Process> runtimeComparator = new Comparator<Process>(){
        @Override
        public int compare(Process o1, Process o2) {
            int i = Long.compare(o1.tau, o2.tau);
            if(i == 0) return idComparator.compare(o1, o2);
            else return i;
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
        out += String.format("Algorithm " + algorithm + "\n");
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
        long bursts = (process.getBursts().size()+1)/2;
        String plural = (bursts == 1) ? "" : "s";
        System.out.printf("Process %s: arrival time %dms; tau %dms; %d CPU burst" + plural + ":\n", process.name(), process.getArrivalTime(), process.tau, bursts);
        for(int i = 0; i < process.getBursts().size() - 1; i += 2) {
            System.out.printf("--> CPU burst %dms --> I/O burst %dms\n", process.getBursts().get(i), process.getBursts().get(i+1));
        }
        System.out.printf("--> CPU burst %dms\n", process.getBursts().get(process.getBursts().size()-1));
    }

    public static ArrayList<Process> cloneProcesses(ArrayList<Process> processes) {
        ArrayList<Process> out = new ArrayList<>();
        for (Process p : processes) {
            /* To fix annoying uncheck cast error */
            ArrayList<Long> cloned = new ArrayList<>(p.getBursts());
            Process newp = new Process(p.id, p.getArrivalTime(), cloned);
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

    // In SJF, processes are stored in the ready queue in order of priority based on their anticipated CPU
    // burst times. More specifically, the process with the shortest predicted CPU burst time will be
    // selected as the next process executed by the CPU.
    public static long SJF(ArrayList<Process> processes, BufferedWriter writer){
        long time = 0;
        int context_switches = 0;
        int preemptions = 0;
        int utilized = 0;
        int burst_sum = 0;
        int burst_count = 0;

        for (Process p : processes) {
            ArrayList<Long> bursts = p.getBursts();
            for(int i = 0; i < bursts.size(); i+=2){
                burst_sum += bursts.get(i);
                burst_count++;
            }
        }
        double average_burst_time = burst_sum / (double) burst_count;
        int turnaround_time = 0;
        int wait_time = 0;

        processes.sort(arrivalComparator);
        ArrayList<Process> queue = new ArrayList<>();
        ArrayList<Process> waiting = new ArrayList<>();

        long context_wait = 0;
        Process curr_proc = null;
        Process to_add = null;
        Process from_queue = null;
        while (!queue.isEmpty() || curr_proc!=null || !processes.isEmpty() || !waiting.isEmpty() || to_add != null || from_queue != null) {

            //1 compare current process
            if(curr_proc != null && context_wait == 0) {
                if(curr_proc.getBursts().get(curr_proc.current_burst) == curr_proc.elapsed_time) {
                    // we are done with the current process before it reaches its allotted time
                    curr_proc.elapsed_time = 0;
                    long old_tau = curr_proc.tau;
                    curr_proc.current_burst++; // use that method instead of curr_proc.current_burst++;

                    turnaround_time += curr_proc.turnaround_time;
                    curr_proc.turnaround_time = 0;
                    wait_time += curr_proc.wait_time;
                    curr_proc.wait_time = 0;

                    if(curr_proc.current_burst == curr_proc.getBursts().size()){
                        // the current process is done with its bursts and terminates
                        System.out.println("time " + time + "ms: Process " + curr_proc.name() + " terminated " + queueString(queue));
                    } else {
                        // the current process has more bursts and goes to the back of the queue

                        int remaining_bursts = ((curr_proc.getBursts().size()/2) - curr_proc.current_burst/2);
                        String s = remaining_bursts == 1 ? "" : "s";
                        if (time < 1000)
                            System.out.println("time " + time + "ms: Process " + curr_proc.name() + " (tau " + curr_proc.tau + "ms) completed a CPU burst; " + remaining_bursts +
                                " burst" + s + " to go " + queueString(queue));

                        //TAU
                        curr_proc.recalculateTau();
                        if (time < 1000)
                            System.out.printf("time %dms: Recalculated tau for process %s: old tau %dms; new tau %dms %s\n",
                                    time, curr_proc.name(), old_tau, curr_proc.tau, queueString(queue));
                        queue.sort(runtimeComparator);


                        if (time < 1000)
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
                    // context_switches++;
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
                    queue.sort(runtimeComparator);
                    i--;
                    if (time < 1000)
                        System.out.println("time " + time + "ms: Process " + p.name() + " (tau " + p.tau + "ms) completed I/O; added to ready queue " + queueString(queue));
                }
            }

            //3 arrival check
            if(!processes.isEmpty() && processes.get(0).getArrivalTime() == time) {
                Process p = processes.get(0);
                processes.remove(0);
                queue.add(p);
                queue.sort(runtimeComparator);
                if (time < 1000)
                    System.out.println("time " + time + "ms: Process " + p.name()  + " (tau " + p.tau + "ms) arrived; added to ready queue " + queueString(queue));
            }

            //4 assign cpu
            if(curr_proc == null && context_wait == 0){
                if(queue.size() > 0){
                    // the cpu is free assign a ready process to
                    //curr_proc = queue.remove(0);
                    from_queue = queue.remove(0);
                    context_wait += tcs/2;
                    context_switches++;
                }
            }

            //5 run current
            if(context_wait == 0  && curr_proc != null) {
                curr_proc.elapsed_time++;
                utilized++;
            }
            for(Process p : queue){
                p.turnaround_time++;
                p.wait_time++;
            }
            if(from_queue != null) from_queue.turnaround_time++;
            if(curr_proc != null) curr_proc.turnaround_time++;
            if(to_add != null) to_add.turnaround_time++;
            if(from_queue != null) from_queue.turnaround_time++;

            //6 run waiting
            for(Process p : waiting) p.elapsed_time++;


            if(context_wait > 0) {
                context_wait--;
                if(context_wait == 0 && to_add != null) {
                    //context just switched
                    queue.add(to_add);
                    to_add = null;
                    queue.sort(runtimeComparator);
                }
                if(context_wait == 0 && from_queue != null) {
                    long burst_length = from_queue.getBursts().get(from_queue.current_burst);
                    String message = "time " + (time + 1) + "ms: Process " + from_queue.name() + " (tau " + from_queue.tau + "ms) started using the CPU for ";
                    if(from_queue.elapsed_time != 0) message += "remaining " + (burst_length - from_queue.elapsed_time) + "ms of ";
                    message += burst_length + "ms burst " + queueString(queue);
                    if (time < 1000)
                        System.out.println(message);
                    curr_proc = from_queue;
                    from_queue = null;
                    queue.sort(runtimeComparator);
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
        time += context_wait;
        double cpu_utilization = 100 * utilized / (double) time;

        // write stats to file
        try {
            writer.write(get_stats("SJF", average_burst_time, average_wait, average_turnaround, context_switches, preemptions, cpu_utilization));
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return time;
    }

    // The SRT algorithm is a preemptive version of the SJF algorithm. In SRT, when a process arrives,
    // as it enters the ready queue, if it has a predicted CPU burst time that is less than the remaining
    // predicted time of the currently running process, a preemption occurs. When such a preemption
    // occurs, the currently running process is added back to the ready queue.
    public static long SRT(ArrayList<Process> processes, BufferedWriter writer) {
        boolean srtdebug = false;
        long time = 0;
        int context_switches = 0;
        int preemptions = 0;
        int utilized = 0;
        int burst_sum = 0;
        int burst_count = 0;

        for (Process p : processes) {
            ArrayList<Long> bursts = p.getBursts();
            for(int i = 0; i < bursts.size(); i+=2){
                burst_sum += bursts.get(i);
                burst_count++;
            }
        }
        double average_burst_time = burst_sum / (double) burst_count;
        int turnaround_time = 0;
        int wait_time = 0;

        processes.sort(arrivalComparator);
        ArrayList<Process> queue = new ArrayList<>();
        ArrayList<Process> waiting = new ArrayList<>();

        long context_wait = 0;
        Process curr_proc = null;
        Process to_add = null;
        Process from_queue = null;
        while (!queue.isEmpty() || curr_proc!=null || !processes.isEmpty() || !waiting.isEmpty() || to_add != null || from_queue != null) {

            //1 compare current process
            if(curr_proc != null && context_wait == 0) {
                if(curr_proc.getBursts().get(curr_proc.current_burst) == curr_proc.elapsed_time) {
                    // we are done with the current process before it reaches its allotted time
                    curr_proc.elapsed_time = 0;
                    long old_tau = curr_proc.tau;
                    curr_proc.current_burst++; // use that method instead of curr_proc.current_burst++;

                    turnaround_time += curr_proc.turnaround_time;
                    curr_proc.turnaround_time = 0;
                    wait_time += curr_proc.wait_time;
                    curr_proc.wait_time = 0;

                    if(curr_proc.current_burst == curr_proc.getBursts().size()){
                        // the current process is done with its bursts and terminates
                        System.out.println("time " + time + "ms: Process " + curr_proc.name() + " terminated " + queueString(queue));
                    } else {
                        // the current process has more bursts and goes to the back of the queue

                        int remaining_bursts = ((curr_proc.getBursts().size()/2) - curr_proc.current_burst/2);
                        String s = remaining_bursts == 1 ? "" : "s";
                        if (srtdebug || time < 1000)
                            System.out.println("time " + time + "ms: Process " + curr_proc.name() + " (tau " + curr_proc.tau + "ms) completed a CPU burst; " + remaining_bursts +
                                    " burst" + s + " to go " + queueString(queue));

                        //TAU
                        curr_proc.recalculateTau();
                        if (srtdebug || time < 1000)
                            System.out.printf("time %dms: Recalculated tau for process %s: old tau %dms; new tau %dms %s\n",
                                    time, curr_proc.name(), old_tau, curr_proc.tau, queueString(queue));
                        queue.sort(tauComparator);


                        if (srtdebug || time < 1000)
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
                }
            }

            Process to_be_preempted = null;
            //2 compare waiting processes
            for(int i = 0; i < waiting.size(); i++){
                Process p = waiting.get(i);
                if(p.elapsed_time == p.getBursts().get(p.current_burst)){
                    // process is done waiting on I/O
                    p.waiting = false;
                    p.elapsed_time = 0;
                    p.current_burst++;
                    waiting.remove(p);
                    if (to_be_preempted == null && curr_proc != null && p.tau < curr_proc.tau - curr_proc.elapsed_time) {
                        // then preempt
                        to_be_preempted = p;
                        queue.add(p);
                        queue.sort(tauComparator);
                        if (srtdebug || time < 1000)
                            System.out.printf("time %dms: Process %s (tau %dms) completed I/O; preempting %s %s\n",
                                    time, p.name(), p.tau, curr_proc.name(), queueString(queue));
                        queue.add(curr_proc);
                        queue.sort(tauComparator);
                        preemptions++;
                        context_wait += tcs/2;
                        to_be_preempted.wait_time -= tcs/2;
                        to_be_preempted.turnaround_time -= tcs/2;
                        curr_proc = null;
                    } else {
                        queue.add(p);
                        queue.sort(tauComparator);
                        if (srtdebug || time < 1000)
                            System.out.println("time " + time + "ms: Process " + p.name() + " (tau " + p.tau + "ms) completed I/O; added to ready queue " + queueString(queue));
                    }
                    i--;
                }
            }

            //3 arrival check
            if(!processes.isEmpty() && processes.get(0).getArrivalTime() == time) {
                Process p = processes.get(0);
                processes.remove(0);
                queue.add(p);
                queue.sort(tauComparator);
                if (srtdebug || time < 1000)
                    System.out.println("time " + time + "ms: Process " + p.name()  + " (tau " + p.tau + "ms) arrived; added to ready queue " + queueString(queue));
            }

            //4 assign cpu
            if(curr_proc == null && context_wait == 0){
                if(queue.size() > 0){
                    // the cpu is free assign a ready process to
                    //curr_proc = queue.remove(0);
                    from_queue = queue.remove(0);
                    context_wait += tcs/2;
                    context_switches++;
                }
            }

            //5 run current
            if(context_wait == 0  && curr_proc != null) {
                curr_proc.elapsed_time++;
                utilized++;
            }
            for(Process p : queue){
                if (p != to_be_preempted) {
                    p.turnaround_time++;
                }
                p.wait_time++;
            }
            if(from_queue != null) from_queue.turnaround_time++;
            if(from_queue != null) from_queue.turnaround_time++;
            if(curr_proc != null) curr_proc.turnaround_time++;
            // if(to_add != null) to_add.turnaround_time++;
            if(to_be_preempted != null) to_be_preempted.turnaround_time++;

            //6 run waiting
            for(Process p : waiting) p.elapsed_time++;


            if(context_wait > 0) {
                context_wait--;
                if(context_wait == 0 && to_add != null) {
                    //context just switched
                    queue.add(to_add);
                    to_add = null;
                    queue.sort(tauComparator);
                }
                if (context_wait == 0 && to_be_preempted != null) {
                    curr_proc = to_be_preempted;
                    System.out.printf("time %dms: Process %s (tau %dms) started using the CPU for %dms burst %s\n",
                            time, to_be_preempted.name(), to_be_preempted.tau, to_be_preempted.getCurrentBurst());
                    to_be_preempted = null;
                } else if(context_wait == 0 && from_queue != null) {
                    long burst_length = from_queue.getBursts().get(from_queue.current_burst);
                    String message = "time " + (time + 1) + "ms: Process " + from_queue.name() + " (tau " + from_queue.tau + "ms) started using the CPU for ";
                    if(from_queue.elapsed_time != 0) message += "remaining " + (burst_length - from_queue.elapsed_time) + "ms of ";
                    message += burst_length + "ms burst " + queueString(queue);
                    if (srtdebug || time < 1000)
                        System.out.println(message);
                    curr_proc = from_queue;
                    from_queue = null;
                    queue.sort(tauComparator);
                }
            }
            time++;
        }

        double average_turnaround = turnaround_time / (double) burst_count;
        double average_wait = wait_time / (double) burst_count;
        time += context_wait;
        double cpu_utilization = 100 * utilized / (double) time;

        // write stats to file
        try {
            writer.write(get_stats("SRT", average_burst_time, average_wait, average_turnaround, context_switches, preemptions, cpu_utilization));
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        Process from_queue = null;
        while (!queue.isEmpty() || curr_proc!=null || !processes.isEmpty() || !waiting.isEmpty() || to_add != null || from_queue != null) {

            //1 compare current process
           if(curr_proc != null && context_wait == 0) {
               if(curr_proc.getBursts().get(curr_proc.current_burst) == curr_proc.elapsed_time) {
                   // we are done with the current process before it reaches its allotted time
                   curr_proc.elapsed_time = 0;
                   curr_proc.current_burst++;


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
                       if (time < 1000) {
                           System.out.println("time " + time + "ms: Process " + curr_proc.name() + " completed a CPU burst; " + remaining_bursts +
                                   " burst" + s + " to go " + queueString(queue));
                           System.out.println("time " + time + "ms: Process " + curr_proc.name() + " switching out of CPU; will block on I/O until time " +
                                   (time + curr_proc.getBursts().get(curr_proc.current_burst) + tcs / 2) + "ms " + queueString(queue));
                       }
                       curr_proc.waiting = true;
                       long iotime = curr_proc.getBursts().get(curr_proc.current_burst);
                       curr_proc.getBursts().set(curr_proc.current_burst, iotime + tcs/2);
                       waiting.add(curr_proc);
                       waiting.sort(idComparator);
                   }
                   curr_proc = null;
                   context_wait += tcs / 2;
                  // context_switches++;
                   elapsed = 0;

               } else if(time_limit != 0 && elapsed == time_limit) {
                   // we have reached the allotted time for the round-robin: switch this process out
                   if(queue.isEmpty()){
                       if (time < 1000)
                           System.out.println("time " + time + "ms: Time slice expired; no preemption because ready queue is empty " + queueString(queue));
                   } else {
                       context_wait += tcs / 2;
                       long remaining_time = curr_proc.getBursts().get(curr_proc.current_burst) - curr_proc.elapsed_time;
                       if (time < 1000)
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
                    if (time < 1000)
                        System.out.println("time " + time + "ms: Process " + p.name() + " completed I/O; added to ready queue " + queueString(queue));
                }
            }

            //3 arrival check
            if(!processes.isEmpty() && processes.get(0).getArrivalTime() == time) {
                Process p = processes.get(0);
                processes.remove(0);
                queue.add(p);
                if (time < 1000)
                    System.out.println("time " + time + "ms: Process " + p.name() + " arrived; added to ready queue " + queueString(queue));
            }

            //4 assign cpu
            if(curr_proc == null && context_wait == 0){
                if(queue.size() > 0){
                    // the cpu is free assign a ready process to
                    //curr_proc = queue.remove(0);
                    from_queue = queue.remove(0);
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
                p.turnaround_time++;
                p.wait_time++;
            }
            if(from_queue != null) from_queue.turnaround_time++;
            if(curr_proc != null) curr_proc.turnaround_time++;
            //if(to_add != null) to_add.turnaround_time++;
            if(from_queue != null) from_queue.turnaround_time++;


            //6 run waiting
            for(Process p : waiting) p.elapsed_time++;


            if(context_wait > 0) {
                context_wait--;
                if(context_wait == 0 && to_add != null) {
                    //context just switched
                        queue.add(to_add);
                        to_add = null;
                }
                if(context_wait == 0 && from_queue != null) {
                    long burst_length = from_queue.getBursts().get(from_queue.current_burst);
                    String message = "time " + (time + 1) + "ms: Process " + from_queue.name() +
                            " started using the CPU for ";
                    if(from_queue.elapsed_time != 0) message += "remaining " + (burst_length - from_queue.elapsed_time) + "ms of ";
                    message += burst_length + "ms burst " + queueString(queue);
                    if (time < 1000)
                        System.out.println(message);
                    curr_proc = from_queue;
                    from_queue = null;
                }
            }
            time++;
        }
        
        double average_turnaround = turnaround_time / (double) burst_count;
        double average_wait = wait_time / (double) burst_count;
        time += context_wait;
        double cpu_utilization = 100 * utilized / (double) time;

        // write stats to file
        try {
            String algorithm = (time_limit != 0) ? "RR" : "FCFS";
            writer.write(get_stats(algorithm, average_burst_time, average_wait, average_turnaround, context_switches, preemptions, cpu_utilization));
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return time;
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

        run_processes(processes, writer);
    }

    public static void run_processes(ArrayList<Process> processes, BufferedWriter writer) {
        System.out.println("\ntime " + 0 + "ms: Simulator started for FCFS [Q: empty]");
        long FCFS_time = FCFS(cloneProcesses(processes), writer);
        System.out.println("time " + FCFS_time + "ms: Simulator ended for FCFS [Q: empty]");

        System.out.println("\ntime " + 0 + "ms: Simulator started for SJF [Q: empty]");
        long SJF_time = SJF(cloneProcesses(processes), writer);
        System.out.println("time " + SJF_time + "ms: Simulator ended for SJF [Q: empty]");

        System.out.println("\ntime " + 0 + "ms: Simulator started for SRT [Q: empty]");
        long SRT_time = SRT(cloneProcesses(processes), writer);
        System.out.println("time " + SRT_time + "ms: Simulator ended for SRT [Q: empty]");

        System.out.println("\ntime " + 0 + "ms: Simulator started for RR with time slice " + tslice +"ms [Q: empty]");
        long RR_time = RR(tslice, cloneProcesses(processes), writer);
        System.out.println("time " + RR_time + "ms: Simulator ended for RR [Q: empty]");
    }

}
