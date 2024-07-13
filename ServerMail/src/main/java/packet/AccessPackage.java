package packet;

import java.io.Serializable;

public class AccessPackage implements Packet<String>, Serializable {

    String type="ACCESS";
    private String message=null;

    public AccessPackage(String message) {
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