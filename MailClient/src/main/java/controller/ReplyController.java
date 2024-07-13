package controller;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Box;
import model.Mail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReplyController{

    Box box=null;
    List<Scene> scenes=null;
    Stage stage=null;

    @FXML
    private Label replyMail;

    @FXML
    private TextField replyTitle;

    @FXML
    private TextField replyTarget;

    @FXML
    private Label replyToError;

    @FXML
    private TextArea replyOldMail;

    @FXML
    private TextArea replyMessage;

    /**
     * Store data passed by launcher and set the instruction to do when the server is unreached.
     * @param box model of the application
     * @param scenes of every load view
     * @param stage principal stage where the scene are shown
     */
    public void transferData(Box box, List<Scene> scenes, Stage stage) {
        this.box = box;
        this.scenes = scenes;
        this.stage = stage;
        box.getUpToDate().addListener(o->unreachableServer());
        replyMail.setText(box.getUsername());
    }

    /**
     * Set reply view to reply mode
     * @param m mail passed by detailed mail view
     */
    public void setReply(Mail m){
        replyTarget.setText(m.getSender());
        replyTarget.setDisable(true);
        replyOldMail.setText(m.getText());
        replyTitle.setText("RE: "+m.getTitle());
    }

    /**
     * Set reply view to reply-all mode
     * @param m mail passed by detailed mail view
     */
    public void setReplyAll(Mail m){
        List<String> rcv = effectiveReplyAllTarget(m.getSender(),m.getReceivers());
        String targetString=rcv.get(0);
        for (int i = 1; i < rcv.size(); i++) {
            targetString=targetString+";"+rcv.get(i);
        }
        replyTarget.setText(targetString);
        replyTarget.setDisable(true);
        replyOldMail.setText(m.getText());
        replyTitle.setText("RE: "+m.getTitle());
    }

    /**
     * @param sender of the mail
     * @param receivers of the mail
     * @return the users to whom the reply will be sent, the mail owner wil be excluded.
     */
    public List<String> effectiveReplyAllTarget(String sender, List<String> receivers){
        ArrayList<String> effectiveTarget= new ArrayList<>();
        if(!sender.equals(box.getUsername()))effectiveTarget.add(sender);
        for (String s:receivers
             ) {
            if(!s.equals(box.getUsername()) && !s.equals(sender)){
                effectiveTarget.add(s);
            }
        }
        return effectiveTarget;
    }

    /**
     * Set reply view to forward mode
     * @param m mail passed by detailed mail view
     */
    public void setForward(Mail m){
        replyOldMail.setText(m.getText());
        replyTitle.setText("Forward: "+m.getTitle());
    }

    /**
     * Clear fields and move back to detailed mail view
     */
    @FXML
    public void onMoveBack(ActionEvent ev){
        resetFields();
        stage.setScene(scenes.get(2));
    }

    /**
     * Check for errors in typing and then build a reply and add it to box model out mails.
     */
    @FXML
    public void onReply(ActionEvent actionEvent) {

        /*Check if the error label is already disabled*/
        replyToError.setVisible(false);

        /*Check for typing error in the text fields*/
        boolean check=true;
        ArrayList<String> receivers = new ArrayList<>(Arrays.asList(replyTarget.getText().split(";")));
        for (int i = 0; i < receivers.size(); i++) {
            receivers.set(i,receivers.get(i).toLowerCase(Locale.ROOT).replaceAll(" ",""));
            if(!checkMailFormat(receivers.get(i))){
                replyToError.setText("Wrong mail format (use ';' )");
                replyToError.setVisible(true);
                check=false;
            }
        }
        if(replyToError.getText().equals("")){
            replyToError.setText("Empty");
            replyToError.setVisible(true);
            check=false;
        }

        if(check){
            /*Build a reply mail and adding it to the outbox mails*/
            Mail mail=new Mail(-1,box.getUsername() ,
                    receivers,replyTitle.getText(),
                    replyMessage.getText()+"\n\n--------------------------------\n"+replyOldMail.getText());
            box.addMail(mail,true);

            /*Reset fields and move to box view*/
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

    /**Reset field when server is unreached*/
    private void unreachableServer(){
        resetFields();
    }

    /**Clear all the fields*/
    private void resetFields() {
        replyTitle.setText("");
        replyTarget.setText("");
        replyMessage.setText("");
        replyOldMail.setText("");
        replyTarget.setDisable(false);
        replyToError.setVisible(false);
    }


}
