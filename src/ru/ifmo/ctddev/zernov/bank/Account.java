package ru.ifmo.ctddev.zernov.bank;

import java.rmi.*;

public interface Account extends Remote {
    int getId()
        throws RemoteException;

    int getAmount()
        throws RemoteException;

    void setAmount(int amount)
        throws RemoteException;
}