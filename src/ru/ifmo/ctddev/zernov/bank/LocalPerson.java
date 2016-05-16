package ru.ifmo.ctddev.zernov.bank;


import java.io.Serializable;
import java.rmi.RemoteException;

public class LocalPerson implements Person, Serializable {
    private String name, surname;
    int passId;

    public LocalPerson (String name, String surname, int id){
        this.name = name;
        this.surname = surname;
        this.passId = id;
    }

    public LocalPerson (Person p) throws RemoteException{
       this (p.getName(), p.getSurname(), p.getPassportId());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSurname() {
        return surname;
    }

    @Override
    public int getPassportId() {
        return passId;
    }
}
