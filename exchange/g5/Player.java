package exchange.g5;

import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;
import exchange.sim.Transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Player extends exchange.sim.Player {
    /*
        Inherited from exchange.sim.Player:
        Random random   -       Random number generator, if you need it
        Remark: you have to manually adjust the order of socks, to minimize the total embarrassment
                the score is calculated based on your returned list of getSocks(). Simulator will pair up socks 0-1, 2-3, 4-5, etc.
     */

    private static final int RED = 0;
    private static final int GREEN = 1;
    private static final int BLUE = 2;

    private int id1, id2, id;
    private int p;
    private Sock[] socks;
    private SockPair[] pairs;
    private ArrayList<ArrayList<Sock>> globalInfo;
    private boolean tradeHappened;
    private int noTradeCount;
    private int tradeID;
    private int tradingPairs;

    public double findTotalDistance(SockPair[] pairs) {
        double totalDistance = 0;
        for (int i = 0; i < pairs.length; i++) {
            totalDistance += pairs[i].distance;
        }
        return totalDistance;
    }

    public SockPair[] generatePairs(Sock[] socks) {
//    	ArrayList<SockPair> allPairs = new ArrayList<SockPair>();
//    	for(int i = 0; i < socks.length; i++){
//    		for(int j = i+1; j < socks.length; j++){
//    			allPairs.add(new SockPair(i,j, socks[i], socks[j]));
//    		}
//    	}
//
//    	allPairs.sort((p1,p2)->{
//    		return Double.compare(p1.distance,p2.distance);
//    	});
//
//    	int j = 0;
//    	ArrayList<Integer> pairedSocks = new ArrayList<Integer>();
//    	SockPair[] pairs = new SockPair[socks.length/2];
//
//    	for(int i = 0; i < pairs.length; i++){
//    		pairs[i] = allPairs.get(j);
//
//    		pairedSocks.add(pairs[i].id1);
//    		pairedSocks.add(pairs[i].id2);
//    		while(j != allPairs.size() && (pairedSocks.contains(allPairs.get(j).id1) | pairedSocks.contains(allPairs.get(j).id2)))
//    			j++;
//    	}
        int n = socks.length;
        float[][] allPairs = new float[2 * n * (2 * n - 1) / 2][3];

        int k = 0;
        for (int i = 0; i < socks.length; i++) {
            for (int j = i + 1; j < socks.length; j++) {
                allPairs[k] = new float[]{i, j, -(float) (socks[i].distance(socks[j]))};
                k++;
            }
        }

//    	System.out.println("Before Blossom");
        int[] result = new Blossom(allPairs, true).maxWeightMatching();
//    	System.out.println("After Blossom");
        SockPair[] pairs = new SockPair[socks.length / 2];
        ArrayList<SockPair> pairsList = new ArrayList<SockPair>();

        int j = 0;
        ArrayList<Integer> pairedSocks = new ArrayList<Integer>();

        for (int i = 0; i < pairs.length; i++) {
            pairsList.add(new SockPair(j, result[j], socks[j], socks[result[j]]));

            pairedSocks.add(j);
            pairedSocks.add(result[j]);
            while (j != result.length && (pairedSocks.contains(j) | pairedSocks.contains(result[j])))
                j++;
        }

        pairsList.sort((p1, p2) -> {
            return Double.compare(p1.distance, p2.distance);
        });

        for (int i = 0; i < pairs.length; i++) {
            pairs[i] = pairsList.get(i);
        }

        return pairs;
    }

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);
        this.pairs = new SockPair[n];
        this.pairs = generatePairs(this.socks);
        int k = 0;
        Sock[] copy = Arrays.copyOf(this.socks, this.socks.length);

        for (int i = 0; i < pairs.length; i++) {
            this.socks[k] = copy[pairs[i].id1];
            this.socks[k + 1] = copy[pairs[i].id2];
            pairs[i].id1 = k;
            pairs[i].id2 = k + 1;
            k = k + 2;
        }

        this.p = p;

        this.globalInfo = new ArrayList<ArrayList<Sock>>();
        for (int i = 0; i < p; i++) {
            globalInfo.add(new ArrayList<Sock>());
            if (i == id) {
                for (int j = 0; j < 2 * n; j++) {
                    globalInfo.get(i).add(this.socks[j]);
                }
            }
        }

        double trading = .2 * pairs.length;
        this.tradingPairs = (int) trading;
        this.noTradeCount = 0;
        this.tradeHappened = false;
        this.tradeID = pairs.length - 1;

    }

    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        /*
            lastRequests.get(i)		-		Player i's request last round
			lastTransactions		-		All completed transactions last round.
		 */
        if (tradeHappened) {
            noTradeCount = 0;
            tradeHappened = false;
        } else
            noTradeCount++;

        updateGlobalInfo(lastTransactions);
        if (noTradeCount >= 5) {
            int offset = random.nextInt(tradingPairs);
            tradeID = (pairs.length - offset) - 1;
            noTradeCount = 0;
        }

//    	id1 = pairs[tradeID].id1;
//    	id2 = pairs[tradeID].id2;

        int[][] maxMinSocks = getMaxMinSocks();
        int required = maxMinSocks[id][0];
        ArrayList<Integer> attractivePlayers = new ArrayList<Integer>();

        for (int i = 0; i < maxMinSocks.length; i++) {
            if (i == id) continue;

            if (maxMinSocks[i][1] == required) {
                attractivePlayers.add(i);
            }
        }

        int index, index2;
        if (attractivePlayers.size() == 0) {
            id1 = random.nextInt(socks.length);
            do {
                id2 = random.nextInt(socks.length);
            } while (id2 == id1);
            return new Offer(socks[id1], socks[id2]);
        }

        index = random.nextInt(attractivePlayers.size());
        int offerColor1 = maxMinSocks[attractivePlayers.get(index)][0];
        do {
            index2 = random.nextInt(attractivePlayers.size());
            if (attractivePlayers.size() == 1)
                break;
        } while (index2 == index);
        int offerColor2 = maxMinSocks[attractivePlayers.get(index2)][0];

        id1 = getRandomColoredSock(offerColor1);
        id2 = getRandomColoredSock(offerColor2);

//    	return new Offer(sock1,sock2);
        return new Offer(socks[id1], socks[id2]);
    }

    public int getRandomColoredSock(int color) {
        ArrayList<Integer> shortlisted = new ArrayList<Integer>();

        for (int i = 0; i < socks.length; i++) {
            Sock temp = socks[i];
            if (temp.R >= Math.max(temp.G, temp.B) && color == RED) {
                shortlisted.add(i);
            } else if (temp.G >= Math.max(temp.R, temp.B) && color == GREEN) {
                shortlisted.add(i);
            } else if (temp.B >= Math.max(temp.R, temp.G) && color == BLUE) {
                shortlisted.add(i);
            }
        }

        int index = random.nextInt(shortlisted.size());
        return shortlisted.get(index);
    }

    public int[][] getMaxMinSocks() {
        int[][] maxMinSocks = new int[p][2];

        for (int i = 0; i < p; i++) {
            int countR = 0;
            int countG = 0;
            int countB = 0;

            for (Sock s : globalInfo.get(i)) {
                if (s.R >= Math.max(s.G, s.B)) {
                    countR += 1;
                } else if (s.G >= Math.max(s.R, s.B)) {
                    countG += 1;
                } else {
                    countB += 1;
                }
            }

            if (countR == Math.max(countR, Math.max(countG, countB))) {
                maxMinSocks[i][0] = RED;
            } else if (countG == Math.max(countR, Math.max(countG, countB))) {
                maxMinSocks[i][0] = GREEN;
            } else {
                maxMinSocks[i][0] = BLUE;
            }

            if (countR == Math.min(countR, Math.min(countG, countB))) {
                maxMinSocks[i][1] = RED;
            } else if (countG == Math.min(countR, Math.min(countG, countB))) {
                maxMinSocks[i][1] = GREEN;
            } else {
                maxMinSocks[i][1] = BLUE;
            }
        }


        return maxMinSocks;
    }

    public void updateGlobalInfo(List<Transaction> lastTransactions) {
        if (lastTransactions != null) {
            for (Transaction t : lastTransactions) {
                int firstID = t.getFirstID();
                int secondID = t.getSecondID();

                Sock firstSock = t.getFirstSock();
                Sock secondSock = t.getSecondSock();

                int firstOfferedIndex = findIndexOfSock(globalInfo.get(firstID), firstSock);
                int secondOfferedIndex = findIndexOfSock(globalInfo.get(secondID), secondSock);

                if (firstOfferedIndex != -1)
                    globalInfo.get(firstID).set(firstOfferedIndex, secondSock);
                else
                    globalInfo.get(firstID).add(secondSock);

                if (secondOfferedIndex != -1)
                    globalInfo.get(secondID).set(secondOfferedIndex, firstSock);
                else
                    globalInfo.get(secondID).add(firstSock);
            }
        }
//    	for(int i = 0; i < offers.size(); i++){
//    		if(i == id) continue;
//    		Offer offer = offers.get(i);
//
//    		if(offer.getFirst() != null && !globalInfo.get(i).contains(offer.getFirst()))
//    			globalInfo.get(i).add(offer.getFirst());
//    		if(offer.getSecond() != null && !globalInfo.get(i).contains(offer.getSecond()))
//    			globalInfo.get(i).add(offer.getSecond());
//    	}
    }

    public int findIndexOfSock(ArrayList<Sock> socks, Sock sock) {
        for (int i = 0; i < socks.size(); i++) {
            if (socks.get(i).equals(sock))
                return i;
        }
        return -1;
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
//    	updateGlobalInfo(offers);

        int firstID = -1;
        int firstRank = -1;
        double minTotalDistance1 = findTotalDistance(pairs);
        int secondID = -1;
        int secondRank = -1;
        double minTotalDistance2 = findTotalDistance(pairs);
//		System.out.println("min1: "+minTotalDistance1);
//		System.out.println("min2: "+minTotalDistance2);


        for (int i = 0; i < offers.size(); i++) {
            if (i == id)
                continue;

            Offer offer = offers.get(i);

            Sock first = offer.getFirst();
            Sock second = offer.getSecond();

            double avgFirstDistance = findAverageDistanceForNewSock(first);
//			System.out.println("avg1: "+avgFirstDistance);
            double avgSecondDistance = findAverageDistanceForNewSock(second);
//    		System.out.println("avg2: "+avgSecondDistance);

            if (avgFirstDistance <= minTotalDistance1) {
                secondID = firstID;
                secondRank = firstRank;
                minTotalDistance2 = minTotalDistance1;

                firstID = i;
                firstRank = 1;
                minTotalDistance1 = avgFirstDistance;
            } else if (minTotalDistance1 < avgFirstDistance && avgFirstDistance <= minTotalDistance2) {
                secondID = i;
                secondRank = 1;
                minTotalDistance2 = avgFirstDistance;
            }

            if (avgSecondDistance <= minTotalDistance1) {
                secondID = firstID;
                secondRank = firstRank;
                minTotalDistance2 = minTotalDistance1;

                firstID = i;
                firstRank = 2;
                minTotalDistance1 = avgSecondDistance;
            } else if (minTotalDistance1 < avgSecondDistance && avgSecondDistance <= minTotalDistance2) {
                secondID = i;
                secondRank = 2;
                minTotalDistance2 = avgSecondDistance;
            }
        }
        return new Request(firstID, firstRank, secondID, secondRank);
    }

    public double findAverageDistanceForNewSock(Sock sock) {
        Sock[] sockAtId1 = Arrays.copyOf(socks, socks.length);

        //System.out.println("before change "+sockAtId1[id1]);
        sockAtId1[id1] = sock;
        //System.out.println(sockAtId1[id1]);

        Sock[] sockAtId2 = Arrays.copyOf(socks, socks.length);
        sockAtId2[id2] = sock;
        //System.out.println(sock);
        //System.out.println(sockAtId2[id2]);

        SockPair[] sockAtId1Pairs = generatePairs(sockAtId1);
        SockPair[] sockAtId2Pairs = generatePairs(sockAtId2);

        double sockAtId1Distance = findTotalDistance(sockAtId1Pairs);
        double sockAtId2Distance = findTotalDistance(sockAtId2Pairs);
        double avgSockDistance = (sockAtId1Distance + sockAtId2Distance) / 2;

        return avgSockDistance;
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
//            System.out.println("Inside if");
            rank = transaction.getFirstRank();
            newSock = transaction.getSecondSock();
        } else {
//            System.out.println("Inside else");
            rank = transaction.getSecondRank();
            newSock = transaction.getFirstSock();
        }
        if (rank == 1) {
//            System.out.println("Inside rank if");
            socks[id1] = newSock;
        } else {
//            System.out.println("Inside rank else");
            socks[id2] = newSock;
        }

        tradeHappened = true;
        pairs = generatePairs(socks);
        int j = 0;
        Sock[] copy = Arrays.copyOf(socks, socks.length);

        for (int i = 0; i < pairs.length; i++) {
            socks[j] = copy[pairs[i].id1];
            socks[j + 1] = copy[pairs[i].id2];
            pairs[i].id1 = j;
            pairs[i].id2 = j + 1;
            j = j + 2;
        }
    }

    @Override
    public List<Sock> getSocks() {
        return Arrays.asList(socks);
    }

    private class SockPair {
        public int id1;
        public int id2;
        public Sock testSock1;
        public Sock testSock2;
        public double distance;

        public SockPair(int id1, int id2, Sock sock1, Sock sock2) {
            this.id1 = id1;
            this.id2 = id2;
            this.testSock1 = sock1;
            this.testSock2 = sock2;
            calcDistance();
        }

        private void calcDistance() {
            distance = testSock1.distance(testSock2);
        }


        public void printSockPair() {
            //*System.out.println("(" + this.id1 + "," + this.id2 + "," + distance + ")");*/
        }
    }
}