package main;

import model.DataBase;
import model.LogBox;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ThreadLauncher extends Thread{

    private DataBase db=null;
    private LogBox logs=null;

    public ThreadLauncher(DataBase db, LogBox logs) {
        setDaemon(true);
        this.db=db;
        this.logs=logs;
    }

    @Override
    public void run(){
        /*Server socket launching*/
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(7222);
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*Accept client socket connection requests*/
        while(true){
            Socket socket = null;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ThreadServerSocket thSocket = new ThreadServerSocket(socket, db, logs);
            thSocket.start();
        }
    }
}
