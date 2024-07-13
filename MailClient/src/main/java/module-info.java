module com.example.mailclient {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens main to javafx.fxml;
    exports main;

    opens controller to javafx.fxml;
    exports controller;

    opens model to javafx.fxml;
    exports model;
    exports exceptions;
    opens exceptions to javafx.fxml;

}