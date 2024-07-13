package model;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Model of the application. Interacts with view controller and the client socket thread.
 */
public class Box {

    /*Observable list that contain the received and sent mails of the user*/
    private ObservableList<Mail> in;
    private ObservableList<Mail> out;

    private String username=null;
    private int id=-1;

    /*R and W lock used to control the access to received mails*/
    private ReadWriteLock inRwl = new ReentrantReadWriteLock();
    private Lock inRl = inRwl.readLock();
    private Lock inWl = inRwl.writeLock();

    /*R and W lock used to control the access to sent mails*/
    private ReadWriteLock outRwl = new ReentrantReadWriteLock();
    private Lock outRl = outRwl.readLock();
    private Lock outWl = outRwl.writeLock();

    /*Property used to signal that the box is no longer up to date with the one on the server*/
    private BooleanProperty upToDate=new SimpleBooleanProperty();
    /*String property that contains the text of an error if it occurs*/
    private StringProperty errorMessage=new SimpleStringProperty();


    public Box(String username){
        in= FXCollections.observableArrayList();
        out= FXCollections.observableArrayList();
        this.username=username;
    }

    /**
     * @return an array containing inbox mails
     */
     public ObservableList<Mail> getInMails(){
        inRl.lock();
        ObservableList<Mail> falseIn= FXCollections.observableArrayList();
        for (Mail m:in
             ) {
            falseIn.add(m);
        }
        inRl.unlock();
        return falseIn;
    }

    /**
     * @return an array containing sent mails
     */
    public ObservableList<Mail> getOutMails(){
        outRl.lock();
        ObservableList<Mail> falseOut= FXCollections.observableArrayList();
        for (Mail m:out
        ) {
            falseOut.add(m);
        }
        outRl.unlock();
        return falseOut;
    }

    /**Return upToDate property*/
    public BooleanProperty getUpToDate(){
        return upToDate;
    }

    /**
     * @param mail to be removed
     * @return true if a mail has been removed, false otherwise
     */
    public boolean removeMail(Mail mail){
        inWl.lock();
        boolean inBool = in.remove(mail);
        inWl.unlock();
        outWl.lock();
        boolean outBool = out.remove(mail);
        outWl.unlock();
        return inBool || outBool;
    }

    /**
     * @return username associated with the box
     */
    public String getUsername(){
        return username;
    }

    /**
     * @param mail to be added
     * @param sent true if the mail will be added to out array, false if the mail will be added to in array
     * If the mail is sent a false id will be assigned to the mail, for being then changed once the server acknowledge
     * the mail.
     */
    public void addMail(Mail mail,boolean sent){
        if(sent){
            outWl.lock();
            if(mail.getId()==-1)mail.setId(id);
            id--;
            out.add(mail);
            outWl.unlock();
        }else{
            inWl.lock();
            in.add(mail);
            inWl.unlock();
        }

    }

    /**
     * @param oldId old id of the mail
     * @param newId new id to assigned to mail
     */
    synchronized public void setIdToMail(int oldId,int newId){
        for (Mail m:out
             ) {
            if(m.getId()==oldId)m.setId(newId);
        }
    }

    /**
     * @return StringProperty error message
     */
    public StringProperty getErrorMessage(){
        return errorMessage;
    }

    public void resetBox(){
        inWl.lock();
        in.clear();
        inWl.unlock();
        outWl.lock();
        out.clear();
        outWl.unlock();
        id=0;
    }

    synchronized public void addInInvalidationListener(InvalidationListener il){
        in.addListener(il);
    }

    synchronized public void addOutInvalidationListener(InvalidationListener il){
        out.addListener(il);
    }

    synchronized public void addInChangeListener(ListChangeListener<Mail> lcl){
        in.addListener(lcl);
    }

    synchronized public void addOutChangeListener(ListChangeListener<Mail> lcl){
        out.addListener(lcl);
    }

    synchronized public void removeInChangeListener(ListChangeListener<Mail> lcl){
        in.removeListener(lcl);
    }

    synchronized public void removeOutChangeListener(ListChangeListener<Mail> lcl){
        out.removeListener(lcl);
    }


}