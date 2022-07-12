import java.math.BigInteger;
import java.util.ArrayList;

/* Emulate C drand48() functionality */
public class RNG {
    private static final long m48 = 1L << 48;

    private final BigInteger seed;
    private final double lambda;
    private final BigInteger upper_bound;
    private BigInteger x;

    public RNG(long seed, double lambda, long upper_bound) {
        this.seed = prepare_seed(seed);
        this.x = this.seed;
        this.lambda = lambda;
        this.upper_bound = BigInteger.valueOf(upper_bound);
    }

    private BigInteger prepare_seed(long seed) {
        return BigInteger.valueOf(((seed & 0xffffffffL) << 16) | 0x330eL);
    }

    private BigInteger next_int(BigInteger x) {
        return BigInteger.valueOf(0x5deece66dL).multiply(x).add(BigInteger.valueOf(0xbL)).mod(BigInteger.valueOf(m48));
    }

    public double next_uniform() {
        x = next_int(x);
        return x.doubleValue() / m48;
    }

    public double next_exp() {
        double r, exp;
        do {
            r = next_uniform();
            exp = -Math.log(r) / lambda;
        } while (exp > upper_bound.longValue());
        return exp;
    }

    public ArrayList<Long> arrival_times(long nproc) {
        ArrayList<Long> l = new ArrayList<>((int) nproc);
        for (int i = 0; i < nproc; ++i)
            l.add((long) Math.floor(next_exp()));
        return l;
    }

    public ArrayList<ArrayList<Long>> bursts(long nproc) {
        ArrayList<ArrayList<Long>> b = new ArrayList<>((int) nproc);

        /* Number of CPU bursts */
        ArrayList<Integer> ncpu = new ArrayList<>((int) nproc);
        for (int i = 0; i < nproc; ++i)
            ncpu.add((int) Math.ceil(100 * next_uniform()));

        /* CPU and IO Bursts */
        for (int i = 0; i < nproc; ++i) {
            ArrayList<Long> bi = new ArrayList<>(2 * ncpu.get(i) - 1);
            for (int j = 0; j < 2 * ncpu.get(i) - 1; ++j)
                bi.add((long) Math.ceil((j % 2 == 0 ? 1 : 10) * next_exp()));
            b.add(bi);
        }

        return b;
    }

}
