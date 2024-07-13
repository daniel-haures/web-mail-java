package packet;

import java.io.Serializable;

public class RemovePackage implements Packet<Integer>, Serializable {

    private static String type="REMOVE";
    private int id=0;

    public RemovePackage(int id){
        this.id=id;
    }

    @Override
    public Integer getContent() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }
}
