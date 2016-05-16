package ru.ifmo.ctddev.zernov.bank;

import java.util.*;
import java.rmi.server.*;
import java.rmi.*;

public class BankImpl implements Bank {
    private final Map<Integer, Map<Integer, Account>> accounts = new HashMap<>();
    private final Map<Integer, Person> personMap = new HashMap<>();
    private final int port;

    public BankImpl(final int port) {
        this.port = port;
    }

    @Override
    public Account createAccount(int passportId, int accountId) throws RemoteException {
        Map<Integer, Account> personAccounts = accounts.get(passportId);
        Account account = new AccountImpl(accountId);
        personAccounts.put(accountId, account);
        UnicastRemoteObject.exportObject(account, port);
        return account;
    }

    @Override
    public Account getAccount(int passportId, int id) throws RemoteException {
        if (accounts.containsKey(passportId)){
            return accounts.get(passportId).get(id);
        }
        return null;
    }

    public Person addPerson(String name, String surname, int passportId) throws RemoteException{
        if (!personMap.containsKey(passportId) || !personMap.get(passportId).getName().equals(name)
                || !personMap.get(passportId).getSurname().equals(surname)) {
            Person person = new RemotePerson(name, surname, passportId);
            personMap.put(passportId, person);
            accounts.put(passportId, new HashMap<>());
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            return personMap.get(passportId);
        }
    }

    public void addLocalPerson(LocalPerson person){
        try {
            if (!personMap.containsKey(person.getPassportId()) ||
                    !personMap.get(person.getPassportId()).getName().equals(person.getName()) ||
                    !personMap.get(person.getPassportId()).getSurname().equals(person.getSurname())) {
                personMap.put(person.getPassportId(), person);
                accounts.put(person.getPassportId(), new HashMap<>());
            }
        } catch (RemoteException ignored){}
    }

    public Person getPerson(int id){
        return personMap.get(id);
    }

    public LocalPerson getLocalPerson(int id){
        try {
            return personMap.containsKey(id) ? new LocalPerson(personMap.get(id)) : null;
        } catch (RemoteException ignored){
            return null;
        }
    }
}
