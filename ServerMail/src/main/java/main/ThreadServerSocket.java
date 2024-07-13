package main;

import exceptions.AlreadyConnectException;
import exceptions.NoStoredMailsException;
import exceptions.WrongAccessException;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Alert;
import model.Box;
import model.DataBase;
import model.LogBox;
import model.Mail;
import packet.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.Semaphore;

public class ThreadServerSocket extends Thread{

    /*Socket and socket streams*/
    private Socket socket = null;
    private ObjectOutputStream outStream = null;
    private ObjectInputStream inStream = null;

    /*Model*/
    private LogBox logs=null;
    private DataBase db=null;
    private Box box=null;
    private String usermail=null;

    private boolean stop=false;

    /*Semaphore used to control the access to socket output stream*/
    private Semaphore write=new Semaphore(1);

    /*List change listener for inbox mail send*/
    private ListChangeListener<Mail> lcl= change->checkMailsToSend((ListChangeListener.Change<Mail>) change);

    public ThreadServerSocket(Socket socket, DataBase db, LogBox logBox){
        setDaemon(true);
        this.socket=socket;
        this.db=db;
        this.logs=logBox;
    }

    @Override
    public void run() {

        setUpThread();
        box.addActionListener(lcl);
        while(!stop) {
            try {
                Packet p= (Packet)inStream.readObject();
                handleReceivedPacket(p);
            } catch (Exception e){
                e.printStackTrace();
                Platform.runLater(()->logs.addLog("ALERT: the connection with "+usermail+" was closed"));
                stop=true;
            }
        }
        close();
    }

    /**
     * Set up the thread in five steps, while checking for errors
     */
    private void setUpThread() {

        boolean wasAcquired=false;
        try{
            write.acquire();
            wasAcquired=true;
            /*First step: set up IO*/
            setUpIO();
            /*Second step: get connected username*/
            usermail=waitUserData();
            /*Third step: check if it's the user fir*/
            isFirstThread(usermail);
            /*Fourth step: confirm the user acknowledge to client*/
            acknowledgeUser(usermail);
            /*Fifth step: send stored mails*/
            sendBoxMails();
            Platform.runLater(()->logs.addLog("SUCCESS: "+usermail+" connected successfully"));
        }catch(AlreadyConnectException e){
            e.printStackTrace();
            try {
                outStream.writeObject(new AccessPackage("FAILED_ALREADY_CNT"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Platform.runLater(()->logs.addLog("FAILURE: "+usermail+" is already connected"));
            stop=true;
        }catch(WrongAccessException e){
            e.printStackTrace();
            try {
                outStream.writeObject(new AccessPackage("FAILED_WRONG_USER"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Platform.runLater(()->logs.addLog("FAILURE: an unregistered user "+usermail+" tried to connect"));
            stop=true;
        }catch(InterruptedException e){
            e.printStackTrace();
            stop=true;
        }catch(Exception e){
            e.printStackTrace();
            Platform.runLater(()->logs.addLog("FAILURE: the server failed to connect with user "+usermail));
            stop=true;
        }finally {
            if(wasAcquired)write.release();
        }
    }

    /**
     * Set-up input and output stream
     * @throws IOException if the connection wasn't able to start
     */
    private void setUpIO() throws IOException {
        outStream=new ObjectOutputStream(socket.getOutputStream());
        inStream= new ObjectInputStream(socket.getInputStream());
    }

    /**
     * Wait the client to send the user data
     * @return the mail address of the user connected with the client
     * @throws WrongAccessException the received package is not an AccessPackage
     * @throws IOException if connection was interrupted
     * @throws ClassNotFoundException the received package is not recognized
     */
    private String waitUserData() throws WrongAccessException, IOException, ClassNotFoundException {
        Packet p= ((Packet)inStream.readObject());
        if(p!=null && p.getClass().getName()=="packet.AccessPackage"){
            System.out.println(((AccessPackage)p).getContent());
            return ((AccessPackage)p).getContent();
        }else{
            throw new WrongAccessException();
        }
    }

    /**
     * Check if a mail is already taken in charge by another thread
     * @param mail of the user who wants to access his data
     * @throws AlreadyConnectException if the mail is already taken in charge by another thread
     */
    private void isFirstThread(String mail) throws AlreadyConnectException {
        if(!db.takeInChargeBox(mail)){
            throw new AlreadyConnectException();
        }
    }

    /**
     * Check if the user is registered and then acknowledge it, otherwise throw an error
     * @param mail of the user asking for acknowledge
     * @throws WrongAccessException if the user is not registered on the server (mailIndex)
     * @throws IOException
     * @throws InterruptedException
     */
    private void acknowledgeUser(String mail) throws WrongAccessException, IOException, InterruptedException {
            box=db.findUserBox(mail);
            if(box==null){
                db.releaseBox(mail);
                throw new WrongAccessException();
            }else{

                outStream.writeObject(new AccessPackage("LOGGED"));
            }
    }

    /**
     * Send to client the stored mail of logged user
     * @throws NoStoredMailsException if the user didn't receive the stored mail correctly
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void sendBoxMails() throws IOException, ClassNotFoundException, NoStoredMailsException {

            List<List<Mail>> mails = box.getMailsArray();
            System.out.println(mails);
            outStream.writeObject(new InMailsPackage(mails.get(0)));
            System.out.println("Sended");
            outStream.writeObject(new OutMailsPackage(mails.get(1)));
            System.out.println("Sended");
            Packet p=(Packet)inStream.readObject();
            System.out.println(p);
            if(p!=null && p.getType().equals("ACK")){
                if(p.getContent().equals("STORED_MAILS_OK")){
                    System.out.println("TTTTT");
                }
                if(p.getContent().equals("STORED_MAILS_ERROR")){
                    System.out.println("FFFF");

                }
            }else{
                throw new NoStoredMailsException();
            }

    }

    /**
     * Method invoked when a new mail has been added to the inbox
     * @param change
     */
    public void checkMailsToSend(ListChangeListener.Change<Mail> change) {
        boolean wasAcquired=false;
        try {
            write.acquire();
            wasAcquired=true;
            while(change.next()){
                if(change.wasAdded()){
                    for (Mail m: change.getAddedSubList()
                    ) {
                        outStream.writeObject(new SingleMailPackage(m));
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(wasAcquired)write.release();
        }
    }

    /**
     * Handle the received packages
     * @param p packet to be handled
     * @throws IOException
     */
    private void handleReceivedPacket(Packet p) throws IOException, InterruptedException {
        switch(p.getType()){
            case "SINGLE_MAIL":
                Mail m=(Mail) p.getContent();
                int oldId=m.getId();
                int newId= db.sendMail(m.getSender(),m.getReceivers(),m);
                write.acquire();
                try {
                    outStream.writeObject(new AcknowledgePackage("MAIL/" + oldId + "/" + newId));
                }finally {
                    write.release();
                }
                break;
            case "REMOVE":
                Integer id=((RemovePackage)p).getContent();
                box.removeMail(id);
                Platform.runLater(()->logs.addLog("SUCCESS: the mail with id "+id+" of user "+usermail+" was removed"));
        }
    }

    private void close(){
        try {
            db.releaseBox(usermail);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            outStream.close();
            inStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        box.removeActionListener(lcl);
        if(socket!=null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
