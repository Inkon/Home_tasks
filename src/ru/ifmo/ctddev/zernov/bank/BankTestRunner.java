package ru.ifmo.ctddev.zernov.bank;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class BankTestRunner {
    public static void main(String[] args) {
        Result result = JUnitCore.runClasses(BankTests.class);
        for (Failure failure : result.getFailures()) {
            System.err.println(failure.toString());
        }
        if (result.getFailureCount() == 0){
            System.err.println("====================================");
            System.out.println("Tests cleared");
        }
    }
}
