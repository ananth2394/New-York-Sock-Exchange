package exchange.g4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;
import exchange.sim.Transaction;

public class Player extends exchange.sim.Player {
    /*
        Inherited from exchange.sim.Player:
        Random random   -       Random number generator, if you need it

        Remark: you have to manually adjust the order of socks, to minimize the total embarrassment
                the score is calculated based on your returned list of getSocks(). Simulator will pair up socks 0-1, 2-3, 4-5, etc.
     */
    private int id1, id2, id;
    private double maxDistance, minDistance;
    private Sock[] socks;

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);
    }

    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        /*
			lastRequests.get(i)		-		Player i's request last round
			lastTransactions		-		All completed transactions last round.
		*/
        id1 = isolate(-1);
        id2 = isolate(id1);
        return new Offer(socks[id1], socks[id2]);
    }

    @Override
    public Request requestExchange(List<Offer> offers) {
		/*
			offers.get(i)			-		Player i's offer
			For each offer:
			offer.getSock(rank = 1, 2)		-		get rank's offer
			offer.getFirst()				-		equivalent to offer.getSock(1)
			offer.getSecond()				-		equivalent to offer.getSock(2)

			Remark: For Request object, rank ranges between 1 and 2
		 */

		List<Integer> availableOffers = new ArrayList<>();
		for (int i = 0; i < offers.size(); ++ i) {
		    if (i == id) continue;

		    // Encoding the offer information into integer: id * 2 + rank - 1
            if (offers.get(i).getFirst() != null)
                availableOffers.add(i * 2);
            if (offers.get(i).getSecond() != null)
                availableOffers.add(i * 2 + 1);
        }

        if (availableOffers.size() == 0) 
            return new Request(-1, -1, -1, -1);
        //int expect = availableOffers.get(random.nextInt(availableOffers.size()));
        int expect1 = close(availableOffers, offers, -1);
        if (expect1 == -1)
            return new Request(-1, -1, -1, -1);
        int expect2 = close(availableOffers, offers, expect1);
        if (expect2 == -1)
            return new Request(expect1 / 2, expect1 % 2 + 1, -1, -1);
        return new Request(expect1 / 2, expect1 % 2 + 1, expect2 / 2, expect2 % 2 + 1);
    }

    @Override
    public void completeTransaction(Transaction transaction) {
        /*
            transaction.getFirstID()        -       first player ID of the transaction
            transaction.getSecondID()       -       Similar as above
            transaction.getFirstRank()      -       Rank of the socks for first player
            transaction.getSecondRank()     -       Similar as above
            transaction.getFirstSock()      -       Sock offered by the first player
            transaction.getSecondSock()     -       Similar as above

            Remark: rank ranges between 1 and 2
         */
        int rank;
        Sock newSock;
        if (transaction.getFirstID() == id) {
            rank = transaction.getFirstRank();
            newSock = transaction.getSecondSock();
        } else {
            rank = transaction.getSecondRank();
            newSock = transaction.getFirstSock();
        }
        if (rank == 1) socks[id1] = newSock;
        else socks[id2] = newSock;
    }

    @Override
    public List<Sock> getSocks() {
        return Arrays.asList(socks);
    }
    
    public int isolate(int ignore) {
        double[] dist;
        dist = new double[socks.length];
        for (int i = 0; i < socks.length; i++) {
            if (i == ignore) {
                dist[i] = 0;
                continue;
            }
            double min = 1e9;
            for (int j = 0; j < socks.length; j++) {
                if (i == j) continue;
                double temp = socks[i].distance(socks[j]);
                if (temp < min) {
                    min = temp;
                }
            }
            dist[i] = min;
        }
        maxDistance = 0;
        int mark = -1;
        for (int i = 0; i < socks.length; i++) {
            if (dist[i] > maxDistance) {
                maxDistance = dist[i];
                mark = i;
            }
        }
        //System.out.println(max + " " + mark);
        return mark;
    }
    
    public int close(List<Integer> availableOffers, List<Offer> offers, int ignore) {
        int n = availableOffers.size();
        double[] dist;
        dist = new double[n];
        for (int i = 0; i < n; i++) {
            int k = availableOffers.get(i);
            if (k == ignore) {
                dist[i] = 1e9;
                continue;
            }
            int k1 = k / 2;
            int k2 = k % 2 + 1;
            Sock sock0;
            if (k2 == 1) {
                sock0 = offers.get(k1).getFirst();
            }
            else {
                sock0 = offers.get(k1).getSecond();
            }
            double min = 1e9;
            for (int j = 0; j < socks.length; j++) {
                double temp = sock0.distance(socks[j]);
                if (temp < min) {
                    min = temp;
                }
            }
            dist[i] = min;
        }
        minDistance = 1e9;
        int mark = -1;
        for (int i = 0; i < n; i++) {
            if (dist[i] < minDistance) {
                minDistance = dist[i];
                mark = availableOffers.get(i);
            }
        }
        System.out.println(minDistance + " " + mark);
        if (minDistance < maxDistance) return mark;
        else return -1;
    }
}
