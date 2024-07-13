package main;

import exceptions.AlreadyConnectException;
import exceptions.NoStoredMailsException;
import exceptions.WrongAccessException;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Alert;
import model.Box;
import model.Mail;
import packet.*;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;


public class ThreadClientSocket extends Thread{

    private Box box=null;
    private Socket socket=null;

    /*Server address*/
    private String ipAddress=null;
    private int port;

    /*Property used to signal that the user connected,acknowledged and
    * up to date*/
    private BooleanProperty connected=new SimpleBooleanProperty();

    /*Used to write object in the socket stream*/
    private ObjectOutputStream outStream = null;
    private ObjectInputStream inStream = null;

    /*Semaphore used to control the access to socket output stream*/
    private Semaphore write=new Semaphore(1);

    /*List change listener for outbox mail send*/
    private ListChangeListener<Mail> lclSend= change->checkMailsToSend((ListChangeListener.Change<Mail>) change);
    /*List change lister for inbox and outbox remove request*/
    private ListChangeListener<Mail> lclRemove= change->checkMailsToRemove((ListChangeListener.Change<Mail>) change);

    boolean stop=true;

    public ThreadClientSocket(String ipAddress, Integer port, Box box){
        setDaemon(true);
        this.box=box;
        this.ipAddress=ipAddress;
        this.port=port;
        this.connected.set(false);
        box.getUpToDate().bind(connected);
    }

    public BooleanProperty getConnected(){
        return connected;
    }

    /**Set-up thread and then wait for new packages from server*/
    @Override
    public void run() {
        boolean wasAcquired=false;
        try {
            write.acquire();
            wasAcquired=true;
            setUpThread();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(wasAcquired)write.release();

        /*Listening for new messages from the server*/
        while(!stop) {
            try {
                Packet p= (Packet)inStream.readObject();
                Platform.runLater(()->handleReceivedPacket(p));
            } catch (Exception e){
                e.printStackTrace();
                box.removeOutChangeListener(lclSend);
                box.removeOutChangeListener(lclRemove);
                box.removeInChangeListener(lclRemove);
                setUpThread();
            }
        }

        try {
            if(socket!=null)socket.close();
            outStream.close();
            inStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Thread closing");
    }

    /**Set up the thread through six steps, catching eventual exceptions*/
    private void setUpThread() {
        Platform.runLater(()->connected.setValue(false));
        boolean success = false;
        boolean retry=true;
        while (retry) {
            try {
                /*First step: connect to server*/
                if(socket!=null)socket.close();
                socket = new Socket(ipAddress, port);
                /*Second step: set-up input-output*/
                outStream = new ObjectOutputStream(socket.getOutputStream());
                inStream = new ObjectInputStream(socket.getInputStream());
                /*Third step: log-in the user*/
                login();
                /*Fourth step: reset the old box*/
                boxReset();
                /*Fifth step: collect mails stored by the server*/
                collectStoredMails();
                /*Sixth step: set-up listeners*/
                box.addOutChangeListener(lclSend);
                box.addOutChangeListener(lclRemove);
                box.addInChangeListener(lclRemove);
                success=true;
                retry=false;
                stop=false;
            }catch (WrongAccessException e) {
                System.out.println("A wrong mail address was registered");
                Platform.runLater(()->box.getErrorMessage().setValue("A wrong mail address was registered, change mail address."));
                success=false;
                retry=false;
                stop=true;
            }catch (AlreadyConnectException e) {
                System.out.println("The user is already connected");
                Platform.runLater(()->box.getErrorMessage().setValue("The user is already connected."));
                success=false;
                retry=true;
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                stop=false;
            }catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(()->box.getErrorMessage().setValue("Unable to connect to server, please wait..."));
                success=false;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        boolean finalSuccess = success;
        Platform.runLater(()->{if(finalSuccess)connected.setValue(true);});

    }

    /**
     * Third step: log the user to the server providing his mail
     * @throws WrongAccessException if login information are wrong
     * @throws AlreadyConnectException if the user is already connected
     * @throws IOException if connection fails
     * @throws ClassNotFoundException if package is not compatible
     */
    private void login() throws WrongAccessException, IOException, ClassNotFoundException, AlreadyConnectException {
        /*Sending login information*/
        Packet p=null;
        p=new AccessPackage(box.getUsername());
        outStream.writeObject(p);
        /*Waiting for acknowledgement*/
        System.out.println("get");
        p= (Packet)inStream.readObject();
        System.out.println("not get");
        if(p.getType().equals("ACCESS")){
            String str=((AccessPackage)p).getContent();
            System.out.println(str);
            if(str.equals("FAILED_ALREADY_CNT"))throw new AlreadyConnectException();
            if(str.equals("FAILED_WRONG_USER"))throw new WrongAccessException();
        }else{
            throw new WrongAccessException();
        }
    }

    /**
     * Fourth step: reset the box in order to avoid mails duplicate in client after a failed connection
     * @throws InterruptedException if reset fails
     */
    private void boxReset() throws InterruptedException {
        final CountDownLatch doneLatch = new CountDownLatch(1);
        Platform.runLater(()->{
            box.resetBox();
            doneLatch.countDown();
        });
        doneLatch.await();
    }


    /**
     * Fifth step: collect all the old mail from the server
     * @throws NoStoredMailsException,InterruptedException if no stored mails has been provided by the server
     * @throws ClassNotFoundException if package is not compatible
     * @throws IOException if connection fails
     */
    private void collectStoredMails() throws IOException, ClassNotFoundException, NoStoredMailsException, InterruptedException {
        int count=0;
        final CountDownLatch check=new CountDownLatch(2);
        for (int i = 0; i < 2; i++) {
            final Packet p= (Packet) inStream.readObject();
            if(p!=null && (p.getType().equals("IN_MAILS")||p.getType().equals("OUT_MAILS"))){
                if(p.getType().equals("IN_MAILS"))count+=2;
                if(p.getType().equals("OUT_MAILS"))count+=5;
                Platform.runLater(()->{
                    handleReceivedPacket(p);
                    check.countDown();
                });
            }
        }
        if(count!=7){
            outStream.writeObject(new AcknowledgePackage("STORED_MAILS_ERROR"));
            throw new NoStoredMailsException();
        }
        outStream.writeObject(new AcknowledgePackage("STORED_MAILS_OK"));
        check.await();
    }

    /**Method added to the listener of sent mail array of box model,
    * in order to send the added mails to the server once they are inserted*/
    public void checkMailsToSend(ListChangeListener.Change<Mail> change) {
        boolean wasAcquired=false;
        try {
            wasAcquired=true;
            write.acquire();
            while(change.next()){
                if(change.wasAdded()){
                    for (Mail m: change.getAddedSubList()
                    ) {
                        System.out.println("Sending Mail: "+m);
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

    /**Method added to the listener of sent mails array of box model,
     * in order to send the added mails to the server through a package*/
    public void checkMailsToRemove(ListChangeListener.Change<Mail> change){
        boolean wasAcquired=false;
        try {
            write.acquire();
            wasAcquired=true;
            while(change.next()){
                if(change.wasRemoved()){
                    for(Mail m: change.getRemoved()){
                        System.out.println("Removing "+m);
                        outStream.writeObject(new RemovePackage(m.getId()));
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

    /**Method invoked if package has been received.
     * According to the packet type, different operations are performed
     */
    private void handleReceivedPacket(Packet p){
        switch(p.getType()){
            case "SINGLE_MAIL":
                Mail mail=((SingleMailPackage)p).getContent();
                System.out.println("Receiving a single mail: "+mail);
                box.addMail(mail,false);
                java.awt.Toolkit.getDefaultToolkit().beep();
                break;
            case "IN_MAILS":
                List<Mail> inMails=((InMailsPackage)p).getContent();
                System.out.println("Receiving a lot of IN mails "+inMails);
                for (Mail m:inMails) {
                    box.addMail(m,false);
                }
                break;
            case "OUT_MAILS":
                List<Mail> outMails=((OutMailsPackage)p).getContent();
                System.out.println("Receiving a lot of OUT mails "+outMails);
                for (Mail m:outMails
                ) {
                    System.out.println(m);
                    box.addMail(m,true);
                }
                break;
            case "ACK":
                String s=((AcknowledgePackage)p).getContent();
                String[] strings=s.split("/");
                if(strings[0].equals("MAIL")){
                    box.setIdToMail(Integer.parseInt(strings[1]),Integer.parseInt(strings[2]));
                }
                break;
        }

    }

}