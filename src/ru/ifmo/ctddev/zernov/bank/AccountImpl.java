package ru.ifmo.ctddev.zernov.bank;

public class AccountImpl implements Account {
    private final int id;
    private int amount;

    public AccountImpl(int id) {
        this.id = id;
        amount = 0;
    }

    public int getId() {
        return id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        System.out.println("Setting amount of money for account " + id);
        this.amount = amount;
    }
}
