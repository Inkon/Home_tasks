package ru.ifmo.ctddev.zernov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Person extends Remote{
    String getName() throws RemoteException;

    String getSurname() throws RemoteException;

    int getPassportId() throws RemoteException;
}
