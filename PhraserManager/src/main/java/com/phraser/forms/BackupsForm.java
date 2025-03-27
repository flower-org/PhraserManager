package com.phraser.forms;

import com.flower.fxutils.JavaFxUtils;
import com.phraser.serial.SerialCommunication;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
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

import static com.flower.fxutils.JavaFxUtils.YesNo.YES;
import static com.google.common.base.Preconditions.checkNotNull;

public class BackupsForm extends AnchorPane {
    final static Logger LOGGER = LoggerFactory.getLogger(BackupsForm.class);
    final static String BANK1 = "BANK1";
    final static String BANK2 = "BANK2";
    final static String BANK3 = "BANK3";

    @FXML @Nullable TextField backupFileTextField;
    @FXML @Nullable TextField blockCountTextField;
    @FXML @Nullable TextField restoreFileTextField;
    @FXML @Nullable TextField serialPortTextField;
    @FXML @Nullable TextArea logsTextArea;
    @FXML @Nullable Button startSequenceButton;
    @FXML @Nullable Button stopSequenceButton;

    @FXML @Nullable CheckBox backupBlockCountCheckBox;
    @FXML @Nullable TextField restoreBlockCountTextField;
    @FXML @Nullable CheckBox bankCheckBox;
    @FXML @Nullable ComboBox<String> bankComboBox;

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

        checkNotNull(restoreFileTextField).textProperty()
                .addListener((observableValue, s, t1) -> calculateRestoreBlockCount());
    }

    public void enableBackupBlockCountEdit() {
        if (checkNotNull(backupBlockCountCheckBox).selectedProperty().get()) {
            checkNotNull(blockCountTextField).disableProperty().set(false);
        } else {
            checkNotNull(blockCountTextField).disableProperty().set(true);
            checkNotNull(blockCountTextField).textProperty().set("128");
        }
    }

    public void enableBankSelection() {
        if (checkNotNull(bankCheckBox).selectedProperty().get()) {
            checkNotNull(bankComboBox).disableProperty().set(false);
        } else {
            checkNotNull(bankComboBox).disableProperty().set(true);
            checkNotNull(bankComboBox).getSelectionModel().select(0);
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void calculateRestoreBlockCount() {
        try {
            String filename = checkNotNull(restoreFileTextField).textProperty().get();
            File restoreFile = new File(filename);
            if (restoreFile.exists()) {
                long fileLength = restoreFile.length();
                if (fileLength % 4096 != 0) {
                    String msg = "DB File Length not divisible by 4096, likely invalid file selected. (" +
                            fileLength + ")";
                    Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
                    LOGGER.error(msg);
                    alert.showAndWait();
                }

                int restoreBlockCount = (int) (fileLength / 4096L);
                checkNotNull(restoreBlockCountTextField).textProperty().set(Integer.toString(restoreBlockCount));
            } else {
                checkNotNull(restoreBlockCountTextField).textProperty().set("-1");
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error calculating restore block count: " + e, ButtonType.OK);
            LOGGER.error("Error calculating restore block count", e);
            alert.showAndWait();
        }
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

    String buildBlockCountErrMsg(String blockCountType, int maxBlockCount, int blockCount, int bank) {
        return String.format("BANK%d can't have more than %d blocks. Supplied %s BlockCount = %d",
                bank, maxBlockCount, blockCountType, blockCount);
    }

    private boolean checkBankOverflow(int blockCount, int bank) {
        String errorMsg = null;
        switch (bank) {
            case 1:
                if (blockCount > 256) { errorMsg = "Block data will use blocks from BANK2 and BANK3. (BANK1 will work fine, but don't use BANK2 or BANK 3). Proceed?"; }
                else if (blockCount > 128) { errorMsg = "Block data will use blocks from BANK2. (BANK1 will work fine, but don't use BANK2). Proceed?"; }
                break;
            case 2:
                if (blockCount > 128) { errorMsg = "Block data will use blocks from BANK3. (BANK2 will work fine, but don't use BANK3). Proceed?"; }
                break;
        }

        if (errorMsg != null) {
            return (YES == JavaFxUtils.showYesNoDialog("Bank overflow", errorMsg));
        }
        return true;
    }

    boolean checkBlockCountAgainstBank(String blockCountType, int blockCount, int bank) {
        String errorMsg = null;
        switch (bank) {
            case 1:
                if (blockCount > 384) {
                    errorMsg = buildBlockCountErrMsg(blockCountType, 384, blockCount, bank);
                }
                break;
            case 2:
                if (blockCount > 256) {
                    errorMsg = buildBlockCountErrMsg(blockCountType, 256, blockCount, bank);
                }
                break;
            case 3:
                if (blockCount > 128) {
                    errorMsg = buildBlockCountErrMsg(blockCountType, 128, blockCount, bank);
                }
                break;
        }

        if (errorMsg != null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, errorMsg, ButtonType.OK);
            LOGGER.error(errorMsg);
            alert.showAndWait();
            return false;
        }
        return true;
    }

    public void stopSequence() {
        if (SerialCommunication.serialPort != null) {
            SerialCommunication.serialPort.closePort();
            SerialCommunication.serialPort = null;
        }
        checkNotNull(startSequenceButton).disableProperty().set(false);
        checkNotNull(stopSequenceButton).disableProperty().set(true);
    }

    public void startSequence() {
        int bank;
        String bankStr = checkNotNull(bankComboBox).valueProperty().get();
        switch (bankStr) {
            case BANK1: bank = 1; break;
            case BANK2: bank = 2; break;
            case BANK3: bank = 3; break;
            default: String msg = "Unknown BANK " + bankStr;
                Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
                LOGGER.error(msg);
                alert.showAndWait();
                return;
        }

        try {
            int backupBlockCount = Integer.parseInt(checkNotNull(blockCountTextField).textProperty().get());
            if (!checkBlockCountAgainstBank("Backup", backupBlockCount, bank)) {
                return;
            }
        } catch (Exception e) {}

        try {
            int restoreBlockCount = Integer.parseInt(checkNotNull(restoreBlockCountTextField).textProperty().get());
            if (!checkBlockCountAgainstBank("Restore", restoreBlockCount, bank)) {
                return;
            }
            if (!checkBankOverflow(restoreBlockCount, bank)) {
                return;
            }
        } catch (Exception e) {}

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
        checkNotNull(stopSequenceButton).disableProperty().set(false);
        Consumer<String> logger = s -> Platform.runLater(() -> addLog(s));

        File saveDbFile_ = saveDbFile;
        File loadDbFile_ = loadDbFile;
        Integer blockCount_ = blockCount;

        new Thread(() -> {
            try {
                SerialCommunication.runSequence(comPort, (short)bank, saveDbFile_, loadDbFile_, blockCount_, logger);
                stopSequence();
            } catch (Exception e) {
                LOGGER.error("RunSequence error", e);
                addLog(e.toString());
                stopSequence();
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
