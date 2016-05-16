package ru.ifmo.ctddev.zernov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

public class RemotePerson implements Person, Remote {
    private String name, surname;
    private int passport;

    public RemotePerson(String name, String surname, int passport){
        this.name = name;
        this.surname = surname;
        this.passport = passport;
    }

    @Override
    public String getName() throws RemoteException{
        return name;
    }

    @Override
    public String getSurname() {
        return surname;
    }

    @Override
    public int getPassportId() {
        return passport;
    }
}
