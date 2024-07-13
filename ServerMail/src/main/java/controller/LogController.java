package controller;


import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import model.LogBox;

/**
 * Control log view, updating items of log listView
 */
public class LogController {

    @FXML
    private ListView logList;

    public void transferData(LogBox logs){
        /*Set listView update operation when log list change*/
        logs.addInvalidationListener((o)->logList.setItems(logs.getLogs()));
        /*Set custom cell form factor*/
        logList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                if(item!=null) {
                    String[] splited = item.split(":");
                    if (splited[0].equals("FAILURE")) setStyle("-fx-text-fill: red;-fx-font-size: 16px;-fx-font-weight: bold;");
                    if (splited[0].equals("SUCCESS")) setStyle("-fx-text-fill: green;-fx-font-size: 16px;-fx-font-weight: bold;");
                    if (splited[0].equals("ALERT")) setStyle("-fx-text-fill: blue;-fx-font-size: 16px;-fx-font-weight: bold;");
                }
            }
        });
    }

}
