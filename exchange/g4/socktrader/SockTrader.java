package exchange.g4.socktrader;

import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;
import exchange.sim.Transaction;

import exchange.sim.*;

import exchange.g4.marketvalue.*;
import exchange.g4.kdtree.*;


import java.util.*;
import java.lang.Math;
import javafx.util.*;

public class SockTrader{

      public ArrayList<Sock> socks = null;
      public List<Offer> lastOffers = null;
      public MarketValue market = null;
      public int player_id = -1;
      public ArrayList<Double> minDistance = null;
      public int sock_id1 = -1;
      public int sock_id2 = -1;

      public SockTrader(ArrayList<Sock> socks, int id)
      {
        this.socks = socks;
        this.player_id = id;
        this.market = new MarketValue(this.player_id,20);

      }


      public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions){

        if(this.lastOffers!=null)
          market.updateMarket(lastRequests,lastTransactions,this.lastOffers); //Not used right now

        this.minDistance = computeDistanceArray();


        this.sock_id1 = maxIndex(minDistance);
        Sock offer_sock_1 = maxSock(minDistance);

        ArrayList<Integer> exclude_list = new ArrayList<Integer>();
        exclude_list.add((Integer)this.sock_id1);

        this.sock_id2 = maxIndex(minDistance,exclude_list);
        Sock offer_sock_2 = maxSock(minDistance,exclude_list);

        return new Offer(offer_sock_1,offer_sock_2);

      }

      public Request requestExchange(List<Offer> offers) {

          //ArrayList<Sock> socks_offered = new ArrayList<Sock>();

          this.lastOffers = offers;

          Pair<Integer,Integer> offer_index = pickBestRequest(offers);

          // if(offer_index.getKey()<0)
          // {
          //   return offer_index;
          // }

          int player = offer_index.getKey();
          int rank = offer_index.getValue();

          return new Request(player,rank,-1,-1);

      }

      public Pair<Integer, Integer> pickBestRequest(List<Offer> offers)
      {
          ArrayList<Sock> exclude_list = new ArrayList();

          Sock s1 = offers.get(player_id).getFirst();
          Sock s2 = offers.get(player_id).getSecond();

          if(s1!=null)
          {
            exclude_list.add(s1);
          }

          if(s2!=null)
          {
            exclude_list.add(s2);
          }

      double mn = Double.MAX_VALUE;
      Pair<Integer,Integer> mndex = new Pair<>(-1,-1);

          int mxdex_i2 = -1;
          int mxdex_r2 = -1;
          //double mx = -1;
          for(int i  = 0;i<offers.size();i++)
          {
            if(i!=this.player_id)
            {
              for(int j = 1;j<=2;j++)
              {
                if (offers.get(i).getSock(j) != null)
                {
                  Sock s = offers.get(i).getSock(j);
                  if(getRequestValue(s,exclude_list)<mn)
                  {
                    mndex = new Pair<>(i,j);
                    mn = getRequestValue(s,exclude_list);
                  }
                }
              }
            }
          }


          return mndex;
      }

      public Double getRequestValue(Sock s,ArrayList <Sock> exclude_list)
      {

        return getExternalMinDistance(s,exclude_list);

      }
      public void updateInformation(ArrayList<Sock> socks)
        {
          this.socks = socks;
        }

        public void updateInformation(List<Offer> offers)
          {
            this.lastOffers = offers;
          }


      public Double getMinDistance(int i)
        {
                int n = this.socks.size();
                if(i<0 || i >n)
                  return Double.MAX_VALUE;

                  int j = (i+1)%n;

                  Sock cur = this.socks.get(i);

                  Double mn = cur.distance(this.socks.get(j));

                  for(int k = 0;k<n;k++)
                  {

                    if(k==i)
                      continue;

                    Double dist =cur.distance(this.socks.get(k));

                    mn = Math.min(dist,mn);

                  }

                return mn;

            }

            public Double getExternalMinDistance(Sock s)
            {
              Double mn = Double.MAX_VALUE;

              for(Sock cur: this.socks)
              {
                mn = Math.min(mn,s.distance(cur));
              }

              return mn;
            }

            public Double getExternalMinDistance(Sock s, ArrayList<Sock> exclude_list)
            {
              Double mn = Double.MAX_VALUE;

              for(Sock cur: this.socks)
              {
                if(!exclude_list.contains(cur))
                  mn = Math.min(mn,s.distance(cur));
              }

              return mn;

            }

            public ArrayList<Double> computeDistanceArray()// Right now uses most isolated sock distance. Can use k means or cluster distance as well
            {

              ArrayList<Double> minDistance= new ArrayList<Double>(Collections.nCopies(this.socks.size(),(Double)0.0));
              int n = socks.size();

              for(int i = 0;i<n;i++)
              {
                  int j = (i+1)%n;

                  Sock cur = this.socks.get(i);

                  Double mn = cur.distance(this.socks.get(j));

                  for(int k = 0;k<n;k++)
                  {

                    if(k==i)
                      continue;

                    Double dist =cur.distance(this.socks.get(k));

                    mn = Math.min(dist,mn);

                  }

                  minDistance.set(i,mn + market.getSockMarketValue(this.socks.get(i)));
              }

              return minDistance;
            }

            public int maxIndex(ArrayList<Double> array)
            {
              int mxdex = -1;
              int n = array.size();
              Double mx = -Double.MAX_VALUE;

              for(int i = 0;i<n;i++)
              {
                if(array.get(i) > mx)
                {
                  mx = array.get(i);
                  mxdex = i;
                }
              }

              return mxdex;

            }

            public int maxIndex(ArrayList<Double> array, ArrayList<Integer> exclude_list)
            {
              int mxdex = -1;
              int n = array.size();
              Double mx = -Double.MAX_VALUE;

              for(int i = 0;i<n;i++)
              {
                if(!exclude_list.contains((Integer)i) && array.get(i) > mx)
                {
                  mx = array.get(i);
                  mxdex = i;
                }
              }

              return mxdex;

            }

            public Sock maxSock(ArrayList<Double> array)
            {
              int max_index = maxIndex(array);

              return this.socks.get(max_index);
            }

            public Sock maxSock(ArrayList<Double> array, ArrayList<Integer> exclude_list)
            {
              int max_index = maxIndex(array,exclude_list);

              return this.socks.get(max_index);
            }


}
