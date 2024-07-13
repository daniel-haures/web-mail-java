package packet;

import model.Mail;

import java.io.Serializable;
import java.util.List;

public class InMailsPackage implements Packet<List<Mail>>, Serializable {

    String type="IN_MAILS";
    private List<Mail> mails=null;

    public InMailsPackage(List<Mail> mails) {
        this.mails = mails;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public List<Mail> getContent() {
        return mails;
    }

}

