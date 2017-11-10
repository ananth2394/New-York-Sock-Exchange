package exchange.g1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Collections;
import java.util.Comparator;

import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;
import exchange.sim.Transaction;

import exchange.g1.Blossom;

public class Player extends exchange.sim.Player {
    /*
        Inherited from exchange.sim.Player:
        Random random   -       Random number generator, if you need it

        Remark: you have to manually adjust the order of socks, to minimize the total embarrassment
                the score is calculated based on your returned list of getSocks(). Simulator will pair up socks 0-1, 2-3, 4-5, etc.
     */
    public final boolean USE_ABS_THRESHOLD = false;
    public final double ABS_THRESHOLD_FRAC = 0.8;
    
    private int myFirstOffer, mySecondOffer, id, n, t, turns;
    private int myFirstRequestID, myFirstRequestRank, mySecondRequestID, mySecondRequestRank;
    private List<Request> lastRequests;
    private List<Offer> lastoffers;
    private Offer lastOffer;
    private Pair pairToOffer;
    private boolean marketHasInterest;
    private HashMap<Pair,Double> E1;
    private HashMap<Pair,Double> E2;
    private Sock lastRequestSock1, lastRequestSock2;

    public class Pair {
        public Sock first;
        public Sock second;
        
        public Pair(Sock fst, Sock snd) {
            this.first = fst;
            this.second = snd;
        }

        public int hashCode() {
            return first.hashCode() * second.hashCode();
        }
    }

    public double threshold;
    
    public ArrayList<Sock> socks;
    public ArrayList<Pair> settledPairs;
    public ArrayList<Pair> pendingPairs;

    public int offerIndex;
    public boolean tradeCompleted;
    public int timesPairOffered;

    // Request history of the rest of the players
    public HashMap<Integer, ArrayList<Sock>> playersRequestHistory;
    
    public void repair() {
        this.settledPairs.clear();
        pair(this.socks);
        for (int i = 0; i < this.socks.size(); i += 2) {
            this.settledPairs.add(new Pair(this.socks.get(i), this.socks.get(i + 1)));
        }
        this.pendingPairs.clear();
    }
    
    public void pair(List<Sock> sockList) {
        if (sockList.size() <= 200) {
            blossomPair(sockList);
        } else {
            greedyPair(sockList);
        }
    }
    
    public void blossomPair(List<Sock> sockList) {
        Sock[] socks = sockList.toArray(new Sock[2 * this.n]);
        int[] match = new Blossom(getCostMatrix(socks), true).maxWeightMatching();
        sockList.clear();
        for (int i = 0; i < match.length; i++) {
            if (match[i] < i) continue;
            sockList.add(socks[i]);
            sockList.add(socks[match[i]]);
        }
    }
    
    public void greedyPair(List<Sock> sockList) {
        List<Sock> pairedList = new ArrayList<Sock>();
        while (sockList.size() > 0) {
            Sock toPair = sockList.remove(sockList.size() - 1);
            double minDistance = toPair.distance(sockList.get(0));
            int minIndex = 0;
            for (int i = 0; i < sockList.size(); i++) {
                Sock s = sockList.get(i);
                if (toPair.distance(s) < minDistance) {
                    minDistance = toPair.distance(s);
                    minIndex = i;
                }
            }
            pairedList.add(sockList.remove(minIndex));
            pairedList.add(toPair);
        }
        sockList.addAll(pairedList);
    }
    
    private void adjustThreshold() {
        this.chooseNewThreshold();
        for (Pair p : this.settledPairs) {
            if (p.first.distance(p.second) >= threshold) {
                this.pendingPairs.add(p);
            } else if (!USE_ABS_THRESHOLD) {
                break;
            }
        }
        this.settledPairs.removeAll(this.pendingPairs);
    }
    
    private void chooseNewThreshold() {
        if (USE_ABS_THRESHOLD) {
            this.threshold = this.threshold * ABS_THRESHOLD_FRAC;
        } else {
            Comparator<Pair> comp = (Pair a, Pair b) -> {
                return (new Double(b.first.distance(b.second))).compareTo(a.first.distance(a.second));
            };
            Collections.sort(this.settledPairs, comp);
            Pair partitionPair = this.settledPairs.get(n / 5);
            this.threshold = partitionPair.first.distance(partitionPair.second);
        }
    }
    
    public Sock[] getSockArray() {
        ArrayList<Sock> ret = new ArrayList<Sock>(2 * this.n);
        for (Pair p : settledPairs) {
            ret.add(p.first);
            ret.add(p.second);
        }
        for (Pair p : pendingPairs) {
            ret.add(p.first);
            ret.add(p.second);
        }
        Sock[] sockArray = new Sock[ret.size()];
        return ret.toArray(sockArray);
    }

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.n = n;
        this.t = t;
        this.turns = 0;
        this.socks = new ArrayList<Sock>(socks);
        this.settledPairs = new ArrayList<>();
        this.pendingPairs = new ArrayList<>();
        for (int i = 0; i < socks.size() - 1; i += 2) {
            this.settledPairs.add(new Pair(socks.get(i), socks.get(i + 1)));
        }
        this.repair();
        if (USE_ABS_THRESHOLD) {
            this.threshold = 60.0;
        }
        this.adjustThreshold();
        this.myFirstOffer = 0;
        this.mySecondOffer = 0;

        this.offerIndex = 0;
        this.tradeCompleted = false;
        this.timesPairOffered = 0;
        
        this.playersRequestHistory = new HashMap<Integer, ArrayList<Sock>>();
        for(int i=0; i < p; i++)    {
            if(i == id) continue;
            this.playersRequestHistory.put(i, new ArrayList<Sock>());
        }

        this.E1 = new HashMap<Pair, Double>();
        this.E2 = new HashMap<Pair, Double>();
    }

    private List<Sock> getTradedSocks(List<Transaction> lastTransactions) {
        List<Sock> res = new ArrayList<Sock>();
        for (Transaction transaction: lastTransactions) {
            if (transaction.getFirstID() == id) {
                // System.out.println(transaction.getFirstSock() + " gets traded!");
                res.add(transaction.getFirstSock());
            } else if (transaction.getSecondID() == id) {
                // System.out.println(transaction.getSecondSock() + " gets traded!");
                res.add(transaction.getSecondSock());
            }
        }
        return res;
    }

    private void printRequestHistory(){
        System.out.println("Printing Request History for Player " + this.id);
        for (HashMap.Entry<Integer, ArrayList<Sock>> entry : playersRequestHistory.entrySet()) {
            System.out.println("ID = " + entry.getKey());
            for (Sock s: entry.getValue()) {
                System.out.println("   " + s);
            }
        }
    }

    private void printEmbarrassmentAfterSwitch(HashMap<Integer, HashMap<Integer, Double>> E2) {

        System.out.println("Printing Embarrassment after switching socks History for Player " + this.id);
        for (HashMap.Entry<Integer, HashMap<Integer, Double>> player : E2.entrySet()) {
            System.out.println("ID: " + player.getKey());
            for (HashMap.Entry<Integer, Double> sockSwitch: player.getValue().entrySet()) {
                System.out.println("Sock: " + sockSwitch.getKey() + ", embarrassment: " + sockSwitch.getValue());
            }
        }

    }
    
    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        HashSet<Sock> interestingSocks = new HashSet<Sock>();
        E2 = new HashMap<Pair, Double>();
        List<Sock> tradedSocks = getTradedSocks(lastTransactions);
        this.lastRequests = lastRequests;

        if (turns % 2 == 0 && turns > 0 ) { // even round
            
            for (int j = 0; j < lastRequests.size(); j++) {
                if (j == this.id) continue;
                // Player j is not interested in us
                if (lastRequests.get(j).getFirstID() != this.id && lastRequests.get(j).getSecondID() != this.id)  {

                    ArrayList<Sock> playerRequest = playersRequestHistory.get(j);
                    Sock firstSock = lastoffers.get(j).getFirst();
                    Sock secondSock = lastoffers.get(j).getSecond();

                    // Sock is not null and has not been traded away
                    if (firstSock != null && (!tradedSocks.contains(firstSock)) && playerRequest.size() > 0) {
                        Sock Q = playerRequest.get(playerRequest.size()-1);              
                        double embarrassment = getTotalEmbarrassment(switchSockAndRepair(firstSock, Q));                    
                        E2.put(new Pair(Q, firstSock), embarrassment);
                    }

                    // Sock is not null and has not been traded away
                    if (secondSock != null && (!tradedSocks.contains(secondSock))  && playerRequest.size() > 0) {                        
                        Sock Q = playerRequest.get(playerRequest.size()-1); 
                        double embarrassment = getTotalEmbarrassment(switchSockAndRepair(secondSock, Q));                    
                        E2.put(new Pair(Q, secondSock), embarrassment);
                    }
                    
                }
            }
        }
        // printEmbarrassmentAfterSwitch(E2);
        
        // Get player history 
        if (turns > 0) {
            
            for (int j = 0; j < lastRequests.size(); j++) {
                if(j == this.id) continue;

                // If a player is interested in us and we did not trade that sock
                if (lastRequests.get(j).getFirstID() == this.id && (!tradedSocks.contains(lastOffer.getSock(lastRequests.get(j).getFirstRank())))) {
                    ArrayList<Sock> playerRequest = playersRequestHistory.get(j);
                    playerRequest.add(lastOffer.getSock(lastRequests.get(j).getFirstRank()));
                    playersRequestHistory.put(j, playerRequest);
                } 
                if(lastRequests.get(j).getSecondID() == this.id && (!tradedSocks.contains(lastOffer.getSock(lastRequests.get(j).getSecondRank())))) {
                    ArrayList<Sock> playerRequest = playersRequestHistory.get(j);              
                    playerRequest.add(lastOffer.getSock(lastRequests.get(j).getSecondRank()));
                    playersRequestHistory.put(j, playerRequest);
                }
            }
        }

        if(pendingPairs.size() == 0) {
            adjustThreshold();
            offerIndex = 0;
        }

        if(tradeCompleted == false) {
            if (timesPairOffered >= 4)   {
                offerIndex = (offerIndex + 2) % pendingPairs.size();
                timesPairOffered = 0;
            }            
        }   
        else {
            timesPairOffered = 0;
            tradeCompleted = false;
        }
        
        pairToOffer = getPairToOffer(timesPairOffered, offerIndex);
        timesPairOffered++;
        return new Offer(pairToOffer.first, pairToOffer.second);
    }

    private Pair getPairToOffer(int timesPairOffered, int currentIndex) {
    	// We look at the currentIndex pair (Sock A <-> B) and currentIndex + 1 pair (Sock C <-> D)
    	int nextIndex = (currentIndex + 1) % pendingPairs.size();
    	if (timesPairOffered == 0) {
    		return new Pair(pendingPairs.get(currentIndex).first, pendingPairs.get(nextIndex).second);
    	} else if (timesPairOffered == 1) {
    		// even round
            return getPairToOfferEvenRound();
    	} else if (timesPairOffered == 2) {
    		return new Pair(pendingPairs.get(currentIndex).second, pendingPairs.get(nextIndex).first);
    	} else if (timesPairOffered == 3) {
    		// even round
            return getPairToOfferEvenRound();
    	} else {
    		System.out.println("Error! timesPairOffered " + timesPairOffered + " is not valid!");
    		return new Pair(pendingPairs.get(currentIndex).first, pendingPairs.get(nextIndex).first);
    	}
    }

    private Pair getPairToOfferEvenRound() {
        // In even round, we use E1 and E2 to find the best embarassment
        // and offer the sock that may solicit those transations.
        Sock first = this.pendingPairs.get(0).first;
        Sock second = this.pendingPairs.get(0).second;

        double minValSoFar = getTotalEmbarrassment(this.socks);
        double secondMinValSoFar = getTotalEmbarrassment(this.socks);
        for (Map.Entry<Pair, Double> entry : E1.entrySet()) {
            if (entry.getValue() < minValSoFar) {
                if (!entry.getKey().first.equals(first)) {
                    second = first;
                    secondMinValSoFar = minValSoFar;
                    first = entry.getKey().first;
                }
                minValSoFar = entry.getValue();
            }
            else if (entry.getValue() < secondMinValSoFar) {
                if (!entry.getKey().first.equals(first)) {
                    second = entry.getKey().first;
                    secondMinValSoFar = entry.getValue();    
                }
            }

        }

        for (Map.Entry<Pair, Double> entry : E2.entrySet()) {
            if (entry.getValue() < minValSoFar) {
                if (!entry.getKey().first.equals(first)) {
                    second = first;
                    secondMinValSoFar = minValSoFar;
                    first = entry.getKey().first;
                }
                minValSoFar = entry.getValue();
            }
            else if (entry.getValue() < secondMinValSoFar) {
                if (!entry.getKey().first.equals(first)) {
                    second = entry.getKey().first;
                    secondMinValSoFar = entry.getValue();    
                }
            }
        }

        return new Pair(first, second);
    }

    private Sock getMeanSock(Sock a, Sock b) {
        return new Sock((a.R + b.R)/2, (a.G + b.G)/2, (a.B + b.B)/2);
    }

    private double getMinDistance(Sock s) {
        double minDistance = 1000;
        for (Pair p: pendingPairs) {
            minDistance = Math.min(minDistance, Math.min(s.distance(p.first), s.distance(p.second)));
        }
        for (Pair p: settledPairs) {
            minDistance = Math.min(minDistance, Math.min(s.distance(p.first), s.distance(p.second)));
        }
        return minDistance;
    }

    public double scoreForTrade(Sock ours, Sock theirs) {
        if (n <= 10) {
            return getTotalEmbarrassment(switchSockAndRepair(theirs, ours));
        } else {
            return getMinDistance(theirs);
        }
    }

    @Override
    public Request requestExchange(List<Offer> offers) {
        /*
            offers.get(i)                   -       Player i's offer
            For each offer:
            offer.getSock(rank = 1, 2)      -       get rank's offer
            offer.getFirst()                -       equivalent to offer.getSock(1)
            offer.getSecond()               -       equivalent to offer.getSock(2)

            Remark: For Request object, rank ranges between 1 and 2
         */
        double minValSoFar = getTotalEmbarrassment(this.socks);
        double secondMinValSoFar = getTotalEmbarrassment(this.socks);
        myFirstRequestID = -1;
        myFirstRequestRank = -1;
        mySecondRequestID = -1;
        mySecondRequestRank = -1;
        this.t--;
        turns++;
        lastOffer = offers.get(this.id);
        lastoffers = offers;
        if (timesPairOffered % 2 == 1) { // First time offering these socks
            for (int i = 0; i < offers.size(); ++ i) {
                if (i == id) continue;

                for (int rank = 1; rank <= 2; ++ rank) {
                    Sock s = offers.get(i).getSock(rank);
                    if (s != null) {
                        Sock[] possibleTrades = {pairToOffer.first, pairToOffer.second};
                        for (Sock myOffer : possibleTrades) {
                            Pair keyPair = new Pair(myOffer, s);
                            double score;
                            if (E1.containsKey(keyPair)) {
                                score = E1.get(keyPair);
                            } else {
                                score = scoreForTrade(myOffer, s);
                            }
                            if (score < minValSoFar) {
                                mySecondRequestID = myFirstRequestID;
                                mySecondRequestRank = myFirstRequestRank;
                                myFirstRequestID = i;
                                myFirstRequestRank = rank;
                                minValSoFar = score;
                            } else if (score < secondMinValSoFar) {
                                secondMinValSoFar = score;
                                mySecondRequestID = i;
                                mySecondRequestRank = rank;
                            }
                            E1.put(keyPair, score);
                        }
                    }
                }
            }
            if (myFirstRequestID != -1){
                lastRequestSock1 = offers.get(myFirstRequestID).getSock(myFirstRequestRank); // can be null    
            }
            if (mySecondRequestID != -1){
                lastRequestSock2 = offers.get(mySecondRequestID).getSock(mySecondRequestRank); // can be null    
            }    
        } 
        else { // Second time offering these socks
            for (int i = 0; i < offers.size(); ++ i) {
                if (i == id) continue;

                for (int rank = 1; rank <= 2; ++ rank) {
                    Sock s = offers.get(i).getSock(rank);
                    for (int mySockRank = 1; mySockRank <= 2; ++ mySockRank) {
                        if (s != null && this.E1.containsKey(new Pair(lastOffer.getSock(mySockRank), s))) {
                            double minDistance = this.E1.get(new Pair(lastOffer.getSock(mySockRank), s));
                            if (minDistance <= minValSoFar) {
                                mySecondRequestID = myFirstRequestID;
                                mySecondRequestRank = myFirstRequestRank;
                                secondMinValSoFar = minValSoFar;
                                myFirstRequestID = i;
                                myFirstRequestRank = rank;
                                minValSoFar = minDistance;
                            } else if (minDistance <= secondMinValSoFar) {
                                secondMinValSoFar = minDistance;
                                mySecondRequestID = i;
                                mySecondRequestRank = rank;
                            }
                        }

                        if (s != null && this.E2.containsKey(new Pair(lastOffer.getSock(mySockRank), s))) {
                            double minDistance = this.E2.get(new Pair(lastOffer.getSock(mySockRank), s));
                            if (minDistance <= minValSoFar) {
                                mySecondRequestID = myFirstRequestID;
                                mySecondRequestRank = myFirstRequestRank;
                                secondMinValSoFar = minValSoFar;
                                myFirstRequestID = i;
                                myFirstRequestRank = rank;
                                minValSoFar = minDistance;
                            } else if (minDistance <= secondMinValSoFar) {
                                secondMinValSoFar = minDistance;
                                mySecondRequestID = i;
                                mySecondRequestRank = rank;
                            }
                        }
                    }
                }
            }
            if (myFirstRequestID == -1) {
                // System.out.println("Here!");
                // do the same thing on all offers
                // but exclude requested ones
                minValSoFar = getTotalEmbarrassment(this.socks);
                secondMinValSoFar = getTotalEmbarrassment(this.socks);
                myFirstRequestID = -1;
                myFirstRequestRank = -1;
                mySecondRequestID = -1;
                mySecondRequestRank = -1;
                int lastRequestFirstID = lastRequests.get(this.id).getFirstID();
                int lastRequestSecondID = lastRequests.get(this.id).getSecondID();
                for (int i = 0; i < offers.size(); ++ i) {
                    if (i == id) continue;
                    for (int rank = 1; rank <= 2; ++ rank) {
                        Sock s = offers.get(i).getSock(rank);
                        if (s != null) {
                            if (lastRequestFirstID == i && lastRequestSock1.equals(s)) {
                                continue;
                            }
                            if (lastRequestSecondID == i && lastRequestSock2.equals(s)) {
                                continue;
                            }
                            Sock[] possibleTrades = {pairToOffer.first, pairToOffer.second};
                            double minDistance = getTotalEmbarrassment(this.socks);
                            for (Sock myOffer : possibleTrades) {
                                minDistance = scoreForTrade(myOffer, s);
                            }
                            if (minDistance <= minValSoFar) {
                                mySecondRequestID = myFirstRequestID;
                                mySecondRequestRank = myFirstRequestRank;
                                secondMinValSoFar = minValSoFar;
                                myFirstRequestID = i;
                                myFirstRequestRank = rank;
                                minValSoFar = minDistance;
                            } else if (minDistance <= secondMinValSoFar) {
                                secondMinValSoFar = minDistance;
                                mySecondRequestID = i;
                                mySecondRequestRank = rank;
                            }
                        }
                    }
                }
            }
        }

        // System.out.println("Time offered: " + timesPairOffered);
        // System.out.println("Current Em: " + getTotalEmbarrassment(this.socks));
        // System.out.println("Min val so far: " + minValSoFar);
        // System.out.println("Second min val: " + secondMinValSoFar); 
        return new Request(myFirstRequestID, myFirstRequestRank, mySecondRequestID, mySecondRequestRank);
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
        Sock oldSock;
        Sock newSock;
        if (transaction.getFirstID() == id) {
            oldSock = transaction.getFirstSock();
            newSock = transaction.getSecondSock();
        } else {
            oldSock = transaction.getSecondSock();
            newSock = transaction.getFirstSock();
        }
        // Remove oldSock from history list
        // We can't offer it anymore
        for (List<Sock> value : playersRequestHistory.values()) {
            while (value.remove(oldSock)) {}
        }   
        socks.remove(oldSock);
        socks.add(newSock);
        // System.out.println("b" + socks.size());
        repair();
        adjustThreshold();
        offerIndex = 0;
        E1.clear();
    }

    @Override
    public List<Sock> getSocks() {
        if (t == 0) {
            this.repair();
        }
        return Arrays.asList(this.getSockArray());
    }

    private float[][] getCostMatrix(Sock[] sockArray) {
        float[][] matrix = new float[2*n*(2*n-1)/2][3];
        int idx = 0;
        for (int i = 0; i < sockArray.length; i++) {
            for (int j=i+1; j< sockArray.length; j++) {
                matrix[idx] = new float[]{i, j, (float)(-sockArray[i].distance(sockArray[j]))};
                idx ++;
            }
        }
        return matrix;
    }

    public ArrayList<Sock> switchSockAndRepair(Sock N, Sock Q)  {
        ArrayList<Sock> socksSwitched = new ArrayList<Sock>(this.socks);
        int index = this.socks.indexOf(Q);
        socksSwitched.set(index, N);
        pair(socksSwitched);
        return socksSwitched;
    }
    
    
    // Embarrassment calculation for current list of sockets
    private double getTotalEmbarrassment(Sock[] list) {

        double result = 0;
        for (int i = 0; i < list.length; i += 2)
            result += list[i].distance(list[i + 1]);
        return result;
    }    
    // Embarrassment calculation for current list of sockets
    private double getTotalEmbarrassment(ArrayList<Sock> list) {

        double result = 0;
        for (int i = 0; i < list.size(); i += 2)
            result += list.get(i).distance(list.get(i+1));
        return result;
    }  

}