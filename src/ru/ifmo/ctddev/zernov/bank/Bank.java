package ru.ifmo.ctddev.zernov.bank;

import java.rmi.*;
import java.util.List;
import java.util.Map;

public interface Bank extends Remote {
    Account createAccount(int passportId, int accountId)
        throws RemoteException;
    Account getAccount(int passportId, int id)
        throws RemoteException;
    Person addPerson(String name, String surname, int passportId)
            throws RemoteException;
    LocalPerson getLocalPerson (int id)
            throws RemoteException;
    void addLocalPerson(LocalPerson person)
            throws RemoteException;
    Person getPerson(int id) throws RemoteException;
}
