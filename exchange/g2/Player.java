package exchange.g2;

import java.util.*;

import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;
import exchange.sim.Transaction;

public class Player extends exchange.sim.Player {

    private final int K = 2;

    /*
        Inherited from exchange.sim.Player:
        Random random   -       Random number generator, if you need it

        Remark: you have to manually adjust the order of socks, to minimize the total embarrassment
                the score is calculated based on your returned list of getSocks(). Simulator will pair up socks 0-1, 2-3, 4-5, etc.
     */
    private int id1, id2, id;
    private double id1DistanceToCentroid, id2DistanceToCentroid;

    private Sock[] socks;

    private int currentTurn;
    private int totalTurns;
    private int numPlayers;
    private int numSocks;
    private boolean shouldRecomputePairing;

    private PriorityQueue<SockPair> rankedPairs;
    private List<Offer> lastOffers = null;
    private Map<Sock,Map<Sock,Double>> singleExchangeEmbarrasments;

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.totalTurns = t;
        this.currentTurn = t;
        this.numPlayers = p;
        this.numSocks = n*2;
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);

        this.rankedPairs = new PriorityQueue<SockPair>();

        System.out.println("Initial embarrassment for player "+ id+ ": "+getEmbarrasment());
        pairAlgo();
        this.shouldRecomputePairing = false;
        this.singleExchangeEmbarrasments = new HashMap<>();
    }

    private double getEmbarrasment() {
        return getEmbarrasment(this.socks);
    }

    private double getEmbarrasment(Sock[] socks) {
        double result = 0;
        for (int i = 0; i < socks.length; i += 2){
            result += socks[i].distance(socks[i+1]);
        }
        return result;
    }

    public void pairBlossom() {
        this.socks = pairBlossom(this.socks, true);
    }

    public Sock[] pairBlossom(Sock[] socks) {
        return pairBlossom(socks, false);
    }

    public Sock[] pairAlgo(Sock[] socks) {
        Sock[] result;
        if(this.numSocks < 300){
            result = pairBlossom(socks);
        } else {
            result  = pairGreedily(socks);
        }
        return result;
    }

    public void pairAlgo() {
        if(this.numSocks < 300){
            this.socks = pairBlossom(this.socks, true);
        } else {
            this.socks = pairGreedily(this.socks, true);
        }
    }

    public void pairGreedily() {
        this.socks = pairGreedily(this.socks, true);
    }

    public Sock[] pairGreedily(Sock[] socks) {
        return pairGreedily(socks, false);
    }

    private Sock[] pairGreedily(Sock[] socks, boolean updateRankedPairs) {
        PriorityQueue<SockPair> queue = new PriorityQueue<SockPair>();
        for (int i = 0; i < socks.length ; i++){
            for (int j = 0; j < i; j++){
                queue.add(new SockPair(socks[i],socks[j]));
            }
        }

        HashSet<Sock> matched = new HashSet<Sock>();
        while(matched.size() < socks.length ){
            SockPair pair = queue.poll();
            if(pair != null) {
                if(!matched.contains(pair.s1) && !matched.contains(pair.s2)){
                    matched.add(pair.s1);
                    socks[matched.size()-1] = pair.s1;
                    matched.add(pair.s2);
                    socks[matched.size()-1] = pair.s2;
                    if (updateRankedPairs) {
                        this.rankedPairs.add(pair);
                    }
                }
            }
        }

        return socks;
    }

    public Sock[] pairBlossom(Sock[] socks, boolean updateRankedPairs) {
        int[] match = new Blossom(getCostMatrix(socks), true).maxWeightMatching();
        List<Sock> result = new ArrayList<Sock>();
        for (int i=0; i<match.length; i++) {
            if (match[i] < i) continue;
            result.add(socks[i]);
            result.add(socks[match[i]]);
            if (updateRankedPairs) {
                this.rankedPairs.add(new SockPair(socks[i], socks[match[i]]));
            }
        }
        return (Sock[]) result.toArray(new Sock[socks.length]);
    }

    private float[][] getCostMatrix(Sock[] socks) {
        int numSocks = socks.length;
        float[][] matrix = new float[numSocks*(numSocks-1)/2][3];
        int idx = 0;
        for (int i = 0; i < socks.length; i++) {
            for (int j=i+1; j< socks.length; j++) {
                matrix[idx] = new float[]{i, j, (float)(-socks[i].distance(socks[j]))};
                idx ++;
            }
        }
        return matrix;
    }

    private double getMaxReductionInPairDistance(Sock s) {
        double maxDistanceReduction = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < this.socks.length; i+=2) {
            if (i == id1 || i == id2) continue; // Skip offered pair.
            double pairDistance = this.socks[i].distance(this.socks[i+1]);
            double distanceToFirst = this.socks[i].distance(s);
            double distanceToSecond = this.socks[i+1].distance(s);
            double distanceReduction = pairDistance - Math.min(distanceToFirst, distanceToSecond);
            if (distanceReduction > maxDistanceReduction) {
                maxDistanceReduction = distanceReduction;
            }
        }
        return maxDistanceReduction;
    }

    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        /*
			lastRequests.get(i)		-		Player i's request last round
			lastTransactions		-		All completed transactions last round.
		 */

        currentTurn--;
        if (this.shouldRecomputePairing) {
            rankedPairs.clear();
            pairAlgo();
            this.shouldRecomputePairing = false;
            singleExchangeEmbarrasments.clear();
        }

        // Getting the worst paired socks.

        List<SockPair> poppedPair = new ArrayList<>();
        SockPair maxPair = rankedPairs.poll();
        poppedPair.add(maxPair);

        int bound = 1;
        if(numPlayers == 10) {
            bound = 3;
        } else if(numPlayers == 100) {
            bound = 10;
        } else if(numPlayers == 1000){
            bound = 50;
        }
        for(int i=0; i<bound; i++) {
            SockPair next = rankedPairs.poll();
            poppedPair.add(next);
            if (next.timesOffered <= maxPair.timesOffered-3 ||
                    (next.distance > maxPair.distance && next.timesOffered <= maxPair.timesOffered)) {
                maxPair = next;
            }
        }
        for(SockPair pair : poppedPair) {
            rankedPairs.add(pair);
        }


        id1 = Arrays.asList(socks).indexOf(maxPair.s1);
        id2 = id1+1;
        maxPair.timesOffered++;

        return new Offer(maxPair.s1,maxPair.s2);
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
        //this is looking through each offer and changing the market value
        try {
            lastOffers = offers;

            double maxDistanceReduction = getMaxReductionInPairDistance(this.socks[id1]);
            int firstId = -1;
            int firstRank = -1;

            double secondMaxDistanceReduction = getMaxReductionInPairDistance(this.socks[id2]);
            int secondId = -1;
            int secondRank = -1;

            Sock[] socksNoId1 = this.socks.clone();
            Sock[] socksNoId2 = this.socks.clone();
            Sock[] socksNoId1NorId2 = this.socks.clone();

            double currentEmbarrasment = getEmbarrasment();
            double avgEmbarrasment;
            double embarrasmentExchangingId1ForS1;
            double embarrasmentExchangingId2ForS1;
            double embarrasmentExchangingId1ForS2;
            double embarrasmentExchangingId2ForS2;
            double embarrasmentExchangingId1AndId2 = 0;
            double minPairEmbarrasment = currentEmbarrasment;

            double minSingleEmbarrasment = currentEmbarrasment;
            int singleId = -1;
            int singleRank = -1;

            Sock sock1 = socks[id1];
            Sock sock2 = socks[id2];

            boolean keepLooking = true;
            for (int i = 0; i < offers.size() && keepLooking; ++i) {
                if (i == id) continue; // Skip our own offer.
                for (int j = 1; j < 3 && keepLooking; ++j) {
                    Sock s1 = offers.get(i).getSock(j);
                    if (s1 == null) continue;

                    socksNoId1[id1] = s1;
                    if (!singleExchangeEmbarrasments.containsKey(s1)) singleExchangeEmbarrasments.put(s1,new HashMap<>());
                    if (!singleExchangeEmbarrasments.get(s1).containsKey(sock1)) {
                        singleExchangeEmbarrasments.get(s1).put(sock1,getEmbarrasment(pairAlgo(socksNoId1)));
                    }
                    embarrasmentExchangingId1ForS1 = singleExchangeEmbarrasments.get(s1).get(sock1);
                    if (embarrasmentExchangingId1ForS1 > currentEmbarrasment) continue;

                    socksNoId2[id2] = s1;
                    if (!singleExchangeEmbarrasments.get(s1).containsKey(sock2)) {
                        singleExchangeEmbarrasments.get(s1).put(sock2,getEmbarrasment(pairAlgo(socksNoId2)));
                    }
                    embarrasmentExchangingId2ForS1 = singleExchangeEmbarrasments.get(s1).get(sock2);
                    if (embarrasmentExchangingId2ForS1 > currentEmbarrasment) continue;

                    avgEmbarrasment = (embarrasmentExchangingId1ForS1 + embarrasmentExchangingId2ForS1)/2;
                    if (avgEmbarrasment < minSingleEmbarrasment) {
                        minSingleEmbarrasment = avgEmbarrasment;
                        singleId = i;
                        singleRank = j;
                        keepLooking = true;
                    }

                    socksNoId1NorId2[id1] = s1;
                    for (int k = i; k < offers.size() && keepLooking; ++k) {
                        if (keepLooking == false) break;
                        if (k == id) continue; // Skip our own offer.
                        for (int l = j+1; l < 3 && keepLooking; ++l) {
                            Sock s2 = offers.get(k).getSock(l);
                            if (s2 == null) continue;

                            socksNoId1[id1] = s2;
                            if (!singleExchangeEmbarrasments.containsKey(s2)) singleExchangeEmbarrasments.put(s2,new HashMap<>());
                            if (!singleExchangeEmbarrasments.get(s2).containsKey(sock1)) {
                                singleExchangeEmbarrasments.get(s2).put(sock1,getEmbarrasment(pairAlgo(socksNoId1)));
                            }
                            embarrasmentExchangingId1ForS2 = singleExchangeEmbarrasments.get(s2).get(sock1);
                            if (embarrasmentExchangingId1ForS2 > currentEmbarrasment) continue;

                            socksNoId2[id2] = s2;
                            if (!singleExchangeEmbarrasments.get(s2).containsKey(sock2)) {
                                singleExchangeEmbarrasments.get(s2).put(sock2,getEmbarrasment(pairAlgo(socksNoId2)));
                            }
                            embarrasmentExchangingId2ForS2 = singleExchangeEmbarrasments.get(s2).get(sock2);
                            if (embarrasmentExchangingId2ForS2 > currentEmbarrasment) continue;

                            socksNoId1NorId2[id2] = s2;
                            if(totalTurns < 100)
                                embarrasmentExchangingId1AndId2 = getEmbarrasment(pairAlgo(socksNoId1NorId2));

                            if (embarrasmentExchangingId1AndId2 > currentEmbarrasment) continue;
                            avgEmbarrasment = (embarrasmentExchangingId1ForS1 + embarrasmentExchangingId1ForS2 +
                                    embarrasmentExchangingId2ForS1 + embarrasmentExchangingId2ForS1 +
                                    embarrasmentExchangingId1AndId2) / 5;
                            if (avgEmbarrasment < minPairEmbarrasment) {
                                minPairEmbarrasment = avgEmbarrasment;
                                firstId = i;
                                firstRank = j;
                                secondId = k;
                                secondRank = l;
                                if(totalTurns > 100 && avgEmbarrasment < currentEmbarrasment) keepLooking = false; // Use this assignment to improve efficiency.
                            }
                        }
                    }
                }
            }

            if (minSingleEmbarrasment < minPairEmbarrasment) {
                return new Request(singleId, singleRank, -1, -1);
            } else {
                return new Request(firstId, firstRank, secondId, secondRank);
            }
//            return new Request(singleId, singleRank, -1, -1);
        } catch (Exception e) {
            e.printStackTrace();
            return new Request(-1, -1, -1, -1);
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
        this.shouldRecomputePairing = true;

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
        pairAlgo();
        return Arrays.asList(socks);
    }
}
