package controller;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import model.Box;
import model.Mail;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ErrorController {


    private Box box=null;
    private Stage stage=null;
    private List<Scene> scenes=null;

    @FXML
    private Label errorLabel;


    /**
     * Store data passed by
     * @param box model of the application
     * @param scenes of every load view
     * @param stage principal stage where the scene are shown
     * */
    public void transferData(Box box,List<Scene> scenes,Stage stage){

        this.box=box;
        this.scenes=scenes;
        this.stage=stage;

        /*Add listener to Error Message and Up To Date*/
        box.getUpToDate().addListener(o->unreachableServer());
        box.getErrorMessage().addListener(o->unreachableServer());

        /*Bind error message*/
        errorLabel.textProperty().bind(box.getErrorMessage());

        /*check status at default*/
        unreachableServer();

    }

    /*Move to error view when an error occurs*/
    synchronized void unreachableServer() {
        System.out.println(box.getUpToDate().get());
        if(box.getUpToDate().get()) {
            stage.setScene(scenes.get(0));
        }else{
            stage.setScene(scenes.get(4));
        }
    }
}
