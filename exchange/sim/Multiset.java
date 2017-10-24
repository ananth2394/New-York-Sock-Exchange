package exchange.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Multiset<E> {
    private int size;
    private Map<E, Integer> map;

    public Multiset() {
        map = new HashMap<E, Integer>();
    };

    public void add(E e) {
        if (map.containsKey(e)) {
            int v = map.get(e);
            map.put(e, v + 1);
        } else map.put(e, 1);
    }

    public boolean contains(E e) {
        return map.containsKey(e);
    }

    public void remove(E e) {
        if (map.containsKey(e)) {
            int v = map.get(e);
            if (v == 1) map.remove(e);
            else map.put(e, v - 1);
        }
    }

    public List<E> toList() {
        List<E> ret = new ArrayList<E>();
        for (Map.Entry<E, Integer> entry : map.entrySet())
            for (int i = 0; i < entry.getValue(); ++i)
                ret.add(entry.getKey());
        return ret;
    }
}
