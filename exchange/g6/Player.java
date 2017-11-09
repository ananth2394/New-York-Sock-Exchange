package exchange.g6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.*;

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
    private int id1, id2, id, n, t;
    private Sock[] socks;
    private Sock median;

    private double max_dist1;//for stage 0
    private double max_dist2;//for stage 0
    private int no_txn; //count the number times with no txns
    private int pair_offer; //identify stages: pair_offer = 1 for stage1

    private boolean txn = true;

    private double minEmbarassment = Double.MAX_VALUE;
    private double embarrassment = Double.MAX_VALUE;
    private int improvementCounter;

    private List<Offer> lastOffers;
    private Set<Sock> lastTradedSocks;
    private HashMap<Sock, Integer> lastUnfulfilledRequests;

    private Request lastRequest;
    private Pair[] pairs;

    private int playerCloseToOffer;
    private Set<Integer> playersWithInterest = new HashSet<Integer>();


    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.n = n;
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);
        this.median = new Sock(128,128,128);
        this.max_dist1 = -1;
        this.max_dist2 = -1;
        this.t = t;
        this.pairs = new Pair[n];
        this.pairSocks();

        lastOffers = null;
        lastTradedSocks = new HashSet<Sock>();
        lastUnfulfilledRequests = new HashMap<Sock, Integer>();
        playersWithInterest = new HashSet<Integer>();
    }

    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        /*
            lastRequests.get(i)     -       Player i's request last round
            lastTransactions        -       All completed transactions last round.
        */


        /*
        if(t % 3 == 0) {
            this.lastUnfulfilledRequests.clear();
        }
        */

        playersWithInterest.clear();
        Set<Integer> interestingPlayers = new HashSet<Integer>();
        playerCloseToOffer = -2;


        boolean obtainedFirstSock = false;
        boolean obtainedSecondSock = false;

        this.lastTradedSocks.clear();
        for(Transaction t : lastTransactions) {
            lastTradedSocks.add(t.getFirstSock());
            lastTradedSocks.add(t.getSecondSock());
        }

        this.lastUnfulfilledRequests.clear();
        for(int i = 0; i < lastRequests.size(); i++) {
            Request r = lastRequests.get(i);

            if(i == id) {
                if(!this.txn) {
                    interestingPlayers.add(lastRequests.get(id).getFirstID());
                    interestingPlayers.add(lastRequests.get(id).getSecondID());
                }
            } else if(r == null) {
                continue;
            } else {
                if(r.getFirstID() != -1 && r.getFirstRank() != -1) {
                    Sock firstSock = lastOffers.get(r.getFirstID()).getSock(r.getFirstRank());
                    if(!lastTradedSocks.contains(firstSock)) {
                        if(r.getFirstID() == id && !lastTradedSocks.contains(firstSock)) {
                            playersWithInterest.add(i);
                        }
                        lastUnfulfilledRequests.put(firstSock, i);
                    } else {
                        lastUnfulfilledRequests.remove(firstSock);
                    }
                }

                if(r.getSecondID() != -1 && r.getSecondRank() != -1) {
                    Sock secondSock = lastOffers.get(r.getSecondID()).getSock(r.getSecondRank());
                    if(!lastTradedSocks.contains(secondSock)) {
                        if(r.getSecondID() == id && !lastTradedSocks.contains(secondSock)) {
                            playersWithInterest.add(i);
                        }
                        lastUnfulfilledRequests.put(secondSock, i);
                    } else {
                        lastUnfulfilledRequests.remove(secondSock);
                    }
                }
            }
        }

        this.t--;
        if(!this.txn) {
            this.no_txn++;
        } else {
            this.pairSocks();
            this.embarrassment = getEmbarassment(socks);
            if(this.embarrassment < this.minEmbarassment) {
                this.minEmbarassment = this.embarrassment;
            }
        }

        if(this.no_txn % 3 != 2) {
            //Use Blossom method to pair up socks

            double minDist1 = 1000;
            int minTimesOffered = 1000;
            int minId1 = -1;

            for(int i = Math.max(0, pairs.length - 1 - this.no_txn); i < pairs.length; i++) {
                for(Sock s : lastUnfulfilledRequests.keySet()) {

                    if(s.distance(pairs[i].u) == 0 || s.distance(pairs[i].v) == 0 || s.distance(pairs[i].u) + s.distance(pairs[i].v) < minDist1 && pairs[i].timesOffered <= minTimesOffered) {
                        playerCloseToOffer = lastUnfulfilledRequests.get(s);
                        minId1 = 2*i;
                        minDist1 = s.distance(pairs[i].u) + s.distance(pairs[i].v);
                        minTimesOffered = pairs[i].timesOffered;
                    }
                }
            }


            /*
            double minDist1 = 1000;
            int minTimesOffered1 = 1000;
            int minId1 = -1;

            double minDist2 = 1000;
            int minTimesOffered2 = 1000;
            int minId2 = -1;

            for(int i = pairs.length - 5; i < pairs.length; i++) {
                for(Sock s : lastUnfulfilledRequests) {
                    if(s.distance(pairs[i].u) < minDist1) {
                        minId2 = minId1;
                        minDist2 = minDist1;
                        minTimesOffered2 = minTimesOffered1;
                        minId1 =  2*i;
                        minDist1 = s.distance(pairs[i].u);
                        minTimesOffered1 = pairs[i].timesOffered;
                    } else if(s.distance(pairs[i].u) < minDist2) {
                        minId2 = 2*i;
                        minDist2 = s.distance(pairs[i].u);
                        minTimesOffered2 = pairs[i].timesOffered;
                    }

                    if(s.distance(pairs[i].v) < minDist1) {
                        minId2 = minId1;
                        minDist2 = minDist1;
                        minTimesOffered2 = minTimesOffered1;
                        minId1 = 2*i + 1;
                        minDist1 = s.distance(pairs[i].v);
                        minTimesOffered1 = pairs[i].timesOffered;
                    } else if(s.distance(pairs[i].v) < minDist2) {
                        minId2 =  2*i + 1;
                        minDist2 = s.distance(pairs[i].v);
                        minTimesOffered2 = pairs[i].timesOffered;
                    }
                }
            }
            */
            if(minId1 >= 0) {
                pairs[minId1/2].timesOffered++;
                //pairs[minId2/2].timesOffered++;

                this.id1 = minId1;
                this.id2 = minId1 + 1;
            } else {
                this.id1 = (socks.length - Math.abs(this.no_txn/2)*2 - 1) % socks.length;
                this.id2 = (socks.length - Math.abs(this.no_txn/2)*2 - 2) % socks.length;
            }

            this.pair_offer = 0; //this is stage 0, so pair_offer == 0;

        } else { //this is stage 1
            if(this.txn || this.pair_offer == 0) {
                this.median = getMedianSock();
            }

            //Get the farthest 2 socks
            int maxid1 = socks.length - socks.length/3;
            int maxid2 = socks.length - socks.length/3 + 1;
            this.max_dist1 = median.distance(socks[maxid1]);
            this.max_dist2 = median.distance(socks[maxid2]);

            if(this.max_dist1 < this.max_dist2) {
                double tmp = this.max_dist2;
                this.max_dist2 = this.max_dist1;
                this.max_dist1 = tmp;
                maxid1 = socks.length - socks.length/3 + 1;
                maxid2 = socks.length - socks.length/3;
            }

            for(int i = socks.length - socks.length/3 + 2; i < socks.length; i++) {
                boolean interest = (lastOffers.get(id).getFirst() != socks[i] && lastOffers.get(id).getSecond() != socks[i]) || lastUnfulfilledRequests.containsKey(socks[i]);
                if (median.distance(socks[i]) > this.max_dist1 && interest) {
                    this.max_dist2 = this.max_dist1;
                    this.max_dist1 = median.distance(socks[i]);
                    maxid2 = maxid1;
                    maxid1 = i;
                } else if (median.distance(socks[i]) > this.max_dist2 && interest) {
                    this.max_dist2 = median.distance(socks[i]);
                    maxid2 = i;
                }
            }

            this.id1 = maxid1;
            this.id2 = maxid2;

            this.pair_offer = 1; // this is stage 1
        }

        this.txn = false;
        return new Offer(socks[this.id1], socks[this.id2]);
    }

    @Override
    public Request requestExchange(List<Offer> offers) {
        /*
            offers.get(i)           -       Player i's offer
            For each offer:
            offer.getSock(rank = 1, 2)      -       get rank's offer
            offer.getFirst()                -       equivalent to offer.getSock(1)
            offer.getSecond()               -       equivalent to offer.getSock(2)
            Remark: For Request object, rank ranges between 1 and 2
         */

        lastOffers = offers;

        int c = 0;
        double min_dist1 = this.max_dist1;
        double min_dist2 = this.max_dist2;
        int min1 = -1;
        int min2 = -1;
        int rank1 = -1;
        int rank2 = -1;
        //Sock mean = getMeanSock();
        double curMin1 = this.embarrassment;
        double curMin2 = curMin1;
        for (int i = 0; i < offers.size(); i++) {
            if (i == id) continue;

            // Find averge resulting embarassement
            //double firstEmbarassment = (updatedEmbarassment(offers.get(i).getFirst(), id1) + updatedEmbarassment(offers.get(i).getFirst(), id2))/2;
            //double secondEmbarassment = (updatedEmbarassment(offers.get(i).getSecond(), id1) + updatedEmbarassment(offers.get(i).getSecond(), id2))/2;

            double firstEmbarassment = minDistance(offers.get(i).getFirst());
            double secondEmbarassment = minDistance(offers.get(i).getSecond());

            if(curMin1 > firstEmbarassment) {//&& min1 != playerCloseToOffer) {
                curMin2 = curMin1;
                curMin1 = firstEmbarassment;
                min2 = min1;
                min1 = i;
                rank2 = rank1;
                rank1 = 1;
                c++;
            } else if(curMin2 > firstEmbarassment) {//&& min2 != playerCloseToOffer) {
                curMin2 = firstEmbarassment;
                min2 = i;
                rank2 = 1;
                c++;
            }

            if(curMin1 > secondEmbarassment) {//&& min1 != playerCloseToOffer) {
                curMin2 = curMin1;
                curMin1 = secondEmbarassment;
                min2 = min1;
                min1 = i;
                rank2 = rank1;
                rank1 = 2;
                c++;
            } else if(curMin2 > secondEmbarassment) {// && min2 != playerCloseToOffer) {
                curMin2 = secondEmbarassment;
                min2 = i;
                rank2 = 2;
                c++;
            }

        }

        if(min2 == playerCloseToOffer && min1 != min2) {
            min2 = min1;
            min1 = playerCloseToOffer;
            int tmp = rank1;
            rank1 = rank2;
            rank2 = tmp;
        }

        //form the request
        return new Request(min1, rank1 , min2, rank2);
    }

    private double minDistance(Sock s) {
        double minDistance = 1000;
        if(s == null) return minDistance;

        for(int i = 0; i < socks.length; i += 2) {
            if(i == this.id1 || i == this.id2) continue;
            minDistance = Math.min(minDistance, Math.min(s.distance(socks[i]), s.distance(socks[i+1])));
        }

        return minDistance;
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


        //a txn happens, set no_txn to 0
        this.no_txn = 0;
        this.txn = true;
        int rank;
        Sock newSock;
        Sock oldSock;
        if (transaction.getFirstID() == id) {
            rank = transaction.getFirstRank();
            newSock = transaction.getSecondSock();
            oldSock = transaction.getFirstSock();
        } else {
            oldSock = transaction.getSecondSock();
            rank = transaction.getSecondRank();
            newSock = transaction.getFirstSock();
        }

        if (rank == 1) {
            socks[id1] = newSock;
        } else {
            socks[id2] = newSock;
        }
    }

    @Override
    public List<Sock> getSocks() {
        if(t == 0) {
            pairSocks();
        }

        return Arrays.asList(socks);
    }


    private void pairSocks() {
        if(this.n < 300) {
            this.pairSocksBlossom();
        } else {
            this.pairSocksGreedy();
        }
    }


    public void pairSocksBlossom() {
        int[] match = new Blossom(getCostMatrix(socks), true).maxWeightMatching();
        for (int i = 0, k = 0; i < match.length; i++) {
            if(i > match[i]) continue;
            pairs[k++] = new Pair(socks[i], socks[match[i]]);
        }

        Arrays.sort(pairs);
        for(int j = 0; j < pairs.length; j++) {
            socks[2*j] = pairs[j].u;
            socks[2*j + 1] = pairs[j].v;
        }
    }

    public double getOptimalEmbarassment(Sock[] s) {
        int[] match = new Blossom(getCostMatrix(s), true).maxWeightMatching();
        List<Pair> pairs = new LinkedList<Pair>();
        List<Sock> result = new ArrayList<Sock>();
        for (int i=0; i < match.length; i++) {
            if (match[i] < i) continue;
            pairs.add(new Pair(s[i], s[match[i]]));
        }

        double embarassment = 0;
        for(Pair p : pairs) {
            embarassment += p.weight();
        }

        return embarassment;
    }

    public Sock getMedianSock() {
        List<Integer> sock_R = new ArrayList<Integer>();
        List<Integer> sock_G = new ArrayList<Integer>();
        List<Integer> sock_B = new ArrayList<Integer>();
        for (Sock s : socks){
            sock_R.add(s.R);
            sock_G.add(s.G);
            sock_B.add(s.B);
        }

        Collections.sort(sock_R);
        Collections.sort(sock_G);
        Collections.sort(sock_B);

        int mid_R = sock_R.get(n/2);
        int mid_G = sock_G.get(n/2);
        int mid_B = sock_B.get(n/2);

        return new Sock(mid_R, mid_G, mid_B);
    }

    private float[][] getCostMatrix(Sock[] s) {
        float[][] matrix = new float[s.length*(s.length-1)/2][3];
        int idx = 0;
        for (int i = 0; i < s.length; i++) {
            for (int j=i+1; j< s.length; j++) {
                matrix[idx] = new float[]{i, j, (float)(-s[i].distance(s[j]))};
                idx++;
            }
        }
        return matrix;
    }

    private double updatedEmbarassment(Sock newSock, int replaceIndex) {
        if(newSock == null) return -1;
        int startIndex = Math.min(socks.length - 20, replaceIndex % 2 == 0 ? replaceIndex : replaceIndex - 1);
        startIndex = Math.max(startIndex, 0);
        int offset = replaceIndex % 2 == 0 ? 0 : 1;
        Sock[] bottom = new Sock[socks.length - startIndex];
        for(int i = 0; i < bottom.length; i++) {
            if(startIndex + i == replaceIndex) {
                bottom[i] = newSock;
            } else {
                bottom[i] = socks[startIndex + i];
            }
        }

        return getOptimalEmbarassment(bottom);
    }



    private double getEmbarassment(Sock[] s) {
        double result = 0;
        for (int i = 0; i < s.length; i += 2) {
            result += s[i].distance(s[i + 1]);
        }

        return result;
    }

    private void shuffleSocks() {
        Random r = new Random();
        for(int i = 0; i < socks.length; i++) {
            int j = r.nextInt(socks.length - i) + i;
            Sock tmp = socks[i];
            socks[i] = socks[j];
            socks[j] = tmp;
        }
    }

    private void sortPairs() {
        for (int i = 0; i < socks.length; i += 2) {
            pairs[i/2] = new Pair(socks[i], socks[i+1]);
        }

        Arrays.sort(pairs);
        for(int j = 0; j < pairs.length; j++) {
            socks[2*j] = pairs[j].u;
            socks[2*j + 1] = pairs[j].v;
        }
    }

    public void pairSocksGreedyShuffle() {
        int[] tmp;
        for(int i = 0; i < 3; i++) {
            shuffleSocks();
        }
    }

    public void pairSocksGreedy() {
        boolean[] paired = new boolean[socks.length];
        for(int i = 0; i < socks.length; i++) {
            if(paired[i]) {
                continue;
            }
            int curNeighbor = -1;
            double curMinDist = Double.MAX_VALUE;
            for(int j = i+1; j < socks.length; j++) {
                if(!paired[j] && socks[i].distance(socks[j]) < curMinDist) {
                    curNeighbor = j;
                    curMinDist = socks[i].distance(socks[j]);
                }
            }

            paired[i] = true;
            paired[i+1] = true;

            Sock tmp = socks[i+1];
            socks[i+1] = socks[curNeighbor];
            socks[curNeighbor] = tmp;
            i++;
        }

        sortPairs();
    }
}