package exchange.g4.marketvalue;

import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;
import exchange.sim.Transaction;

import java.util.*;
import java.lang.Math;
import javafx.util.*;

public class MarketValue{

  public double [][][] mval; // Higher value means market values sock more
  public double eps = 10;
  public int id;
  public int maxdepth = 5;
  public MarketValue(int id)
  {
    mval = new double [256][256][256];
    this.id = id;
  }
  public MarketValue(int id,int maxdepth)
  {
    mval = new double [256][256][256];
    this.id = id;
    this.maxdepth = maxdepth;
  }

  public MarketValue(int id,int maxdepth,double eps)
  {
    mval = new double [256][256][256];
    this.id = id;
    this.maxdepth = maxdepth;
    this.eps = eps;
  }
  public MarketValue()
  {
    mval = new double [256][256][256];
    this.id = -1;
  }
  public void updateMarket(List<Request> lastRequests, List<Transaction> lastTransactions, List<Offer> lastOffers)
  {
    for(Request request: lastRequests)
    {
        if(request == null)
          continue;
          if (request.getFirstID() >= 0 && request.getFirstRank() >= 0) {
              Sock first = lastOffers.get(request.getFirstID()).getSock(request.getFirstRank());
              updateLocation(first);
              //updateLocation(first,maxdepth);
          }
          if (request.getSecondID() >= 0 && request.getSecondRank() >= 0) {
              Sock second = lastOffers.get(request.getSecondID()).getSock(request.getSecondRank());
              updateLocation(second);
              //marketValue[second.R / 32][second.G / 32][second.B / 32] += Math.pow(totalTurns - currentTurn, 2);
              //updateLocation(second, maxdepth);
          }


    }


  }
  public void updateLocation(Sock s)
  {
    if(s==null)
      return;

    updateLocation(s,this.eps);
  }
  public void updateLocation(Sock s, double eps)
  {
    if(s==null)
      return;


    int R = s.R/16;
    int G = s.G/16;
    int B = s.B/16;

    mval[R][G][B] += eps;
  }

  public void updateLocation(Sock s, int maxdepth)
  {
    if(s==null)
      return;


    int R = s.R;
    int G = s.G;
    int B = s.B;


    for(int r = R-maxdepth; r<=R+maxdepth;r++)
    {
      for(int g = G -maxdepth; g<=G+maxdepth;g++)
      {
        for(int b = B - maxdepth;b<=B+maxdepth;b++)
        {
          if(0<=r && r<=255 && 0<=g && g<=255 && 0<=b && b<=255)
          {
            //double val = eps * Math.exp(-s.distance(new Sock(r,g,b)));
            double val = Math.max((eps*3.0)/s.distance(new Sock(r,g,b)),eps - s.distance(new Sock(r,g,b)) );

            updateLocation(s,val);

          }
        }
      }
    }

  }
  public double getSockMarketValue(Sock s)
  {
    if(s==null)
      return -1;


    return mval[s.R/16][s.G/16][s.B/16];
  }
  public Offer makeOffer(ArrayList<Sock> socks)
  {
    int mxdex = 0;
    double mx = getSockMarketValue(socks.get(0));

    for(int i = 1;i<socks.size();i++)
    {
      Sock s = socks.get(i);

      if(getSockMarketValue(s)>mx)
      {
        mx = getSockMarketValue(s);
        mxdex = i;
      }


    }
    int mxdex2 = -1;
    double mx2 = -1;

    for(int i = 0;i<socks.size();i++)
    {
      Sock s = socks.get(i);

      if(getSockMarketValue(s)>mx2  && i!=mxdex)
      {
        mx2 = getSockMarketValue(s);
        mxdex2 = i;
      }


    }

    return new Offer(socks.get(mxdex), socks.get(mxdex2));


  }

  public Request chooseRequest(List<Offer> offers)
  {

    //ArrayList<Sock> socks_offered = new ArrayList<Sock>();

    double mx = -1;
Pair<Integer,Integer> mxdex = new Pair<>(-1,-1);

    int mxdex_i2 = -1;
    int mxdex_r2 = -1;
    //double mx = -1;
    for(int i  = 0;i<offers.size();i++)
    {
      if(i!=id)
      {

        if (offers.get(i).getFirst() != null)
        {
          Sock s = offers.get(i).getFirst();
          if(getSockMarketValue(s)>mx)
          {
            mxdex = new Pair<>(i,1);
            mx = getSockMarketValue(s);
          }
        }
        if (offers.get(i).getSecond() != null)
        {
          {
            Sock s = offers.get(i).getSecond();
            if(getSockMarketValue(s)>mx)
            {
              mxdex = new Pair<>(i,2);
              mx = getSockMarketValue(s);
            }
          }
        }
      }
    }

    return new Request(mxdex.getKey(),mxdex.getValue(),-1,-1);

  }
/// Functions that are not used (You can ignore these)

public ArrayList<Sock> getNeighbhours(Sock s)
{
  ArrayList<Sock> dir = new ArrayList<Sock>();
  ArrayList<Sock> ans = new ArrayList<Sock>();

  dir.add(new Sock(-1,0,0));
  dir.add(new Sock(1,0,0));

  dir.add(new Sock(0,-1,0));
  dir.add(new Sock(0,1,0));

  dir.add(new Sock(0,0,-1));
  dir.add(new Sock(0,0,1));

  dir.add(new Sock(-1,1,0));
  dir.add(new Sock(1,1,0));

  dir.add(new Sock(-1,-1,0));
  dir.add(new Sock(1,-1,0));

  dir.add(new Sock(-1,0,1));
  dir.add(new Sock(1,0,1));

  dir.add(new Sock(-1,0,-1));
  dir.add(new Sock(1,0,-1));

  dir.add(new Sock(0,-1,-1));
  dir.add(new Sock(0,-1,1));

  dir.add(new Sock(0,1,-1));
  dir.add(new Sock(0,1,1));

  dir.add(new Sock(1,1,1));
  dir.add(new Sock(1,1,-1));

  dir.add(new Sock(1,-1,1));
  dir.add(new Sock(1,-1,-1));

  dir.add(new Sock(-1,1,1));
  dir.add(new Sock(-1,1,-1));

  dir.add(new Sock(-1,-1,1));
  dir.add(new Sock(-1,-1,-1));


  for(Sock d: dir)
  {
    int r = s.R + d.R;
    int g = s.G + d.G;
    int b = s.B + d.B;

    if(0<=r && r<=255 && 0<=g && g<=255 && 0<=b && b<=255)
    {
      ans.add(new Sock(r,g,b));
    }
  }


  return ans;

}



}
