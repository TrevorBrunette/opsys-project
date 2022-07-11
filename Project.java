import java.util.ArrayList;
import java.util.Arrays;

public class Project {

    static long n;
    static long seed;
    static double lambda;
    static long upper_bound;
    static long tcs;
    static long tau;
    static long tsclice;





    public static void main(String[] args) {
//        if ( args.length != 8 ){
//            System.err.println("Incorrect Number of Arguments.");
//        }
//
//        try {
//            n = Long.parseLong(args[1]);
//            seed = Long.parseLong(args[2]);
//            lambda = Double.parseDouble(args[3]);
//            upper_bound = Long.parseLong(args[4]);
//            tcs = Long.parseLong(args[5]);
//            tau = Long.parseLong(args[6]);
//            tsclice = Long.parseLong(args[7]);
//
//        }
//        catch (NumberFormatException e){
//            System.err.println("Error parsing arguments");
//        }

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



    }

}
