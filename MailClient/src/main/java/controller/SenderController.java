package controller;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Box;
import model.Mail;

import java.net.URL;
import java.nio.channels.Pipe;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SenderController{

    /*Variables used to contain data passed by launcher*/
    Box box=null;
    List<Scene> scenes=null;
    Stage stage=null;

    @FXML
    private Label senderMail;

    @FXML
    private TextArea senderText;

    @FXML
    private TextField senderTitle;

    @FXML
    private TextField senderTarget;

    @FXML
    private Button senderSend;

    @FXML
    private Button senderBack;

    @FXML
    private Label senderToError;

    @FXML
    private Label senderTitleError;

    /**
     * Store data passed by launcher
     * @param box model of the application
     * @param scenes of every load view
     * @param stage principal stage where the scene are shown
     */
    public void transferData(Box box, List<Scene> scenes, Stage stage) {
        this.box = box;
        this.scenes = scenes;
        this.stage = stage;
        senderMail.setText(box.getUsername());
        box.getUpToDate().addListener(o->unreachableServer());
    }

    @FXML
    public void onMoveBack(ActionEvent ev){
        resetFields();
        stage.setScene(scenes.get(0));
    }

    /**
     * Check for errors in typing and then build a mail and add it to box model out mails.
     */
    @FXML
    public void onSend(ActionEvent actionEvent) {

        senderToError.setVisible(false);
        senderTitleError.setVisible(false);

        boolean check=true;
        List<String> receivers = Arrays.asList(senderTarget.getText().split(";"));
        for (int i = 0; i < receivers.size(); i++) {
            receivers.set(i,receivers.get(i).toLowerCase(Locale.ROOT).replaceAll(" ",""));
            if(!checkMailFormat(receivers.get(i))){
                senderToError.setText("Wrong mail format (use ';' to separate)");
                senderToError.setVisible(true);
                check=false;
            }
        }
        if(senderTarget.getText().equals("")){
            senderToError.setText("Empty");
            senderToError.setVisible(true);
            check=false;
        }
        if(senderTitle.getText().equals("")){
            senderTitleError.setText("Empty");
            senderTitleError.setVisible(true);
            check=false;
        }
        for (int i = 0; i < receivers.size(); i++) {
            for (int j = 0; j < receivers.size(); j++) {
                if(i!=j){
                    if(receivers.get(i).equals(receivers.get(j))){
                        senderToError.setText("Duplicated mail");
                        senderToError.setVisible(true);
                        check=false;
                    }
                }
            }
        }

        if(check){
            Mail mail=new Mail(-1, senderMail.getText(),receivers,senderTitle.getText(),senderText.getText());
            box.addMail(mail,true);
            resetFields();
            stage.setScene(scenes.get(0));
        }
    }

    /**
     * @param mail to check
     * @return true if mail is correctly formed, false otherwise.
     */
    private boolean checkMailFormat(String mail) {
        Pattern pattern = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
        Matcher mat = pattern.matcher(mail);
        if(mat.matches()){
            return true;
        }else{
            return false;
        }
    }

    /**Reset field when an error occurs*/
    private void unreachableServer(){
        resetFields();
    }

    /**Clear all the fields*/
    private void resetFields() {
        senderText.setText("");
        senderTitle.setText("");
        senderTarget.setText("");
        senderToError.setVisible(false);
        senderTitleError.setVisible(false);
    }

}
