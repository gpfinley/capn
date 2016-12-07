package util;

/**
 * Simple class for a pair of objects; can be made order sensitive by calling constructor with true
 *
 * @param <T>
 */
public class Pair<T> {
    private final T one;
    private final T two;
    private final boolean orderMatters;

    public Pair(T one, T two) {
        this(one, two, false);
    }

    public Pair(T one, T two, boolean orderMatters) {
        this.orderMatters = orderMatters;
        this.one = one;
        this.two = two;
    }

    public boolean getOrderMatters() {
        return orderMatters;
    }

    public T one() {
        return one;
    }

    public T two() {
        return two;
    }

    @Override
    public String toString() {
        return "<" + one.toString() + ", " + two.toString() + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?> pair = (Pair<?>) o;
        return (one.equals(pair.one) && two.equals(pair.two)) || (!orderMatters && one.equals(pair.two) && two.equals(pair.one));
    }

    @Override
    public int hashCode() {
        return orderMatters ? 31 * one.hashCode() + two.hashCode() : 31 * (one.hashCode() + two.hashCode());
    }

}
