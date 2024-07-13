package controller;

import javafx.scene.control.ListCell;
import model.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Define a cell format to mail ListView
 */
public class CellController extends ListCell<Mail> {

    @FXML
    private Label sender;

    @FXML
    private Label title;

    @FXML
    private VBox box;

    private FXMLLoader mLLoader;

    public CellController() {

    }

    @Override
    protected void updateItem(Mail item, boolean empty) {
        super.updateItem(item, empty);
        if (item != null && !empty) {
            if (mLLoader == null) {
                mLLoader = new FXMLLoader(getClass().getResource("cell.fxml"));
                mLLoader.setController(this);

                try {
                    mLLoader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            sender.setText(item.getSender());
            title.setText(item.getTitle());


            setGraphic(box);
            setText(null);

        } else {
            setGraphic(null);
            setText(null);
        }
    }
}