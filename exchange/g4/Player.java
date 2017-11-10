package exchange.g4;

import exchange.g4.edmonds.SockArrangementFinder;
import exchange.g4.marketvalue.*;
import exchange.g4.socktrader.*;

import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;
import exchange.sim.Transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Player extends exchange.sim.Player {
    /*
     * Inherited from exchange.sim.Player:
     * Random random   -       Random number generator, if you need it
     *
     * Remark: you have to manually adjust the order of socks, to minimize the total embarrassment
     * the score is calculated based on your returned list of getSocks(). Simulator will pair up socks 0-1, 2-3, 4-5, etc.
     */
    private int id;
    private int[] id_offer;
    private double maxDistance, minDistance;
    private Sock[] socks;
    private double[][] centers;
    private int[] clusters;
    private boolean isTransaction;
    private int current;
    private double[] maxDist;


    private SockTrader trader;
    private List<Offer> lastOffers;

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);
        this.isTransaction = true;
        this.current = 0;
        this.maxDist = new double[8];
        this.id_offer = new int[8];

        this.trader = new SockTrader(new ArrayList<Sock>(socks),id);

        this.lastOffers = null;
    }

    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        /*
         * lastRequests.get(i)  -       Player i's request last round
         * lastTransactions     -       All completed transactions last round.
         */

        this.trader.updateInformation(toArrayList(this.socks));
        
        if (isTransaction == true) {
            current = 1;
            isTransaction = false;
            this.trader.makeOffer(lastRequests, lastTransactions);
            return new Offer(socks[trader.sock_id[0]], socks[trader.sock_id[1]]);
        }
        if (current == 0) {
            current = 1;
            return new Offer(socks[trader.sock_id[0]], socks[trader.sock_id[1]]);
        }
        else if (current == 1) {
            current = 2;
            return new Offer(socks[trader.sock_id[2]], socks[trader.sock_id[3]]);
        }
        else if (current == 2) {
            current = 3;
            return new Offer(socks[trader.sock_id[4]], socks[trader.sock_id[5]]);
        }
        else {
            current = 0;
            return new Offer(socks[trader.sock_id[6]], socks[trader.sock_id[7]]);
        }

    }

    @Override
    public Request requestExchange(List<Offer> offers) {
        /*
         * offers.get(i)	        - Player i's offer
         * For each offer:
         * offer.getSock(rank = 1, 2)	- get rank's offer
         * offer.getFirst()		- equivalent to offer.getSock(1)
         * offer.getSecond()		- equivalent to offer.getSock(2)
         *
         * Remark: For Request object, rank ranges between 1 and 2
         */
        
        List<Integer> availableOffers = new ArrayList<>();
        for (int i = 0; i < offers.size(); i++) {
            if (i == id) continue;

            // Encoding the offer information into integer: id * 2 + rank - 1
            if (offers.get(i).getFirst() != null)
                availableOffers.add(i * 2);
            if (offers.get(i).getSecond() != null)
                availableOffers.add(i * 2 + 1);
        }
        if (availableOffers.size() == 0)
            return new Request(-1, -1, -1, -1);
        
        int[] expect;
        expect = new int[2];
        
        if (socks.length > 800) {
            int bias = ((current + 3) % 4) * 2;
            if (availableOffers.size() == 1) {
                int k = availableOffers.get(random.nextInt(availableOffers.size()));
                int k1 = k / 2;
                int k2 = k % 2 + 1;
                Sock sock0;
                if (k2 == 1) {
                    sock0 = offers.get(k1).getFirst();
                }
                else {
                    sock0 = offers.get(k1).getSecond();
                }
                double t1 = picky(k, sock0, trader.sock_id[bias]);
                double t2 = picky(k, sock0, trader.sock_id[bias + 1]);
                if ((t1 > 0) || (t2 > 0)) expect[0] = k;
                else expect[0] = -1;
                expect[1] = -1;
            }
            else {
                int kk[];
                kk = new int[2];
                kk[0] = availableOffers.get(random.nextInt(availableOffers.size()));
                kk[1] = availableOffers.get(random.nextInt(availableOffers.size()));
                while (kk[0] == kk[1])
                    kk[1] = availableOffers.get(random.nextInt(availableOffers.size()));
                for (int i = 0; i < 2; i++) {
                    int k1 = kk[i] / 2;
                    int k2 = kk[i] % 2 + 1;
                    Sock sock0;
                    if (k2 == 1) {
                        sock0 = offers.get(k1).getFirst();
                    }
                    else {
                        sock0 = offers.get(k1).getSecond();
                    }
                    double t1 = picky(kk[i], sock0, trader.sock_id[bias]);
                    double t2 = picky(kk[i], sock0, trader.sock_id[bias + 1]);
                    if ((t1 > 0) || (t2 > 0)) expect[i] = kk[i];
                    else expect[i] = -1;
                }
            }
        }
        else {
            if (current == 0) {
                for (int i = 0; i < 2; i++) {
                    expect[i] = chooseRequest(availableOffers, offers, trader.sock_id[i + 6]);
                }
            }
            else if (current == 1) {
                for (int i = 0; i < 2; i++) {
                    expect[i] = chooseRequest(availableOffers, offers, trader.sock_id[i]);
                }
            }
            else if (current == 2) {
                for (int i = 0; i < 2; i++) {
                    expect[i] = chooseRequest(availableOffers, offers, trader.sock_id[i + 2]);
                }
            }
            else {
                for (int i = 0; i < 2; i++) {
                    expect[i] = chooseRequest(availableOffers, offers, trader.sock_id[i + 4]);
                }
            }
        }

        if (expect[0] == -1) {
            if (expect[1] == -1) {
                return new Request(-1, -1, -1, -1);
            }
            else {
                return new Request(expect[1] / 2, expect[1] % 2 + 1, -1, -1);
            }
        }
        else {
            if (expect[1] == -1) {
                return new Request(expect[0] / 2, expect[0] % 2 + 1, -1, -1);
            }
            else {
                return new Request(expect[0] / 2, expect[0] % 2 + 1, expect[1] / 2, expect[1] % 2 + 1);
            }
        }
    }

    @Override
    public void completeTransaction(Transaction transaction) {
        /*
         * transaction.getFirstID()        -       first player ID of the transaction
         * transaction.getSecondID()       -       Similar as above
         * transaction.getFirstRank()      -       Rank of the socks for first player
         * transaction.getSecondRank()     -       Similar as above
         * transaction.getFirstSock()      -       Sock offered by the first player
         * transaction.getSecondSock()     -       Similar as above
         *
         * Remark: rank ranges between 1 and 2
         */
        int rank;
        Sock newSock;
        if (transaction.getFirstID() == id) {
            rank = transaction.getFirstRank();
            newSock = transaction.getSecondSock();
        }
        else {
            rank = transaction.getSecondRank();
            newSock = transaction.getFirstSock();
        }
        
        if (current == 0) {
            if (rank == 1) socks[trader.sock_id[6]] = newSock;
            else socks[trader.sock_id[7]] = newSock;
        }
        else if (current == 1) {
            if (rank == 1) socks[trader.sock_id[0]] = newSock;
            else socks[trader.sock_id[1]] = newSock;
        }
        else if (current == 2) {
            if (rank == 1) socks[trader.sock_id[2]] = newSock;
            else socks[trader.sock_id[3]] = newSock;
        }
        else {
            if (rank == 1) socks[trader.sock_id[4]] = newSock;
            else socks[trader.sock_id[5]] = newSock;
        }
        
        isTransaction = true;

        this.trader.updateInformation(toArrayList(this.socks));

    }

    @Override
    public List<Sock> getSocks() {
        this.trader.updateInformation(toArrayList(socks));
        ArrayList<Sock> s = new ArrayList(Arrays.asList(this.socks));
        ArrayList<Sock> ans = null;

        if (socks.length > 400) {
            ans = SockHelper.getSocks(s);
        }
        else {
            ans = SockArrangementFinder.getSocks(s);
        }
        return ans;
    }

    public ArrayList<Sock> toArrayList(Sock[] socks) {
        return new ArrayList<Sock>(Arrays.asList(socks));
    }
    
    public int chooseRequest(List<Integer> availableOffers, List<Offer> offers, int identity) {
        int n = availableOffers.size();
        double min = 1e9;
        int mark = 0;
        Sock[] expected;
        expected = new Sock[socks.length];
        Sock s1, s2;
        for (int i = 0; i < socks.length - 1; i += 2) {
            s1 = socks[i];
            s2 = socks[i + 1];
            expected[i] = s1;
            expected[i + 1] = s2;
        }
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
            expected[identity] = sock0;
            double temp = 0;
            ArrayList<Sock> s = new ArrayList(Arrays.asList(expected));
            ArrayList<Sock> ans = null;
            if (socks.length > 30) {
                ans = SockHelper.getSocks(s);
            }
            else {
                ans = SockArrangementFinder.getSocks(s);
            }
            for (int j = 0; j < ans.size() - 1; j += 2) {
                s1 = ans.get(j);
                s2 = ans.get(j + 1);
                temp += s1.distance(s2);
            }
            if (temp < min) {
                min = temp;
                mark = k;
            }
        }
        if (min < 1e8) {
            return mark;
        }
        else return -1;
    }
    
    public double picky(int k, Sock sock0, int identity) {
        Sock[] expected;
        expected = new Sock[socks.length];
        Sock s1, s2;
        for (int i = 0; i < socks.length - 1; i += 2) {
            s1 = socks[i];
            s2 = socks[i + 1];
            expected[i] = s1;
            expected[i + 1] = s2;
        }
        double minPrice = 0;
        ArrayList<Sock> s = new ArrayList(Arrays.asList(socks));
        ArrayList<Sock> ans = null;
        ans = SockHelper.getSocks(s);
        for (int j = 0; j < ans.size() - 1; j += 2) {
            s1 = ans.get(j);
            s2 = ans.get(j + 1);
            minPrice += s1.distance(s2);
        }
        expected[identity] = sock0;
        double temp = 0;
        ArrayList<Sock> ss = new ArrayList(Arrays.asList(expected));
        ans = SockHelper.getSocks(ss);
        for (int j = 0; j < ans.size() - 1; j += 2) {
            s1 = ans.get(j);
            s2 = ans.get(j + 1);
            temp += s1.distance(s2);
        }
        if (temp < minPrice) {
            return temp;
        }
        return -1;
    }
}
