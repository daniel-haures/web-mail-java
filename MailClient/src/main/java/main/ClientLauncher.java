package main;

import controller.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.Box;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ClientLauncher extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        /*Registered user mails*/
        String userMail="daniel.haures@edu.unito.it";
        String userMail2="pear@pear.it";
        String userMail3="apple@apple.it";
        String userMail4="luca@luca.it";

        /*Creating an instance of the model*/
        Box box=new Box(userMail2);

        /*Launching the thread that will create a connection with the server*/
        ThreadClientSocket socketMail=new ThreadClientSocket("localhost",7222,box);
        socketMail.start();

        System.out.println("Socket launched");
        List<Scene> scenes=new ArrayList<>();

        //Loading and saving the scene that will show all the received and sent mails.
        FXMLLoader boxLoader = new FXMLLoader(ClientLauncher.class.getResource("box.fxml"));
        scenes.add(new Scene(boxLoader.load(), 900, 600));

        //Loading and saving the scene that will allow to send a mail
        FXMLLoader senderLoader = new FXMLLoader(ClientLauncher.class.getResource("sender.fxml"));
        scenes.add(new Scene(senderLoader.load(), 900, 600));

        //Loading and saving the scene that will show a single mail in all it's details.
        FXMLLoader mailLoader = new FXMLLoader(ClientLauncher.class.getResource("mail.fxml"));
        scenes.add(new Scene(mailLoader.load(), 900, 600));

        //Loading and saving the scene that will allow to send a reply to a received mail.
        FXMLLoader replyLoader = new FXMLLoader(ClientLauncher.class.getResource("reply.fxml"));
        scenes.add(new Scene(replyLoader.load(), 900, 600));

        //Loading and saving the scene that will signal to the user that an error has occurred in the connection
        FXMLLoader errorLoader = new FXMLLoader(ClientLauncher.class.getResource("error.fxml"));
        scenes.add(new Scene(errorLoader.load(), 900, 600));

        /*Transferring to the scenes controllers the required data,
        like the box instance, all the scene, the primary stage and
        eventually the controller of another scene*/
        ((BoxController)boxLoader.getController()).transferData(box,scenes,stage,mailLoader.getController());
        ((SenderController)senderLoader.getController()).transferData(box,scenes,stage);
        ((MailController)mailLoader.getController()).transferData(box,scenes,stage,replyLoader.getController());
        ((ReplyController)replyLoader.getController()).transferData(box,scenes,stage);
        ((ErrorController)errorLoader.getController()).transferData(box,scenes,stage);

        stage.setTitle("Mail client");
        System.out.println("Gui launched");
        stage.setScene(scenes.get(0));
        stage.setResizable(false);
        stage.show();
    }

    public static void mains(String[] args) {
        launch();
    }
}
