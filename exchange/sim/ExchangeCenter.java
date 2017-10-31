package exchange.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ExchangeCenter {
    private static Random random = new Random();
    private static List<Event> events = new ArrayList<>();

    public static List<Transaction> exchange(Offer[] offers, Request[] requests) {
        events.clear();
        boolean[][] mark = new boolean[requests.length][];
        for (int i = 0; i < requests.length; ++i) {
            mark[i] = new boolean[3];
            int id1 = requests[i].getFirstID(), id2 = requests[i].getSecondID();
            int rank1 = requests[i].getFirstRank(), rank2 = requests[i].getSecondRank();
            if (id1 != -1) {
                if (requests[id1].getFirstID() == i)
                    events.add(new Event(id1, rank1, i, requests[id1].getFirstRank()));
                if (requests[id1].getSecondID() == i)
                    events.add(new Event(id1, rank1, i, requests[id1].getSecondRank()));
            }
            if (id2 != -1) {
                if (requests[id2].getFirstID() == i)
                    events.add(new Event(id2, rank2, i, requests[id2].getFirstRank()));
                if (requests[id2].getSecondID() == i)
                    events.add(new Event(id2, rank2, i, requests[id2].getSecondRank()));
            }
        }
        events.sort((e1, e2) -> {
            if (e1.rank1 + e1.rank2 == e2.rank1 + e2.rank2)
                return Integer.compare(e1.priority, e2.priority);
            else return Integer.compare(e1.rank1 + e1.rank2, e2.rank1 + e2.rank2);
        });
        List<Transaction> transactions = new ArrayList<>();
        for (Event e : events) {
            System.err.println("" + e.id1 + " " + e.rank1 + " " + e.id2 + " " + e.rank2);
            if (!mark[e.id1][e.rank1] && !mark[e.id2][e.rank2]) {
                // Transaction complete!
                transactions.add(new Transaction(e.id1, e.rank1, e.id2, e.rank2, offers[e.id1].getSock(e.rank1), offers[e.id2].getSock(e.rank2)));
                mark[e.id1][e.rank1] = true;
                mark[e.id2][e.rank2] = true;
            }
        }
        return transactions;
    }

    private static class Event {
        public int id1, id2;
        public int rank1, rank2;
        public int priority;

        public Event(int id1, int rank1, int id2, int rank2) {
            this.id1 = id1;
            this.id2 = id2;
            this.rank1 = rank1;
            this.rank2 = rank2;
            this.priority = random.nextInt();
        }
    }
}
