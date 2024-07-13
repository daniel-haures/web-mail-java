package model;


import javafx.application.Platform;

import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataBase {

    /*Hashmap that link mail addresses to respective box*/
    private HashMap<String,Box> boxes=new HashMap<>();

    /*Set of mail already taken in charge by a thread*/
    private Set<String> inUse=new HashSet<>();

    /*A file where are saved the registered mails*/
    private File mailIndex=null;

    /*R and W lock used to regulate the access mailIndex and Hashmap*/
    private ReadWriteLock rwl = new ReentrantReadWriteLock();
    private Lock boxesRl = rwl.readLock();
    private Lock boxesWl = rwl.writeLock();

    /*Log model*/
    private LogBox logs=null;

    /*Semaphore used to control the access to inUse set*/
    private Semaphore acquireBox=new Semaphore(1);

    public DataBase(LogBox logs) throws IOException {
        /*Create or open the mailIndex*/
        mailIndex=new File("./data/"+"mailindex");
        mailIndex.getParentFile().mkdirs();
        mailIndex.createNewFile();
        this.logs=logs;
    }

    /**
     * Load all the existing boxes and their data
     * @throws IOException if the reading of the file failed
     * @throws ClassNotFoundException if a read object doesn't fit in any existing class
     */
    public void chargeData() throws IOException, ClassNotFoundException {
        boxesWl.lock();
        try {
            BufferedReader br = new BufferedReader(new FileReader(mailIndex));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                File outFile = new File("./data/OUT" + line);
                File inFile = new File("./data/IN" + line);
                outFile.createNewFile();
                inFile.createNewFile();
                Box box = new Box(line);
                box.upload();
                boxes.put(line, box);
            }
            br.close();
            Platform.runLater(()->logs.addLog("SUCCESS: Users mails loaded"));
        }finally {
            boxesWl.unlock();
        }
    }

    /**
     * Allow a thread to take in charge a box blocking other connection atomically
     * @param user associated of the box
     * @return true if the box is available, false otherwise
     */
    public boolean takeInChargeBox(String user){
        boolean bool;
        try {
            acquireBox.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(!inUse.contains(user)){
            inUse.add(user);
            bool=true;
        }else{
            bool=false;
        }
        acquireBox.release();
        return bool;
    }

    /**
     * Signal that the box of user passed as parameter is now available
     * @param user associated with the box
     */
    public void releaseBox(String user) throws InterruptedException {
        acquireBox.acquire();
        inUse.remove(user);
        acquireBox.release();

    }

    /**
     * Return the box of a certain username
     * @param mail of the user whom box is requested
     * @return box of the user if exists, null otherwise
     */
    public Box findUserBox(String mail){
        boxesRl.lock();
        Box box= boxes.get(mail);
        boxesRl.unlock();
        return box;
    }

    /**
     * @param sender of the mail
     * @param receivers of the mail
     * @param mail to be sent
     * @return the new ID of the mail sent by sender, as positive number if the receivers exists, negative otherwise
     * @throws IOException
     */
    public int sendMail(String sender, List<String> receivers, Mail mail) throws IOException {
        boxesRl.lock();
        int newId=0;
        try{
            Box senderBox= boxes.get(sender);
            newId=senderBox.addOutMail(mail);
            boolean check=true;
            for (String receiver:receivers
            ) {
                if(!boxes.containsKey(receiver))check=false;
            }
            if(check) {
                for (String receiver : receivers
                ) {
                    Box receiverBox = boxes.get(receiver);
                    receiverBox.addInMail(mail);
                }
                Platform.runLater(()->logs.addLog("SUCCESS: "+mail.getSender()+" sent a massage to "+mail.getReceivers()));
            }else{
                List<String> str=new ArrayList<>();
                str.add(sender);
                senderBox.addInMail(new Mail(0,"no-reply@system.it",str,"Mail failure: "+mail.getTitle(),"One of the mail that has been entered doesn't exists"));
                Platform.runLater(()->logs.addLog("FAILURE: "+mail.getSender()+" sent a message to a user who doesn't exists"));
            }
        }finally {
            boxesRl.unlock();
        }
        return newId;
    }
}
