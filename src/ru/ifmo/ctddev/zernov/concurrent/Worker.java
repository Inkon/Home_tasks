package ru.ifmo.ctddev.zernov.concurrent;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Multithreading worker with <tt>monoid</tt> which constructed from given list of <tt>values</tt>
 * according to <tt>function</tt> and will return element group with only this element if it satisfying
 * the <tt>terminate</tt> predicate (it can be not the first
 * element it list which satisfying predicate). Worker works with amount of <tt>threads</tt>.
 * Result element group can be
 * received by invoke {@link #calculate()} method
 * @see Monoid
 * @see Thread
 * @param <T> type of given list's elements
 * @param <R> second type of {@link Monoid} structure
 * @param <C> first type of {@link Monoid} structure
 */
public class Worker<T, R, C> {
    private Monoid<C, R> monoid;
    private Function<? super T, ? extends C> function;
    private  Predicate<? super C>   terminate;
    private List<? extends T> values;
    private R result;
    private int threads, counter;
    private boolean terminated = false;

    /**
     * Constructs worker and specify it works by
     * @param threads amount of {@link Thread threads} worker will use
     * @param values list of given elements
     * @param function function which describe the rule to convert <tt>T</tt>-type element from the list to
     *                 <tt>C</tt>-type element
     * @param monoid class of element group
     * @param terminate condition of ending calculating
     */
    public Worker(int threads, List<? extends T> values, Function<? super T, ? extends C> function, Monoid<C, R> monoid, Predicate<? super C> terminate) {
        this.threads = values.size() >= threads * 2 ? threads : Math.max(1, values.size() / 2);
        this.values = values;
        this.function = function;
        this.monoid = monoid;
        this.terminate = terminate;
        result = monoid.getZero();
    }

    /**
     * Return the result of element group obtained by Worker's parameters.
     * @return the result of element group obtained by Worker's parameters
     * @throws InterruptedException if method was interrupted during it's invoke
     */
    public R calculate() throws InterruptedException {
        counter = 0;
        Thread[] threadArray = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            threadArray[i] = new Thread(new calculatorRunner(i));
            threadArray[i].start();
        }
        synchronized (this) {
            while (counter != threads && !terminated) {
                wait();
            }
        }
        if (terminated){
            for (int i = 0; i < threads; i++){
                if (!threadArray[i].isInterrupted()){
                    threadArray[i].interrupt();
                }
            }
        }
        return result;
    }

    private void blockCalculate(int from, int to) throws InterruptedException {
        if (from >= values.size()) {
            return;
        }
        R val = monoid.getZero();
        for (int i = from; i < to; i++) {
            C elem = function.apply(values.get(i));
            if (terminate.test(elem)){
                synchronized (this) {
                    terminated = true;
                    result = monoid.insert(monoid.getZero(), elem);
                    notifyAll();
                }
                return;
            }
            val = monoid.insert(val, elem);
        }
        synchronized (this) {
            while (counter != from / (values.size() / threads)) {
                wait();
            }
            result = monoid.divide(result, val);
            counter++;
            notifyAll();
        }
    }

    private class calculatorRunner implements Runnable {
        int cur;

        private calculatorRunner(int a) {
            cur = a;
        }

        @Override
        public void run() {
            try {
                blockCalculate(cur * (values.size() / threads), cur == threads - 1 ? values.size() :
                        (cur + 1) * (values.size() / threads));
            } catch (InterruptedException ignored) {

            }
        }
    }
}
