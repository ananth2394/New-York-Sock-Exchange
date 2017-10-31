/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exchange.g4.edmonds;

import java.util.ArrayList;

/**
 * @author raf
 */
public class Dumbbell {

    Blossom b1, b2;
    Edge connectingEdge;

    public Dumbbell(Blossom b1, Blossom b2, Edge e) {
        // nastavime kvetom v cinke paritu 0, teda ziadnu, lebo na ne sa nevztahuje
        // zmena(r), cize nema zmysel riesit paritu
        this.b1 = b1;
        b1.levelParity = 0;
        b1.dumbbellRef = this;
        b1.treeNodeRef = null;

        this.b2 = b2;
        b2.levelParity = 0;
        b2.dumbbellRef = this;
        b2.treeNodeRef = null;
        // este upravime stopky, aby sedeli (mali by byt pri hrane, ktora spaja cinky)
        if (b1 instanceof GreenBlossom) {
            GreenBlossom gb1 = (GreenBlossom) b1;
            gb1.setStopkaByEdge(e);
        }

        if (b2 instanceof GreenBlossom) {
            GreenBlossom gb2 = (GreenBlossom) b2;
            gb2.setStopkaByEdge(e);
        }

        connectingEdge = e;
    }

    @Override
    public String toString() {
        if (false && b1 instanceof BlueBlossom && b2 instanceof BlueBlossom) {
            return (((BlueBlossom) b1).vertex.id + 1) + "--" + (((BlueBlossom) b2).vertex.id + 1);
        } else {
            return b1 + " " + b2;
        }
    }

    public Pair<Integer, ArrayList<Edge>> getTotalMatchingPrice() {
//        System.out.println(connectingEdge);
        int total_price = connectingEdge.price;
        Pair<Integer, ArrayList<Edge>> R1 = b1.getMatchingPrice();
        Pair<Integer, ArrayList<Edge>> R2 = b2.getMatchingPrice();

        total_price += R1.getLeft() + R2.getLeft();

        ArrayList<Edge> edge_list = new ArrayList<>();
        edge_list.addAll(R1.getRight());
        edge_list.addAll(R2.getRight());
        edge_list.add(this.connectingEdge);
        return new Pair<Integer, ArrayList<Edge>>(total_price, edge_list);

    }

    public Edge getConnectingEdge() {
        return this.connectingEdge;
    }

}
