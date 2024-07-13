package packet;

import java.io.Serializable;

public class AcknowledgePackage implements Packet<String>, Serializable {

    String type="ACK";
    private String message=null;

    public AcknowledgePackage(String message) {
        this.message = message;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getContent() {
        return message;
    }

}