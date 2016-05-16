package ru.ifmo.ctddev.zernov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class HelloUDPClient implements HelloClient {
    private String prefix;
    private int port, requests;
    private final static int TIMEOUT = 100;
    private DatagramSocket clientSocket;
    private InetAddress inetAddress;
    private CountDownLatch latch;
    private ConcurrentMap<Integer, String> map = new ConcurrentHashMap<>();
    private ReentrantLock lock = new ReentrantLock();
    private Condition[] conditions;
    private final static Charset CHARSET = StandardCharsets.UTF_8;

    @Override
    public void start(String host, int port, String prefix, int requests, int threads) {
        this.prefix = prefix;
        this.port = port;
        this.requests = requests;
        latch = new CountDownLatch(threads);
        conditions = new Condition[threads];
        for (int i = 0; i < threads; i++){
            conditions[i] = lock.newCondition();
        }
        try {
            clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(TIMEOUT);
            inetAddress = InetAddress.getByName(host);
        } catch (IOException e) {
            e.printStackTrace();
            clientSocket.close();
            return;
        }
        for (int i = 1; i < threads; i++) {
            new Thread(new Receiver(i)).start();
        }
        new Receiver(0).receive();
        try{
            while (latch.getCount() != 0){
                latch.await();
            }
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        clientSocket.close();
    }

    private class Receiver implements Runnable {
        int num;

        private Receiver(int num) {
            this.num = num;
        }

        @Override
        public void run() {
            receive();
        }

        private void receive() {
            for (int j = 0; j < requests; j++) {

                String sentString = prefix + num + "_" + j;
                try {
                    byte[] receiveData = new byte[clientSocket.getReceiveBufferSize()];
                    DatagramPacket receive = new DatagramPacket(receiveData, receiveData.length);
                    byte[] sendData = sentString.getBytes();
                    DatagramPacket sending = new DatagramPacket(sendData, sendData.length, inetAddress, port);
                    makeRequest(sentString,receive, sending);
                } catch (IOException e){
                    j--;
                    //retry sending
                }
            }
            latch.countDown();
        }

        private void makeRequest(String sentString, DatagramPacket receive, DatagramPacket sending) {
            try {
                sendRequest(sending);
                receiveData(receive);
                String receivedString = null;
                if (map.containsKey(num)){
                    receivedString = map.remove(num);
                } else {
                    lock.lock();
                    try{
                        conditions[num].await(TIMEOUT, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ignored) {
                        //send one more request instead of waiting
                    } finally {
                        lock.unlock();
                    }
                }
                if (map.containsKey(num)){
                    receivedString = map.remove(num);
                }
                if (receivedString == null || !receivedString.equals("Hello, " + sentString)) {
                    makeRequest(sentString, receive, sending);
                }
            } catch (IOException e){
                makeRequest(sentString, receive, sending);
                //can't send ot receive data using current port, retrying
            }
        }

        private void receiveData(DatagramPacket receive) throws IOException{

            try {
                clientSocket.receive(receive);
                String received = new String(receive.getData(), receive.getOffset(), receive.getLength(), CHARSET);
                int from = -1;
                int to = -1;
                for (int i = 0; i < received.length(); i++){
                    if (received.charAt(i) >= '0' && received.charAt(i) <= '9'){
                        if (from == -1) {
                            from = i;
                        }
                    } else if (from != -1){
                        to = received.charAt(i) == '_' ? i : -1;
                        break;
                    }
                }
                if (to != -1) {
                    int receivedNumber = Integer.parseInt(received.substring(from ,to));
                    map.putIfAbsent(receivedNumber,received);
                    map.replace(receivedNumber, received);
                    lock.lock();
                    conditions[receivedNumber].signal();
                    lock.unlock();
                }
            } catch (SocketTimeoutException ignored) {
                //retry
            }
        }

        private void sendRequest(DatagramPacket sending) throws IOException {

            clientSocket.send(sending);
        }
    }
}