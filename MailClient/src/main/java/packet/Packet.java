package packet;

public interface Packet<T>{

    String type = null;

    String getType();

    T getContent();

}