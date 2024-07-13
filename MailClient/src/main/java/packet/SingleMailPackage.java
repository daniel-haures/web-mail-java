package packet;


import model.Mail;

import java.io.Serializable;

public class SingleMailPackage implements Packet<Mail>, Serializable {

    private static String type="SINGLE_MAIL";
    private Mail mail=null;

    public SingleMailPackage(Mail mail){
        this.mail=mail;
    }

    @Override
    public Mail getContent() {
        return mail;
    }

    @Override
    public String getType() {
        return type;
    }
}
