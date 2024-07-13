module com.example.servermail {
    requires javafx.controls;
    requires javafx.fxml;

    opens controller to javafx.fxml;
    exports controller;
    opens main to javafx.fxml;
    exports main;
    exports model;
    opens model to javafx.fxml;
    exports packet;
    opens packet to javafx.fxml;
    exports exceptions;
    opens exceptions to javafx.fxml;
}