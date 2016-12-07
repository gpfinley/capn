package util;

import java.util.*;

/**
 * Simple class for getting all possible pairings of two items from a set (order invariant)
 * @param <T>
 */
public class Pairwise<T> implements Iterable<Pair<T>> {
    private final List<Pair<T>> pairs;

    public Pairwise(Iterable<T> individuals) {
        this(individuals, false);
    }

    public Pairwise(Iterable<T> individuals, boolean orderMatters) {
        Set<T> indiv = new HashSet<>();
        for(T t : individuals) {
            indiv.add(t);
        }
        pairs = new ArrayList<>();
        List<T> listed = new ArrayList<>(indiv);
        for(int i=0; i<listed.size(); i++) {
            for(int j=i+1; j<listed.size(); j++) {
                pairs.add(new Pair<>(listed.get(i), listed.get(j), orderMatters));
            }
        }
    }

    public Pairwise(Iterable<T> individuals, Set<T> filterOn) {
        // todo: determine if adding a new HashSet and an ArrayList really slows it down
        // todo: if so, enforce a set
        Set<T> indiv = new HashSet<>();
        for(T t : individuals) {
            indiv.add(t);
        }
        pairs = new ArrayList<>();
        List<T> listed = new ArrayList<>(indiv);
        for(int i=0; i<listed.size(); i++) {
            for(int j=i+1; j<listed.size(); j++) {
                if(filterOn.contains(listed.get(i)) && filterOn.contains(listed.get(j))) {
                    pairs.add(new Pair<>(listed.get(i), listed.get(j)));
                }
            }
        }
    }

    @Override
    public Iterator<Pair<T>> iterator() {
        return pairs.iterator();
    }

    public int size() {
        return pairs.size();
    }

}

