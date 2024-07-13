package controller;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import model.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MailController{

    /*Variables used to contain data passed by launcher*/
    private Box box=null;
    private Stage stage=null;
    private List<Scene> scenes=null;


    @FXML
    private Label mailViewerMail;

    @FXML
    private Label mailViewerTitle;

    @FXML
    private Label mailViewerSender;

    @FXML
    private Label mailViewerReceivers;

    @FXML
    private TextArea mailViewerDescription;

    @FXML
    private Button mailViewerReply;

    @FXML
    private Button mailViewerReplyAll;

    private Mail mail=null;

    private ReplyController rc=null;

    /**
     * @param box model of the application
     * @param scenes of every load view
     * @param stage principal stage where the scene are shown
     * @param rc controller of reply view
     * */
    public void transferData(Box box,List<Scene> scenes,Stage stage,ReplyController rc){

        this.box=box;
        this.scenes=scenes;
        this.stage=stage;
        this.rc=rc;

        box.getUpToDate().addListener(o->resetFields());
        mailViewerMail.setText(box.getUsername());
    }


    /**Method invoked by box when a mail is selected,
    * this mail is passed by parameter and charged to the gui */
    public void setMail(Mail mail){
        /*Charging all the mail information to the gui*/
        this.mail=mail;
        mailViewerDescription.setText(mail.getText());
        mailViewerSender.setText(mail.getSender());
        String receivers=null;
        List<String> mailReceivers = mail.getReceivers();
        for (int i = 0; i < mailReceivers.size(); i++) {
            String r = mailReceivers.get(i);
            if(i==0)receivers=r;
            if(i>0)receivers = receivers + " ; " + r;
        }
        mailViewerReceivers.setText(receivers);
        mailViewerTitle.setText(mail.getTitle());
        /*Checking if the reply and reply-all should be disabled*/
        mailViewerReply.setDisable(false);
        mailViewerReplyAll.setDisable(false);
        if(mail.getSender().equals(box.getUsername()))mailViewerReply.setDisable(true);
        if(mail.getSender().equals(box.getUsername()) &&
        mail.getReceivers().stream().filter(o->!o.equals(box.getUsername())).collect(Collectors.toList()).size()==0){
            System.out.println(mail.getReceivers());
            System.out.println(mail.getSender());
            mailViewerReplyAll.setDisable(true);
        }
    }

    /**Move to box view*/
    @FXML
    public void onMoveBack(ActionEvent ev){
        stage.setScene(scenes.get(0));
        resetFields();
    }

    /**Delete a mail of the box model and then move back to box view*/
    @FXML
    private void onRemove(ActionEvent ev){
        if(box!=null && mail!=null && mail.getId()>=0){
            box.removeMail(mail);
        }
        onMoveBack(ev);
    }

    /**Move to reply view, setting reply mode*/
    @FXML
    public void onReply(ActionEvent actionEvent) {
        rc.setReply(mail);
        stage.setScene(scenes.get(3));
    }


    /**Move to reply view, setting reply-all mode*/
    @FXML
    public void onReplyAll(ActionEvent actionEvent) {
        rc.setReplyAll(mail);
        stage.setScene(scenes.get(3));
    }

    /**Move to reply view, setting forward mode*/
    @FXML
    public void onForward(ActionEvent actionEvent) {
        rc.setForward(mail);
        stage.setScene(scenes.get(3));
    }

    /**Clear all the fields*/
    private void resetFields() {
        mailViewerDescription.setText("");
        mailViewerSender.setText("");
        mailViewerReceivers.setText("");
        mailViewerTitle.setText("");
    }


}
