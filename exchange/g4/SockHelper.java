package exchange.g4;
import exchange.sim.*;
import exchange.g4.kdtree.*;
import java.util.*;
public class SockHelper {
    public static KDTree<Sock> myTree = new KDTree<Sock>(3);
    public static boolean canDelete(Sock s){
        double[] curr = {s.R,s.G,s.B};
        Sock ans = new Sock(12,12,12);
        try{
            if(myTree.nearest(curr,1).size()==0){
                return false;
            }
            ans = myTree.nearest(curr,1).get(0);
        }
        catch (KeySizeException e) {
            e.printStackTrace();
        }
        return ans.R == s.R && ans.G == s.G && ans.B == s.B;
    }

    public static  ArrayList<Sock> getSocks(ArrayList<Sock> socks) {
        myTree = new KDTree<Sock>(3);
        for(int i=0;i<socks.size();i++){
            Sock curr = socks.get(i);
            double[] colors= {curr.R,curr.G,curr.B};
            try {
                try {
                    myTree.insert(colors, socks.get(i));
                } catch (KeySizeException e) {
                    e.printStackTrace();
                }
            }
            catch (KeyDuplicateException e) {
                continue;
            }
        }
        //System.out.println(myTree.size());
        ArrayList<Sock> ans = new ArrayList<Sock>();
        HashMap<Sock,Integer> isPaired = new HashMap<Sock, Integer>();
        for(int i=0;i<socks.size();i++){
            //System.out.println(i);
            Sock curr = socks.get(i);
            if(isPaired.get(curr)!=null){
                System.out.println(curr);
                continue;
            }
            double[] colors = {curr.R,curr.G,curr.B};

            List<Sock> near = null;
            try {
                near = myTree.nearest(colors,2);
            } catch (KeySizeException e) {
                e.printStackTrace();
            }

            for(int j=0;j<near.size();j++){
                isPaired.put(near.get(j),1);
                ans.add(near.get(j));
                Sock toRemove = near.get(j);
                double[] vanish = {toRemove.R,toRemove.G,toRemove.B};
                try {
                    try {
                        myTree.delete(vanish);
                    } catch (KeySizeException e) {
                        e.printStackTrace();
                    }
                } catch (KeyMissingException e) {
                    e.printStackTrace();
                }
            }
        }
        return ans;
    }
}