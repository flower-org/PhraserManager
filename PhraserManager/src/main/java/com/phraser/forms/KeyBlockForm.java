package com.phraser.forms;

import com.phraser.HexTool;
import com.phraser.db.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

public class KeyBlockForm extends AnchorPane {
    final static Logger LOGGER = LoggerFactory.getLogger(KeyBlockForm.class);
    static final KeyGenerator KEY_GEN;
    static final SecureRandom SECURE_RANDOM;
    static {
        try {
            SECURE_RANDOM = new SecureRandom();

            KEY_GEN = KeyGenerator.getInstance("AES");
            KEY_GEN.init(256);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable final Block keyBlock;
    @Nullable @FXML TextField keyTextField;
    @Nullable @FXML TextField ivTextField;
    @Nullable @FXML TextField blockIdTextField;
    @Nullable @FXML TextField dbNameTextField;
    @Nullable @FXML Button addUpdateKeyBlock;

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

        this.keyBlock = keyBlock;
        if (keyBlock == null) {
            checkNotNull(blockIdTextField).setText("[NEW BLOCK]");
            checkNotNull(dbNameTextField).setText(phraserDB.dbName());

            checkNotNull(addUpdateKeyBlock).textProperty().set("Add key block");
        } else {
            checkNotNull(blockIdTextField).setText(Integer.toString(checkNotNull(keyBlock.keyBlock()).blockId()));
            checkNotNull(dbNameTextField).setText(keyBlock.keyBlock().dbName());

            checkNotNull(keyTextField).setText(HexTool.bytesToHex(keyBlock.keyBlock().key()));
            checkNotNull(ivTextField).setText(HexTool.bytesToHex(keyBlock.keyBlock().iv()));

            checkNotNull(addUpdateKeyBlock).textProperty().set("Update key block");
        }

        this.phraserDB = phraserDB;
        this.keyBlockCallback = keyBlockCallback;
    }

    public void addUpdateBlock() {
        // TODO: close form and add block to
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

        KeyBlock newKeyBlock = ImmutableKeyBlock.builder()
                .blockId(keyBlock == null ? -1 : checkNotNull(keyBlock.keyBlock()).blockId())
                .version(-1)
                .entropy(SECURE_RANDOM.nextLong())

                .key(key)
                .iv(iv)
                .dbName(dbName)
                .build();

        keyBlockCallback.accept(newKeyBlock);
    }

    public void generateKey() {
        SecretKey key = KEY_GEN.generateKey();
        aes256Key = key.getEncoded();
        String hexKey = HexTool.bytesToHex(aes256Key);

        checkNotNull(keyTextField).setText(hexKey);
    }

    public void generateIv() {
        if (iv == null) {
            iv = new byte[16];
        }
        SECURE_RANDOM.nextBytes(iv);
        String hexIv = HexTool.bytesToHex(iv);

        checkNotNull(ivTextField).setText(hexIv);
    }
}
