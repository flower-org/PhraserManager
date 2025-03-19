package com.phraser.forms;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class GenericNameDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(GenericNameDialog.class);

    @Nullable Stage stage;

    @Nullable @FXML TextField nameTextField;
    @Nullable @FXML Button okButton;

    @Nullable String name;

    public GenericNameDialog(String buttonText, @Nullable String textFieldValue) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("GenericNameDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        if (textFieldValue == null) {
            checkNotNull(nameTextField).textProperty().set("");
        } else {
            checkNotNull(nameTextField).textProperty().set(textFieldValue);
        }
        checkNotNull(okButton).textProperty().set(buttonText);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void okClose() {
        try {
            name = checkNotNull(nameTextField).textProperty().get();
            checkNotNull(stage).close();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "GenericNameDialog close Error: " + e, ButtonType.OK);
            LOGGER.error("GenericNameDialog close Error:", e);
            alert.showAndWait();
        }
    }

    public void closeOnEnter(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            okClose();
        }
    }
}
