package ru.ifmo.ctddev.zernov.mapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


/**
 * Class to parallel mapping function to list using some amount of <tt>threads</tt>
 * This class is thread-safe so can be used in different threads
 *
 * @see info.kgeorgiy.java.advanced.mapper.ParallelMapper
 */

public class ParallelMapper implements info.kgeorgiy.java.advanced.mapper.ParallelMapper {
    private Thread[] threads;

    private final ArrayDeque<Integer> deque = new ArrayDeque<>();
    private final ArrayDeque<Function<Integer, Boolean>> functions = new ArrayDeque<>();

    /**
     * Creating mapper with amount of <tt>threads</tt> which will be using in mapping function
     * @param threads amount of threads mapper use
     */

    public ParallelMapper(int threads) {
        this.threads = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            this.threads[i] = new Thread(new Runner());
            this.threads[i].start();
        }
    }

    private class Runner implements Runnable {
        @Override
        public void run() {
            while (true) {
                int elem;
                Function<Integer, Boolean> f;
                synchronized (deque) {
                    while (deque.isEmpty()) {
                        try {
                            deque.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    elem = deque.removeFirst();
                    f = functions.removeFirst();
                }
                f.apply(elem);
            }
        }
    }

    /**
     * return a {@link List list} of <tt>R</tt> type mapping function <tt>f</tt> to list of arguments <tt>args</tt>
     * @param f function which invoked to arguments of list
     * @param args input list
     * @param <T> type of input list arguments
     * @param <R> type of result list arguments
     * @return list of result mapping function to arguments of <tt>args</tt> list
     * @throws InterruptedException in case of interrupting thread during working
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        int size = threads.length * 2 <= args.size() ? threads.length : Math.max(1, args.size() / 2),
                block = args.size() / size, rest = args.size() - block * size;
        ArrayList<R> result = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            result.add(null);
        }
        ArrayList<Integer> list = new ArrayList<>();
        Function<Integer, Boolean> fun = integer -> {
            int from = integer * block + (integer >= rest ? rest : integer);
            int to = (integer + 1) * block + (integer >= rest ? rest : integer + 1);
            ArrayList<R> rs = new ArrayList<>();
            for (int i = 0; i < to - from; i++) {
                rs.add(f.apply(args.get(from + i)));
            }
            for (int i = from; i < to; i++) {
                result.set(i, rs.get(i - from));
            }
            synchronized (list) {
                list.add(integer);
                list.notify();
            }
            return false;
        };
        synchronized (deque) {
            for (int i = 0; i < size; i++) {
                deque.add(i);
                functions.add(fun);
            }
            deque.notifyAll();
        }
        synchronized (list) {
            while (list.size() != size) {
                list.wait();
            }
        }
        return result;
    }

    /**
     * Closing all <tt>threads</tt>
     */
    @Override
    public void close() {
        for (Thread thread : threads) {
            thread.interrupt();
        }
    }
}
