package ru.ifmo.ctddev.zernov.concurrent;

import ru.ifmo.ctddev.zernov.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Class with methods defined in {@link info.kgeorgiy.java.advanced.concurrent.ListIP ListIP} and speeds up some default
 * operations with
 * list. This class work with some amount of {@link Thread threads} and {@link List} of given <tt>elements</tt>.
 */
public class Concurrent implements info.kgeorgiy.java.advanced.concurrent.ListIP {

    info.kgeorgiy.java.advanced.mapper.ParallelMapper mapper;

    /**
     * Create class with {@link info.kgeorgiy.java.advanced.mapper.ParallelMapper} m which will be using in methods call
     *
     * @param m mapper will be using in methods call
     */

    public Concurrent(info.kgeorgiy.java.advanced.mapper.ParallelMapper m) {
        mapper = m;
    }

    /**
     * Create class without any {@link info.kgeorgiy.java.advanced.mapper.ParallelMapper}
     *
     */

    public Concurrent(){
        mapper = null;
    }

    /**
     * return the maximum of elements in given list of <tt>values</tt> specified by comparator (or natural order, if
     * comparator is <tt>null</tt>
     *
     * @param threads    amount of {@link Thread threads} working with
     * @param values     list of given elements
     * @param comparator comparator which comparing elements
     * @param <T>        type of elements in the <tt>values</tt>
     * @return maximum of elements in <tt>values</tt>
     * @throws InterruptedException if method was interrupted during it's invoke
     * @see Comparator#naturalOrder()
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return mapWithMapper((lst) -> lst.stream().max(comparator).get(), splt(values, threads), threads).stream().max(comparator).get();
    }

    private <T> List<List<T>> splt(List<T> list, int count) {
        ArrayList<List<T>> result = new ArrayList<>();
        int size = count * 2 <= list.size() ? count : Math.max(1, list.size() / 2),
                block = list.size() / size, rest = list.size() - block * size;

        for (int i = 0; i < size; i++) {
            int from = i * block + (i >= rest ? rest : i);
            int to = (i + 1) * block + (i >= rest ? rest : i + 1);
            result.add(list.subList(from, to));
        }
        return result;
    }

    private <T, R> List<R> mapWithMapper(Function<? super  T, ? extends R> f, List<? extends T> list, int threads)
            throws InterruptedException {
        List<R> result;
        boolean wasNull = mapper == null;
        if (wasNull){
            mapper = new ParallelMapper(threads);
        }
        result = mapper.map(f, list);
        if (wasNull){
            mapper.close();
            mapper = null;
        }
        return result;
    }

    /**
     * return the minimum of elements in given list of <tt>values</tt> specified by comparator (or natural order, if
     * comparator is <tt>null</tt>
     *
     * @param threads    amount of {@link Thread threads} working with
     * @param values     list of given elements
     * @param comparator comparator which comparing elements
     * @param <T>        type of elements in the <tt>values</tt>
     * @return maximum of elements in <tt>values</tt>
     * @throws InterruptedException if method was interrupted during it's invoke
     * @see Comparator#naturalOrder()
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return mapWithMapper((lst) -> lst.stream().min(comparator).get(), splt(values, threads), threads).stream().min(comparator).get();
    }

    /**
     * return true if all elements from list of <tt>values</tt> satisfying <tt>predicate</tt> and false otherwise
     *
     * @param threads   amount of {@link Thread threads} working with
     * @param values    list of given elements
     * @param predicate condition of filtering elements
     * @param <T>       type of elements in the <tt>values</tt>
     * @return true if all elements from list of <tt>values</tt> satisfying <tt>predicate</tt> and false otherwise
     * @throws InterruptedException if method was interrupted during it's invoke
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return mapWithMapper((lst) -> lst.stream().allMatch(predicate), splt(values, threads), threads).stream().allMatch(x -> x);
    }

    /**
     * @param threads   amount of {@link Thread threads} working with
     * @param values    list of given elements
     * @param predicate condition of filtering elements
     * @param <T>       type of elements in the <tt>values</tt>
     * @return true if at least one element from list of <tt>values</tt> satisfying <tt>predicate</tt> and false otherwise
     * @throws InterruptedException if method was interrupted during it's invoke
     * @see List#contains(Object)
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return mapWithMapper((lst) -> lst.stream().anyMatch(predicate), splt(values, threads), threads).stream().anyMatch(x -> x);
    }

    /**
     * return concatenation of string representation elements from <tt>values</tt> list
     *
     * @param threads amount of {@link Thread threads} working with
     * @param values  list of given elements
     * @return concatenation of string representation elements from <tt>values</tt> list
     * @throws InterruptedException if method was interrupted during it's invoke
     * @see String#concat(String)
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return mapWithMapper(lst -> lst.stream().map(Object::toString).collect(Collectors.joining("")), splt(values, threads), threads).
                stream().collect(Collectors.joining(""));
    }

    /**
     * return list of elements satisfying <tt>predicate</tt> from given list of <tt>values</tt>
     *
     * @param threads   amount of {@link Thread threads} working with
     * @param values    list of given elements
     * @param predicate condition of filtering elements
     * @param <T>       type of elements in the <tt>values</tt>
     * @return list of elements satisfying <tt>predicate</tt> from given list of <tt>values</tt>
     * @throws InterruptedException if method was interrupted during it's invoke
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return mapWithMapper(list -> list.stream().filter(predicate).collect(Collectors.toList()),
                splt(values, threads), threads).stream().flatMap(x -> x.stream()).collect(Collectors.toList());
    }

    /**
     * return list of <tt>function</tt> application's results
     *
     * @param threads amount of {@link Thread threads} working with
     * @param values  list of given elements
     * @param f       function describing converting from <tt>T</tt> type to <tt>U</tt> type
     * @param <T>     type of elements in the <tt>values</tt>
     * @param <U>     type of function result
     * @return list of <tt>function</tt> application's results
     * @throws InterruptedException if method was interrupted during it's invoke
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return mapWithMapper(list -> list.stream().map(f).collect(Collectors.toList()),
                splt(values, threads), threads).stream().flatMap(x -> x.stream()).collect(Collectors.toList());
    }
}
