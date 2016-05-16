package ru.ifmo.ctddev.zernov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private Thread[] threads = new Thread[0];
    private static final Charset CHARSET = Charset.forName("UTF-8");


    @Override
    public void start(int port, int threads) {
        this.threads = new Thread[threads];
        try {
            this.socket = new DatagramSocket(port);
            for (int i = 0; i < threads; i++){
                this.threads[i] = new Thread(new ServerRunnable());
                this.threads[i].start();
            }
        } catch (SocketException e){
            e.printStackTrace();
        }
    }

    private class ServerRunnable implements Runnable{

        @Override
        public void run() {
            while (!Thread.interrupted()){
                try {
                    byte[] buf = new byte[socket.getReceiveBufferSize()];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    byte[] ans = ("Hello, " + new String(packet.getData(), packet.getOffset(), packet.getLength(), CHARSET)).getBytes();
                    InetAddress inetAddress = packet.getAddress();
                    int port = packet.getPort();
                    DatagramPacket send = new DatagramPacket(ans, ans.length, inetAddress, port);
                    socket.send(send);
                }  catch (IOException ignored){
                    //retry receive
                }
            }
        }

    }


    @Override
    public void close() {
        socket.close();
        for (Thread thread : threads) {
            thread.interrupt();
        }
    }
}
