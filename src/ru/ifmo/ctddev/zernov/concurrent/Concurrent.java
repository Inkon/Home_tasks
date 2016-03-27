package ru.ifmo.ctddev.zernov.concurrent;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Class with methods defined in {@link info.kgeorgiy.java.advanced.concurrent.ListIP ListIP} and speeds up some default
 * operations with
 * list. This class work with some amount of {@link Thread threads} and {@link List} of given <tt>elements</tt>.
 * @see Thread
 * @see info.kgeorgiy.java.advanced.concurrent.ListIP
 */

public class Concurrent implements info.kgeorgiy.java.advanced.concurrent.ListIP {

    /**
     * return the maximum of elements in given list of <tt>values</tt> specified by comparator (or natural order, if
     * comparator is <tt>null</tt>
     * @param threads amount of {@link Thread threads} working with
     * @param values list of given elements
     * @param comparator comparator which comparing elements
     * @param <T> type of elements in the <tt>values</tt>
     * @return maximum of elements in <tt>values</tt>
     * @throws InterruptedException if method was interrupted during it's invoke
     * @see Comparator#naturalOrder()
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return new Worker<T, T, T>(threads, values, (T a) -> (a), new UnaryMonoid<T>(values.get(0)) {
            @Override
            public T divide(T first, T second) {
                int cmp = comparator.compare(first, second);
                return cmp >= 0 ? first : second;
            }
        }, (T a)->(false)).calculate();
    }

    /**
     * return the minimum of elements in given list of <tt>values</tt> specified by comparator (or natural order, if
     * comparator is <tt>null</tt>
     * @param threads amount of {@link Thread threads} working with
     * @param values list of given elements
     * @param comparator comparator which comparing elements
     * @param <T> type of elements in the <tt>values</tt>
     * @return maximum of elements in <tt>values</tt>
     * @throws InterruptedException if method was interrupted during it's invoke
     * @see Comparator#naturalOrder()
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return new Worker<T, T, T>(threads, values, (T a) -> (a), new UnaryMonoid<T>(values.get(0)) {
            @Override
            public T divide(T first, T second) {
                int cmp = comparator.compare(first, second);
                return cmp <= 0 ? first : second;
            }
        },(T a)->(false)).calculate();
    }

    /**
     * return true if all elements from list of <tt>values</tt> satisfying <tt>predicate</tt> and false otherwise
     * @param threads amount of {@link Thread threads} working with
     * @param values list of given elements
     * @param predicate condition of filtering elements
     * @param <T> type of elements in the <tt>values</tt>
     * @return true if all elements from list of <tt>values</tt> satisfying <tt>predicate</tt> and false otherwise
     * @throws InterruptedException if method was interrupted during it's invoke
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return new Worker<T, Boolean, Boolean>(threads, values, (T a) -> (predicate.test(a)), new UnaryMonoid<Boolean>(true) {
            @Override
            public Boolean divide(Boolean first, Boolean second) {
                return first && second;
            }
        },(Boolean a)->(!a)).calculate();
    }

    /**
     * return true if at least one element from list of <tt>values</tt> satisfying <tt>predicate</tt> and false otherwise
     * @param threads amount of {@link Thread threads} working with
     * @param values list of given elements
     * @param predicate condition of filtering elements
     * @param <T> type of elements in the <tt>values</tt>
     * @return true if at least one element from list of <tt>values</tt> satisfying <tt>predicate</tt> and false otherwise
     * @throws InterruptedException if method was interrupted during it's invoke
     * @see List#contains(Object)
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return new Worker<T, Boolean, Boolean>(threads, values, (T a) -> (predicate.test(a)), new UnaryMonoid<Boolean>(false) {
            @Override
            public Boolean divide(Boolean first, Boolean second) {
                return first || second;
            }
        },(Boolean a)->(a)).calculate();
    }

    /**
     * return concatenation of string representation elements from <tt>values</tt> list
     * @param threads amount of {@link Thread threads} working with
     * @param values list of given elements
     * @return concatenation of string representation elements from <tt>values</tt> list
     * @throws InterruptedException if method was interrupted during it's invoke
     * @see String#concat(String)
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        StringBuilder builder = new Worker<Object, StringBuilder, String>(threads, values, (Object a) -> (a.toString()),
                new Monoid<String, StringBuilder>() {
            @Override
            public StringBuilder divide(StringBuilder first, StringBuilder second) {
                return first.append(second);
            }

            @Override
            public StringBuilder getZero() {
                return new StringBuilder();
            }

            @Override
            public StringBuilder insert(StringBuilder container, String element) {
                return container.append(element);
            }
        },(String a)->(false)).calculate();
        return builder.toString();
    }

    /**
     * return list of elements satisfying <tt>predicate</tt> from given list of <tt>values</tt>
     * @param threads amount of {@link Thread threads} working with
     * @param values list of given elements
     * @param predicate condition of filtering elements
     * @param <T> type of elements in the <tt>values</tt>
     * @return list of elements satisfying <tt>predicate</tt> from given list of <tt>values</tt>
     * @throws InterruptedException if method was interrupted during it's invoke
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return new Worker<T, List<T>, T>(threads, values, (T t) -> (t), new ListMonoid<T>() {
            @Override
            public List<T> insert(List<T> container, T element) {
                if (predicate.test(element)) {
                    container.add(element);
                }
                return container;
            }
        },(T a)->(false)).calculate();
    }

    /**
     * return list of <tt>function</tt> application's results
     * @param threads amount of {@link Thread threads} working with
     * @param values list of given elements
     * @param f function describing converting from <tt>T</tt> type to <tt>U</tt> type
     * @param <T> type of elements in the <tt>values</tt>
     * @param <U> type of function result
     * @return list of <tt>function</tt> application's results
     * @throws InterruptedException if method was interrupted during it's invoke
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return new Worker<T, List<U>, U>(threads, values, (T t) -> (f.apply(t)), new ListMonoid<>(), (U a)->(false)).calculate();
    }

    private abstract class UnaryMonoid<T> implements Monoid<T, T> {
        T zero;

        private UnaryMonoid(T zero) {
            this.zero = zero;
        }

        @Override
        public T getZero() {
            return zero;
        }

        @Override
        public T insert(T container, T element) {
            return divide(container, element);
        }
    }

    private class ListMonoid<T> implements Monoid<T, List<T>> {
        @Override
        public List<T> divide(List<T> first, List<T> second) {
            first.addAll(second);
            return first;
        }

        @Override
        public List<T> getZero() {
            return new ArrayList<>();
        }

        @Override
        public List<T> insert(List<T> container, T element) {
            container.add(element);
            return container;
        }
    }
}
