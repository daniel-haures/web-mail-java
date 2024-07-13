package main;


import controller.LogController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.DataBase;
import model.LogBox;
import model.Mail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class ServerLauncher extends Application {

    @Override
    public void start(Stage stage){
        try {
            System.out.println(InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        /*Initialize the model*/
        LogBox logs=new LogBox();
        DataBase db= null;
        try {
            db = new DataBase(logs);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }

        /*Load data to the model*/
        boolean check=true;
        try {
            db.chargeData();
        } catch (ClassNotFoundException e) {
            Platform.runLater(()->logs.addLog("FAILURE: failed to load data from file"));
            e.printStackTrace();
            check=false;
        }catch (IOException e){
            Platform.runLater(()->logs.addLog("FAILURE: failed to load data from file"));
            e.printStackTrace();
            check=false;
        }

        /*Launch log view*/
        FXMLLoader logLoader = new FXMLLoader(ServerLauncher.class.getResource("log.fxml"));
        Scene scene= null;
        try {
            scene = new Scene(logLoader.load(),900,600);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        ((LogController)logLoader.getController()).transferData(logs);

        if(check){
            /*Launch the thread that manage the server socket*/
            ThreadLauncher th=new ThreadLauncher(db,logs);
            th.start();
        }

        stage.setTitle("Server Mail");
        System.out.println("Gui launched");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

    }

    public static void main(String[] args) {
        launch();
    }
}

