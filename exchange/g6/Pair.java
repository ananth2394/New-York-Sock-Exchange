package exchange.g6;


import exchange.sim.Sock;


public class Pair implements Comparable<Pair> {
    public Sock u;
    public Sock v;

    public Pair(Sock u, Sock v) {
        this.u = u;
        this.v = v;
    }

    public int compareTo(Pair other) {
        return this.weight() > other.weight() ? 1 : -1;
    }

    public double weight() {
        return v.distance(u);
    }
}