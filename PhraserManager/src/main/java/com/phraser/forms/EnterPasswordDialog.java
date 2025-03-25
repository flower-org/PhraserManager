package com.phraser.forms;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.phraser.utils.Pbkdf2Tool.PBKDF2_ITERATIONS;

public class EnterPasswordDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(EnterPasswordDialog.class);

    @Nullable Stage stage;

    @Nullable String password;
    @Nullable Integer pbkdf2IterationCount;

    @Nullable @FXML PasswordField passwordPasswordField;
    @FXML @Nullable TextField pbkdf2IterationsTextField;

    public EnterPasswordDialog() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("EnterPasswordDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        checkNotNull(pbkdf2IterationsTextField).textProperty().set(Integer.toString(PBKDF2_ITERATIONS));
        checkNotNull(pbkdf2IterationsTextField).setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.length() <= 32 && newText.matches("[0-9]*")) {
                change.setText(change.getText().toLowerCase());
                return change;
            }
            return null;
        }
        ));
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    @Nullable public Integer getPbkdf2IterationCount() { return pbkdf2IterationCount; }

    public void okClose() {
        try {
            password = checkNotNull(passwordPasswordField).textProperty().get();
            pbkdf2IterationCount = Integer.parseInt(checkNotNull(pbkdf2IterationsTextField).textProperty().get());
            checkNotNull(stage).close();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "EnterPasswordDialog close Error: " + e, ButtonType.OK);
            LOGGER.error("EnterPasswordDialog close Error:", e);
            alert.showAndWait();
        }
    }

    public void closeOnEnter(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            okClose();
        }
    }
}
