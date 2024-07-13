package controller;


import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import model.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BoxController{

    /*Variables used to contain data passed by launcher*/
    private Box box=null;
    private Stage stage=null;
    private List<Scene> scenes=null;
    private MailController mailController= null;

    /*Integer used to signal which category is currently showed*/
    private AtomicInteger displayedCategory=new AtomicInteger();

    /*Boolean used to signal that the removing button is activated*/
    private AtomicBoolean removing=new AtomicBoolean();

    /*ListView used to show the existing category and allow the user to choose one of them*/
    @FXML
    private ListView mailList =new ListView<>();

    /*ListView used to show the mails received or sent by the user*/
    @FXML
    private ListView categoryList=new ListView<>();

    /*Label used to display the user mail address*/
    @FXML
    private Label mail;

    /*Button that allows removing operations*/
    @FXML
    private Button boxRemove;

    /*Invalidation listeners to refresh the mail list*/
    private InvalidationListener ilIn=(o)->setMailsByCategory(0);
    private InvalidationListener ilOut=(o)->setMailsByCategory(1);

    /**
     * Store data passed by launcher and build the mail list
     * @param box model of the application
     * @param scenes of every load view
     * @param stage principal stage where the scene are shown
     * @param mailController controller of the detailed mail view
     */
    public void transferData(Box box, List<Scene> scenes, Stage stage, MailController mailController){
        this.box=box;
        this.scenes=scenes;
        this.stage=stage;
        this.mailController=mailController;

        /*Setting a cell format to the mail listView*/
        mailList.setCellFactory(mailListView->new CellController());

        /*Setting up the category list view*/
        ObservableList<String> c= FXCollections.observableArrayList();
        c.add("Inbox");
        c.add("Sent");
        categoryList.setItems(c);

        /*An invalidation listener is added to both received and sent mails of the box,
        in order to automatically update the categories when mail array have been modified*/
        box.addInInvalidationListener(ilIn);
        box.addOutInvalidationListener(ilOut);

        mail.setText(box.getUsername());

        removing.set(false);

        /*Setting the default state of */
        displayedCategory.getAndSet(0);
        setMailsByCategory(0);

    }

    /**
     * Displays a set of mail on the mail ListView,
     * according to category parameter and actual category.
     * @param category the category of mail to be displayed by the listview
     */
    synchronized private void setMailsByCategory(int category) {

        if(category==0 && displayedCategory.get()==0){
            mailList.setItems(box.getInMails().sorted((m1,m2)->m2.getDate().compareTo(m1.getDate())));
            categoryList.getSelectionModel().select(0);
        }
        if(category==1 && displayedCategory.get()==1){
            mailList.setItems(box.getOutMails().sorted((m1,m2)->m2.getDate().compareTo(m1.getDate())));
            categoryList.getSelectionModel().select(1);
        }
        System.out.println(box.getInMails());
        System.out.println(box.getOutMails());
    }


    /**Method invoked when an action occurs on the category list view.
     * If 'inbox' is selected inbox mail will be displayed
     * If 'sent' is selected sent mails will be displayed*/
    @FXML
    public void onCategoryListViewSelected(MouseEvent arg0) {
        if(categoryList.getSelectionModel().getSelectedItem()!=null) {
            if (categoryList.getSelectionModel().getSelectedItem().equals("Inbox")){
                displayedCategory.getAndSet(0);
                setMailsByCategory(0);
            }
            if (categoryList.getSelectionModel().getSelectedItem().equals("Sent")){
                displayedCategory.getAndSet(1);
                setMailsByCategory(1);
            }
        }
    }

    /**Method invoked when an action occurs on the mail list view.
    * If a mail is selected with a double click the mail will be shown in details or removed,
    * according to remove button status */
    @FXML
    public void onMailListViewSelected(MouseEvent mouseEvent) {
        if(mouseEvent.getButton().equals(MouseButton.PRIMARY)){
            if(mouseEvent.getClickCount() == 2){
                if(removing.get()){
                    Mail m=(Mail)mailList.getSelectionModel().getSelectedItem();
                    if(m!=null && m.getId()>=0)box.removeMail((Mail)mailList.getSelectionModel().getSelectedItem());
                }else{
                    Mail m=(Mail)mailList.getSelectionModel().getSelectedItem();
                    if(m!=null){
                        mailController.setMail(m);
                        stage.setScene(scenes.get(2));
                    }
                }
            }
        }
    }

    /**Method invoked when the remove button is pressed.
     * The boolean variable named 'removing' is switched.*/
    @FXML
    public void onDeleteButtonPressed(ActionEvent ec){
        if(removing.get()){
            removing.getAndSet(false);
            boxRemove.setStyle(null);
        }else{
            removing.getAndSet(true);
            boxRemove.setStyle("-fx-background-color: #ff0000;-fx-text-fill: #FFFFFF");
        }
    }

    /**Method invoked when send button is pressed
    * Move to send view*/
    @FXML
    public void onSendButtonPressed(ActionEvent ec){
        stage.setScene(scenes.get(1));
    }


}

