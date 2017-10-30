/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exchange.g4.edmonds;

/**
 *
 * @author raf
 */
public class Edge {
    public Vertex u,v;
    public int price;

    public Edge(Vertex u, Vertex v, int price) {
        this.u = u;
        this.v = v;
        this.price = price;
    }

    @Override
    public String toString(){
        return (this.u.id + 1) + " " + (this.v.id + 1) + " " + this.price;
    }

    public Vertex getU()
    {
      return this.u;
    }

    public Vertex getV()
    {
      return this.v;
    }

    public int getPrice()
    {
      return this.price;
    }
}
