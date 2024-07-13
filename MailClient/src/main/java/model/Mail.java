package model;


import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Mail object with ID, sender, receivers, title, text and creation date.
 * */
public class Mail implements Serializable {

    private static final long serialVersionUID = 6529685098267757690L;

    private int id = 0;
    private String sender = null;
    private List<String> receivers= null;
    private String title = null;
    private String text = null;
    private Date date=null;

    public Mail(int id, String sender,List<String> receivers, String title, String text) {

        this.id = id;
        this.sender = sender;
        this.receivers = receivers;
        this.title = title;
        this.text = text;
        this.date=new Date();
        System.out.println(date);

    }

    public String getSender() {
        return sender;
    }

    public List<String> getReceivers() {
        return receivers;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    synchronized public void setId(int id) {
        this.id=id;
    }

    public int getId(){
        return id;
    }

    @Override
    public String toString() {
        return id + title + sender;
    }

    public Date getDate() {
        return date;
    }
}
