package ru.ifmo.ctddev.zernov.concurrent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

class Test {

    public static void main(String[] args){
        Concurrent concurrent = new Concurrent();
        ArrayList<String> list = new ArrayList<>();
        Random r = new Random();
        for (int i = 0; i < 10; i++){
//            list.add(r.nextInt());
            list.add("a" + i +" ");
        }
        System.out.println(list);
//        long time = System.currentTimeMillis();
//        int max = list.get(0);
//        for (int i = 1; i < list.size(); i++){
//            max = BURN_COMPARATOR.compare(max, list.get(i)) < 0 ? list.get(i) : max;
//        }
//        System.out.println(max);
//        System.out.println(Runtime.getRuntime().availableProcessors() / 1.5);
//        System.out.println("Without concurrent " + (System.currentTimeMillis() - time));
//        time = System.currentTimeMillis();
//        try {
//            System.out.println("result is " + concurrent.maximum(1, list, BURN_COMPARATOR));
//        } catch (InterruptedException ignored){
//        }
//        System.out.println("one thread " + (System.currentTimeMillis() - time));
//
//        time = System.currentTimeMillis();
//        try {
//            System.out.println("result is " + concurrent.maximum(Runtime.getRuntime().availableProcessors() * 2, list, BURN_COMPARATOR));
//        } catch (InterruptedException ignored){
//        }
//        System.out.println("many threads " + (System.currentTimeMillis() - time));
        try{
            System.out.println(concurrent.join(11,list));
        } catch (InterruptedException ignored){

        }
    }

    public static final Comparator<Integer> BURN_COMPARATOR = (o1, o2) -> {
        int total = o1 + o2;
//        for (int i = 0; i < 50_000_000; i++) {
//            total += i;
//        }
//        if (total == o1 + o2) {
//            throw new AssertionError();
//        }
        return Integer.compare(o1, o2);
    };
}


