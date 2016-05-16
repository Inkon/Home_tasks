package ru.ifmo.ctddev.zernov.bank;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.net.*;

public class Server {
    public final static String OK = "OK";
    private final static int PORT = 8888, RECEIVE_PORT = 8889, THREADS = 10;
    private static Thread[] threads = new Thread[0];
    private static ServerSocket socket;
    private static Bank bank;

    private static class ServerRunnable implements Runnable{

        @Override
        public void run() {
            while (!Thread.interrupted()){
                try {
                    Socket user = socket.accept();
                    try (ObjectInputStream ois = new ObjectInputStream(user.getInputStream())){
                        boolean bool = ois.readBoolean();
                        if (bool) {
                            try (OutputStream outputStream = user.getOutputStream()){
                                LocalPerson p = (LocalPerson) ois.readObject();
                                System.out.println("Received person with passId " + p.getPassportId());
                                bank.addLocalPerson(p);
                                outputStream.write(OK.getBytes());
                            }
                        } else {
                            try (ObjectOutputStream oos = new ObjectOutputStream(user.getOutputStream())){
                                int passId = (int)ois.readObject();
                                System.out.println("Receiver request with passId " + passId);
                                LocalPerson person = bank.getLocalPerson(passId);
                                oos.writeObject(person);
                            }
                        }
                    }  catch (ClassNotFoundException e){
                        System.out.println("Error occurred while parsing request");
                    }
                } catch (IOException e){
                    System.out.println("Error occurred while receiving request");
                }
            }
        }
    }


    public void close() {
        try {
            socket.close();
        } catch (IOException e){
            System.out.println("Error occurred closing socket");
        }
        for (Thread thread : threads) {
            thread.interrupt();
        }
    }

    public static void main(String[] args) {
        bank = new BankImpl(PORT);
        threads = new Thread[THREADS];
        try {
            socket = new ServerSocket(RECEIVE_PORT);
            for (int i = 0; i < THREADS; i++){
                threads[i] = new Thread(new ServerRunnable());
                threads[i].start();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        try {
            UnicastRemoteObject.exportObject(bank, PORT);
            Naming.rebind("//localhost/bank", bank);
        } catch (RemoteException e) {
            System.out.println("Cannot export object: " + e.getMessage());
            e.printStackTrace();
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL");
        }
        System.out.println("Server started");
    }
}
