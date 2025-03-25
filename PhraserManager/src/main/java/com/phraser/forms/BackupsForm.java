package com.phraser.forms;

import com.phraser.serial.SerialCommunication;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

public class BackupsForm extends AnchorPane {
    final static Logger LOGGER = LoggerFactory.getLogger(BackupsForm.class);

    @FXML @Nullable TextField backupFileTextField;
    @FXML @Nullable TextField blockCountTextField;
    @FXML @Nullable TextField restoreFileTextField;
    @FXML @Nullable TextField serialPortTextField;
    @FXML @Nullable TextArea logsTextArea;
    @FXML @Nullable Button startSequenceButton;

    private @Nullable Stage stage;

    public BackupsForm() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("BackupsForm.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        checkNotNull(blockCountTextField).setTextFormatter(new TextFormatter<>(change -> {
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

    public void saveBackupTo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Phraser Database files (*.phr)", "*.phr"));
        fileChooser.setTitle("Backup Database");
        File saveFile = fileChooser.showSaveDialog(checkNotNull(stage));
        if (saveFile == null) { return; }

        if (!saveFile.getName().endsWith(".phr")) {
            saveFile = new File(saveFile.getPath()  + ".phr");
        }

        checkNotNull(backupFileTextField).textProperty().set(saveFile.getPath());
    }

    public void restoreFrom() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Phraser Database files (*.phr)", "*.phr"));
        fileChooser.setTitle("Restore Database");
        File dbFile = fileChooser.showOpenDialog(checkNotNull(stage));
        if (dbFile == null) { return; }

        checkNotNull(restoreFileTextField).textProperty().set(dbFile.getPath());
    }

    public void startSequence() {
        // Start Backup/Restore sequence, connect to logs
        String comPort = checkNotNull(serialPortTextField).textProperty().get();

        String backupFilePath = checkNotNull(backupFileTextField).textProperty().get();
        File saveDbFile = null;
        if (!StringUtils.isBlank(backupFilePath)) {
            saveDbFile = new File(backupFilePath);
        }

        File loadDbFile = null;
        String restoreFilePath = checkNotNull(restoreFileTextField).textProperty().get();
        if (!StringUtils.isBlank(restoreFilePath)) {
            loadDbFile = new File(restoreFilePath);
        }

        Integer blockCount = null;
        String blockCountText = checkNotNull(blockCountTextField).textProperty().get();
        if (!StringUtils.isBlank(blockCountText)) {
            blockCount = Integer.parseInt(blockCountText);
        }

        checkNotNull(startSequenceButton).disableProperty().set(true);
        Consumer<String> logger = s -> Platform.runLater(() -> addLog(s));

        File saveDbFile_ = saveDbFile;
        File loadDbFile_ = loadDbFile;
        Integer blockCount_ = blockCount;

        new Thread(() -> {
            try {
                SerialCommunication.runSequence(comPort, saveDbFile_, loadDbFile_, blockCount_, logger);
                Platform.runLater(() -> checkNotNull(startSequenceButton).disableProperty().set(false));
            } catch (Exception e) {
                addLog(e.toString());
                Platform.runLater(() -> checkNotNull(startSequenceButton).disableProperty().set(false));
            }
        }).start();
    }

    int lineCount = 1;
    void addLog(String s) {
        String text = checkNotNull(logsTextArea).textProperty().get();
        if (StringUtils.isBlank(text)) {
            lineCount = 1;
            logsTextArea.textProperty().set(lineCount++ + " " + s);
        } else {
            logsTextArea.textProperty().set(lineCount++ + " " + s + "\n" + text);
        }
        logsTextArea.setScrollTop(0);
    }

    public void clearLogs() {
        checkNotNull(logsTextArea).textProperty().set("");
    }
}
