package ru.ifmo.ctddev.zernov.bank;

import java.io.*;
import java.rmi.*;
import java.net.*;

public class Client {
    private static Bank bank;
    private static final int PORT = 8889;
    private static final String HOST = "localhost";

    public static void addRemotePerson(String name, String surname, int id) throws RemoteException {
        bank.addPerson(name, surname, id);
    }

    public static Person findRemotePerson(int id) throws RemoteException {
        return bank.getPerson(id);
    }

    public static LocalPerson findLocalPerson(int id) {
        try (Socket socket = new Socket(InetAddress.getByName(HOST), PORT)) {
            try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
                oos.writeBoolean(false);
                oos.writeObject(id);
                try (ObjectInputStream o = new ObjectInputStream(socket.getInputStream())){
                    return (LocalPerson)o.readObject();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("can't sent person data");
        }
        catch (ClassNotFoundException ignored){}
        return null;
    }

    public static Account getAccount(int passportId, int id) throws RemoteException {
        Account account = bank.getAccount(passportId, id);
        if (account == null) {
            account = bank.createAccount(passportId, id);
        }
        return account;
    }

    public static void sendLocalPerson(LocalPerson person) {
        try (Socket socket = new Socket(InetAddress.getByName(HOST), PORT)) {
            try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                oos.writeBoolean(true);
                oos.writeObject(person);
                if (!reader.readLine().equals(Server.OK)) {
                    sendLocalPerson(person);
                }
            }
        } catch (IOException e) {
            System.out.println("can't sent person data");
        }
    }

    public static void makeOperation(String name, String surname, int passId, int accountId, int amount) throws RemoteException{
        Person person = findRemotePerson(passId);
        if (person == null || !person.getName().equals(name) || !person.getSurname().equals(surname)){
            addRemotePerson(name,surname,passId);
        }
        Account account = getAccount(passId, accountId);
        account.setAmount(amount);
        System.out.println(surname + " " + name + "'s money: " + account.getAmount());
    }

    public static void start() throws RemoteException{
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (NotBoundException e) {
            System.out.println("Bank is not bound");
        } catch (MalformedURLException e) {
            System.out.println("Bank URL is invalid");
        }
    }

    public static void main(String[] args) throws RemoteException {
        start();
        makeOperation(args[0], args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
    }
}
