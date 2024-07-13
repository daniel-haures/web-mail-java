package model;

import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

/**
 * Log view model*/
public class LogBox {

    /*ObservableList String containing all the logs of the application*/
    ObservableList<String> logs= FXCollections.observableArrayList();

    /*Add a log into the list*/
    synchronized public void addLog(String log){
        System.out.println("Adding");
        logs.add(log);
        System.out.println(logs);
    }

    /*Return the observable list*/
    synchronized public ObservableList<String> getLogs(){
        ObservableList<String> falseLogs= FXCollections.observableArrayList();
        for (int i = logs.size()-1; i >=0; i--) {
            falseLogs.add(logs.get(i));
        }
        return falseLogs;
    }

    synchronized public void addInvalidationListener(InvalidationListener il){
        logs.addListener(il);
    }

}
