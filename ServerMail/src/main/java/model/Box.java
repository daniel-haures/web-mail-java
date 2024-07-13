package model;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class Box {

    /*Observable list that contain the received and sent mails of the user*/
    private ObservableList<Mail> in= null;
    private ObservableList<Mail> out= null;

    private String username=null;

    /*Number identification assigned to new mail*/
    private int inId=0;
    private int outId=1;

    /*R and W lock used to control the access to received mails*/
    private ReadWriteLock inRwl = new ReentrantReadWriteLock();
    private Lock inRl = inRwl.readLock();
    private Lock inWl = inRwl.writeLock();

    /*R and W lock used to control the access to sent mails*/
    private ReadWriteLock outRwl = new ReentrantReadWriteLock();
    private Lock outRl = outRwl.readLock();
    private Lock outWl = outRwl.writeLock();

    public Box(String username){
        in= FXCollections.observableArrayList();
        out= FXCollections.observableArrayList();
        this.username=username;
    }

    /**
     * Add a mail to out mails
     * @param mail added to the out mail list
     * @return the even id assigned to the mail
     */
    public int addOutMail(Mail mail) throws IOException {
        System.out.println("Lock A");
        outWl.lock();
        try {
            Mail newMail = new Mail(outId, mail.getSender(), mail.getReceivers(), mail.getTitle(), mail.getText());
            outId += 2;
            System.out.println(newMail);
            out.add(newMail);
            System.out.println(outId);
            writeOutbox();
            System.out.println("Unlock A");
        }finally {
            outWl.unlock();
        }
        return outId-2;
    }


    /**
     * Add a mail to inbox mail
     * @param mail to be added in the in list, an odd id is assigned to the mail
     */
    public void addInMail(Mail mail) throws IOException {
        inWl.lock();
        try {
            System.out.println("Lock B");
            Mail newMail = new Mail(inId, mail.getSender(), mail.getReceivers(), mail.getTitle(), mail.getText());
            inId += 2;
            System.out.println(newMail);
            in.add(newMail);
            System.out.println(inId);
            writeInbox();
            System.out.println("Unlock B");
        }finally {
            inWl.unlock();
        }
    }

    /**
     * Return all the mails of the box
     * @return a list containing both in and out mail list
     */
    public List<List<Mail>> getMailsArray(){
        System.out.println("Lock C");
        ArrayList<List<Mail>> mails=new ArrayList<>();
        inRl.lock();
        List<Mail> inMails=in.stream().collect(Collectors.toList());
        mails.add(inMails);
        inRl.unlock();
        outRl.lock();
        List<Mail> outMails=out.stream().collect(Collectors.toList());
        mails.add(outMails);
        outRl.unlock();
        System.out.println("Unlock C");
        return mails;
    }

    /**
     * Remove mails by id
     * @param id of the mail that will be removed
     */
     public void removeMail(int id) throws IOException {
         removeMailIn(id);
         removeMailOut(id);
     }

    private void removeMailIn(int id) throws IOException {
        inWl.lock();
        try {
            Iterator<Mail> iter = in.iterator();
            while (iter.hasNext()) {
                Mail m = iter.next();
                if (m.getId() == id) {
                    iter.remove();
                }
            }
            writeInbox();
        }finally {
            inWl.unlock();
        }
    }

    private void removeMailOut(int id) throws IOException {
        Iterator<Mail> iter;
        outWl.lock();
        try{
            iter = out.iterator();
            while (iter.hasNext()){
                Mail m=iter.next();
                if(m.getId()== id){
                    iter.remove();
                }
            }
            writeOutbox();
        }finally {
            outWl.unlock();
        }
    }

    /**
     * Write inbox to a file
     */
    private void writeInbox() throws IOException {
        inWl.lock();
        try {
            ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream("./data/IN" + username));
            output.writeObject(new ArrayList<>(in));
            output.close();
        }finally {
            inWl.unlock();
        }
    }

    /**
     * Write out mails to a file
     */
    private void writeOutbox() throws IOException {
        outWl.lock();
        try{
            ObjectOutputStream output=new ObjectOutputStream(new FileOutputStream("./data/OUT"+username));
            output.writeObject(new ArrayList<>(out));
            output.close();
        }finally {
            outWl.unlock();
        }
    }

    /**
     * Upload both inbox and sent mail, taking care of updating correctly the mail index
     * @throws IOException if the input stream reader stops working
     * @throws ClassNotFoundException if a wrong mail class has found
     */
    public void upload() throws IOException, ClassNotFoundException {
        uploadIn();
        uploadOut();
    }

    private void uploadIn() throws IOException, ClassNotFoundException {
        inWl.lock();
        try {
            ObjectInputStream input = null;
            try {
                input = new ObjectInputStream(new FileInputStream("./data/IN" + username));
                in = FXCollections.observableList((List<Mail>) input.readObject());
                input.close();
            } catch (EOFException e) {
                e.printStackTrace();
            }
            Optional<Integer> resIn = in.stream().map(m -> m.getId())
                    .max(Integer::compare);
            inId = 0;
            if (resIn.isPresent()) inId = resIn.get() + 2;
        }finally {
            inWl.unlock();
        }
    }

    private void uploadOut() throws IOException, ClassNotFoundException {
        outWl.lock();
        try {
            try {
                ObjectInputStream output = new ObjectInputStream(new FileInputStream("./data/OUT" + username));
                out = FXCollections.observableList((List<Mail>) output.readObject());
                output.close();
            } catch (EOFException e) {
                e.printStackTrace();
            }
            Optional<Integer> resOut = out.stream().map(m -> m.getId()).max(Integer::compare);
            outId = 1;
            if (resOut.isPresent()) outId = resOut.get() + 2;
        }finally {
            outWl.unlock();
        }
    }

    synchronized public void addActionListener(ListChangeListener<Mail> lcl){
        in.addListener(lcl);
    }

    synchronized public void removeActionListener(ListChangeListener<Mail> lcl){
        in.removeListener(lcl);
    }
}
