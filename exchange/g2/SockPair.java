package exchange.g2;

import exchange.sim.Sock;

public class SockPair implements Comparable<SockPair> {
    double distance;
    Sock s1;
    Sock s2;
    double totalCentroidDistance = 0;

    public SockPair(Sock s1, Sock s2) {
        this.s1 = s1;
        this.s2 = s2;
        this.distance = s1.distance(s2);
    }

    public int compareTo(SockPair s) {
        return (this.distance > s.distance) ? 1 : -1;
    }

    public String toString() {
        return this.s1 + "; " + this.s2 + "; " + this.distance;
    }
}
