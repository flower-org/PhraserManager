package com.phraser.forms;

import com.phraser.HexTool;
import com.phraser.db.Block;
import com.phraser.db.ImmutableKeyBlock;
import com.phraser.db.KeyBlock;
import com.phraser.db.PhraserDB;
import com.phraser.utils.PhraserUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.AnchorPane;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.phraser.forms.PhraserDbForm.NEW_BLOCK;

public class KeyBlockForm extends AnchorPane {
    final static Logger LOGGER = LoggerFactory.getLogger(KeyBlockForm.class);

    final static int MIN_BLOCK_COUNT = 10;
    final static int MAX_BLOCK_COUNT = 384;

    @Nullable final Block keyBlock;
    @Nullable @FXML TextField keyTextField;
    @Nullable @FXML TextField ivTextField;
    @Nullable @FXML TextField blockIdTextField;
    @Nullable @FXML TextField versionTextField;
    @Nullable @FXML TextField blockCountTextField;
    @FXML @Nullable TextField entropyTextField;

    @Nullable @FXML TextField dbNameTextField;
    @Nullable @FXML Label blockCountLabel;

    final PhraserDB phraserDB;
    @Nullable volatile byte[] aes256Key;
    @Nullable volatile byte[] iv;

    final Consumer<KeyBlock> keyBlockCallback;

    public KeyBlockForm(@Nullable Block keyBlock, PhraserDB phraserDB, Consumer<KeyBlock> keyBlockCallback) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("KeyBlockForm.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        checkNotNull(blockCountLabel).setText("Block count (" + MIN_BLOCK_COUNT + "-" + MAX_BLOCK_COUNT + "):");

        checkNotNull(keyTextField).setTextFormatter(new TextFormatter<>(change -> {
                String newText = change.getControlNewText();
                if (newText.length() <= 64 && newText.matches("[0-9a-fA-F]*")) {
                    change.setText(change.getText().toLowerCase());
                    return change;
                }
                return null;
            }
        ));

        checkNotNull(ivTextField).setTextFormatter(new TextFormatter<>(change -> {
                String newText = change.getControlNewText();
                if (newText.length() <= 32 && newText.matches("[0-9a-fA-F]*")) {
                    change.setText(change.getText().toLowerCase());
                    return change;
                }
                return null;
            }
        ));

        checkNotNull(blockCountTextField).setTextFormatter(new TextFormatter<>(change -> {
                String newText = change.getControlNewText();
                if (newText.length() <= 32 && newText.matches("[0-9]*")) {
                    change.setText(change.getText().toLowerCase());
                    return change;
                }
                return null;
            }
        ));

        this.keyBlock = keyBlock;
        if (keyBlock == null) {
            checkNotNull(blockIdTextField).setText(NEW_BLOCK);
            checkNotNull(versionTextField).setText(NEW_BLOCK);
            checkNotNull(entropyTextField).textProperty().set(NEW_BLOCK);
            checkNotNull(dbNameTextField).setText(phraserDB.dbName());
            checkNotNull(blockCountTextField).textProperty().set("384");
        } else {
            checkNotNull(blockIdTextField).setText(Integer.toString(checkNotNull(keyBlock.keyBlock()).blockId()));
            checkNotNull(versionTextField).setText(Long.toString(checkNotNull(keyBlock.keyBlock()).version()));
            checkNotNull(dbNameTextField).setText(keyBlock.keyBlock().dbName());
            checkNotNull(blockCountTextField).textProperty().set(Integer.toString(checkNotNull(keyBlock.keyBlock()).blockCount()));
            checkNotNull(entropyTextField).textProperty().set(Long.toString(keyBlock.getEntropy()));

            checkNotNull(keyTextField).setText(HexTool.bytesToHex(keyBlock.keyBlock().key()));
            checkNotNull(ivTextField).setText(HexTool.bytesToHex(keyBlock.keyBlock().iv()));
        }

        this.phraserDB = phraserDB;
        this.keyBlockCallback = keyBlockCallback;
    }

    public void saveToDb() {
        // 1 - empty key proceed?
        byte[] key = HexTool.hexStringToByteArray(checkNotNull(keyTextField).textProperty().get());
        byte[] iv = HexTool.hexStringToByteArray(checkNotNull(ivTextField).textProperty().get());
        String dbName = checkNotNull(dbNameTextField).textProperty().get();

        if (key.length != 32) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Key should be 32 bytes long (64 hex chars)", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        if (iv.length != 16) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "IV should be 16 bytes long (32 hex chars)", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        if (StringUtils.isBlank(dbName)) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "DB Name should not be empty", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        int blockCount;
        try {
            blockCount = Integer.parseInt(checkNotNull(blockCountTextField).textProperty().get());
            if (blockCount < MIN_BLOCK_COUNT || blockCount > MAX_BLOCK_COUNT) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Block count should be between "
                        + MIN_BLOCK_COUNT + " and " + MAX_BLOCK_COUNT + " (inclusive)", ButtonType.OK);
                alert.showAndWait();
                return;
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "saveToDb Error " + e, ButtonType.OK);
            LOGGER.error("saveToDb Error", e);
            alert.showAndWait();
            return;
        }

        KeyBlock newKeyBlock = ImmutableKeyBlock.builder()
                .blockId(keyBlock == null ? -1 : checkNotNull(keyBlock.keyBlock()).blockId())
                .version(-1)
                .blockCount(blockCount)
                .entropy(PhraserUtils.generateEntropy())
                .key(key)
                .iv(iv)
                .dbName(dbName)
                .build();

        // This call will close the form and process the formed block
        keyBlockCallback.accept(newKeyBlock);
    }

    public void generateKey() {
        SecretKey key = PhraserUtils.getAes256Key();
        aes256Key = key.getEncoded();
        String hexKey = HexTool.bytesToHex(aes256Key);

        checkNotNull(keyTextField).setText(hexKey);
    }

    public void generateIv() {
        iv = PhraserUtils.generateAesIv();
        String hexIv = HexTool.bytesToHex(iv);

        checkNotNull(ivTextField).setText(hexIv);
    }
}
