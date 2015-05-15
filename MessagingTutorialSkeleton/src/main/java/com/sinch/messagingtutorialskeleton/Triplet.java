package com.sinch.messagingtutorialskeleton;

public class Triplet<F, S, T> {
    public final F first;
    public final S second;
    public final T third;
    /**
     * Constructor for a Triplet.
     *
     * @param first the first object in the Triplet
     * @param second the second object in the Triplet
     * @param third the third object in the Triplet
     */
    public Triplet(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }
    /**
     * Convenience method for creating an appropriately typed triple.
     * @param a the first object in the Triplet
     * @param b the second object in the Triplet
     * @param c the second object in the Triplet
     * @return a Triplet that is templatized with the types of a, b amd C
     */
    public static <A, B, C> Triplet <A, B, C> create(A a, B b, C c) {
        return new Triplet<A, B, C>(a, b, c);
    }
}
