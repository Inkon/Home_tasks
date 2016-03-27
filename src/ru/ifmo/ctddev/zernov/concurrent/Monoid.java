package ru.ifmo.ctddev.zernov.concurrent;

/**
 * Describes a element group of elements with binary operation <tt>division</tt> and neutral element which could be
 * received by <tt>getZero</tt> method. You can insert an element of <tt>C</tt> type into this monoid using
 * <tt>insert</tt> method
 *
 * @param <C> type of elements included in monoid
 * @param <T> type-element group of <tt>C</tt> type
 */
public interface Monoid<C, T> {
    T divide(T first, T second);

    T getZero();

    T insert(T container, C element);
}