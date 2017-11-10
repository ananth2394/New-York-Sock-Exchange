package exchange.g2;

import exchange.sim.*;
import java.util.*;

public class SockPair implements Comparable<SockPair> {
    double distance;
    Sock s1;
    Sock s2;
    int timesOffered;

    public SockPair(Sock s1, Sock s2) {
        this.s1 = s1;
        this.s2 = s2;
        this.distance = s1.distance(s2);
        this.timesOffered = 0;
    }

    // Reverse comparison function so that the Priority Queue sorts them the way we want.
    public int compareTo(SockPair s) {
        return (this.distance < s.distance)? 1 : -1;
    }

    public String toString(){
        return  this.s1+"; "+this.s2+"; "+this.distance;
    }
}
