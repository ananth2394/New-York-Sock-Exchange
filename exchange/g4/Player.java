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
    private int id1, id2, id3, id4, id;
    private double maxDistance, minDistance;
    private Sock[] socks;
    private double[][] centers;
    private int[] clusters;
    private boolean isTransaction;
    private int current;
    private double[] maxDist;

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);
        this.isTransaction = true;
        this.current = 0;
        this.maxDist = new double[4];
    }

    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        /*
			lastRequests.get(i)		-		Player i's request last round
			lastTransactions		-		All completed transactions last round.
		*/
        if (isTransaction == true) {
            current = 0;
            centers = new double[4][3];
            clusters = new int[socks.length];
            K_means(10);
            current = 1;
            isTransaction = false;
            chooseOffer();
            return new Offer(socks[id1], socks[id2]);
        }
        else {
            if (current == 0) {
                current = 1;
                return new Offer(socks[id1], socks[id2]);
            }
            else {
                current = 0;
                return new Offer(socks[id3], socks[id4]);
            }
        }
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
        
        int expect1 = -1;
        int expect2 = -1;
        if (current == 0) {
            //int expect1 = close(availableOffers, offers, -1);
            //int expect2 = close(availableOffers, offers, expect1);
            expect1 = chooseRequest(availableOffers, offers, id1, 0);
            expect2 = chooseRequest(availableOffers, offers, id2, 1);
        }
        else {
            expect1 = chooseRequest(availableOffers, offers, id3, 2);
            expect2 = chooseRequest(availableOffers, offers, id4, 3);
        }
        if (expect1 == -1) {
            if (expect2 == -1) {
                return new Request(-1, -1, -1, -1);
            }
            else {
                return new Request(expect2 / 2, expect2 % 2 + 1, -1, -1);
            }
        }
        else {
            if (expect2 == -1) {
                return new Request(expect1 / 2, expect1 % 2 + 1, -1, -1);
            }
            else {
                return new Request(expect1 / 2, expect1 % 2 + 1, expect2 / 2, expect2 % 2 + 1);
            }
        }
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
        if (current == 1) {
            if (rank == 1) socks[id1] = newSock;
            else socks[id2] = newSock;
        }
        else {
            if (rank == 1) socks[id3] = newSock;
            else socks[id4] = newSock;
        }
        isTransaction = true;
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
        if (minDistance < maxDistance) return mark;
        else return -1;
    }
    
    public void K_means(int rounds) {
        centers[0][0] = 64; centers[0][1] = 64; centers[0][2] = 64;
        centers[1][0] = 64; centers[1][1] = 192; centers[1][2] = 192;
        centers[2][0] = 192; centers[2][1] = 192; centers[2][2] = 64;
        centers[3][0] = 192; centers[3][1] = 64; centers[3][2] = 192;
        for (int k = 0; k < rounds; k++) {
            for (int i = 0; i < socks.length; i++) {
                double min = 1e9;
                for (int j = 0; j < 4; j++) {
                    double dist = Math.sqrt(Math.pow(socks[i].R - centers[j][0], 2) + 
                    Math.pow(socks[i].G - centers[j][1], 2) + Math.pow(socks[i].B - centers[j][2], 2));
                    if (dist < min) {
                        min = dist;
                        clusters[i] = j;
                    }
                }
            }
            double[][] new_centers;
            int[] count;
            new_centers = new double[4][3];
            count = new int[4];
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 3; j++) {
                    new_centers[i][j] = 0;
                }
                count[i] = 0;
            }
            for (int i = 0; i < socks.length; i++) {
                int t = clusters[i];
                count[t] ++;
                new_centers[t][0] += socks[i].R;
                new_centers[t][1] += socks[i].G;
                new_centers[t][2] += socks[i].B;
            }
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 3; j++) {
                    centers[i][j] = new_centers[i][j] / count[i];
                }
                //System.out.println(count[i]);
                //System.out.println(centers[i][0] + " " + centers[i][1] + " " + centers[i][2]);
            }
        }
    }
    
    public void chooseOffer() {
        double[] maxFirst;
        double[] maxSecond;
        int[] mark;
        maxFirst = new double[4];
        maxSecond = new double[4];
        mark = new int[4];
        for (int i = 0; i < 4; i++) {
            maxFirst[i] = 0;
            maxSecond[i] = 0;
            mark[i] = 0;
        }
        for (int i = 0; i < socks.length; i++) {
            int j = clusters[i];
            double dist = Math.sqrt(Math.pow(socks[i].R - centers[j][0], 2) + 
            Math.pow(socks[i].G - centers[j][1], 2) + Math.pow(socks[i].B - centers[j][2], 2));
            if (dist > maxFirst[j]) {
                maxFirst[j] = dist;
            }
        }
        for (int i = 0; i < socks.length; i++) {
            int j = clusters[i];
            double dist = Math.sqrt(Math.pow(socks[i].R - centers[j][0], 2) + 
            Math.pow(socks[i].G - centers[j][1], 2) + Math.pow(socks[i].B - centers[j][2], 2));
            if ((dist > maxSecond[j]) && (dist < maxFirst[j])) {
                maxSecond[j] = dist;
                mark[j] = i;
            }
        }
        id1 = mark[0];
        id2 = mark[1];
        id3 = mark[2];
        id4 = mark[3];
        maxDist[0] = maxSecond[0];
        maxDist[1] = maxSecond[1];
        maxDist[2] = maxSecond[2];
        maxDist[3] = maxSecond[3];
    }
    
    public int chooseRequest(List<Integer> availableOffers, List<Offer> offers, int identity, int num) {
        int n = availableOffers.size();
        double[] dist;
        double min = 1e9;
        int mark = 0;
        for (int i = 0; i < n; i++) {
            int k = availableOffers.get(i);
            int k1 = k / 2;
            int k2 = k % 2 + 1;
            Sock sock0;
            if (k2 == 1) {
                sock0 = offers.get(k1).getFirst();
            }
            else {
                sock0 = offers.get(k1).getSecond();
            }
            double temp = sock0.distance(socks[identity]);
            if (temp < min) {
                min = temp;
                mark = k;
            }
        }
        if (min < 2 * maxDist[num]) {
            return mark;
        }
        else return -1;
    }
}
