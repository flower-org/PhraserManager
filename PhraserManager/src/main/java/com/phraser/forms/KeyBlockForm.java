package com.phraser.forms;

import com.phraser.HexTool;
import com.phraser.db.Block;
import com.phraser.db.ImmutableKeyBlock;
import com.phraser.db.KeyBlock;
import com.phraser.db.PhraserDB;
import com.phraser.dbcodec.FlatBufBlockEncoder;
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
    @FXML @Nullable TextField blockSizeTextField;

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

        updateBlockSize();
    }

    public KeyBlock formKeyBlock() {
        // 1 - empty key proceed?
        byte[] key = HexTool.hexStringToByteArray(checkNotNull(keyTextField).textProperty().get());
        byte[] iv = HexTool.hexStringToByteArray(checkNotNull(ivTextField).textProperty().get());
        String dbName = checkNotNull(dbNameTextField).textProperty().get();

        if (key.length != 32) {
            throw new RuntimeException("Key should be 32 bytes long (64 hex chars)");
        }
        if (iv.length != 16) {
            throw new RuntimeException("IV should be 16 bytes long (32 hex chars)");
        }
        if (StringUtils.isBlank(dbName)) {
            throw new RuntimeException("DB Name should not be empty");
        }
        int blockCount;
            blockCount = Integer.parseInt(checkNotNull(blockCountTextField).textProperty().get());
            if (blockCount < MIN_BLOCK_COUNT || blockCount > MAX_BLOCK_COUNT) {
                throw new RuntimeException("Block count should be between "
                        + MIN_BLOCK_COUNT + " and " + MAX_BLOCK_COUNT + " (inclusive)");
            }

        return ImmutableKeyBlock.builder()
                .blockId(keyBlock == null ? -1 : checkNotNull(keyBlock.keyBlock()).blockId())
                .version(-1)
                .blockCount(blockCount)
                .entropy(PhraserUtils.generateEntropy())
                .key(key)
                .iv(iv)
                .dbName(dbName)
                .build();
    }

    public void saveToDb() {
        try {
            // This call will close the form and process the formed block
            KeyBlock newKeyBlock = formKeyBlock();
            keyBlockCallback.accept(newKeyBlock);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "saveToDb Error " + e, ButtonType.OK);
            LOGGER.error("saveToDb Error", e);
            alert.showAndWait();
            return;
        }
    }

    public void generateKey() {
        SecretKey key = PhraserUtils.getAes256Key();
        aes256Key = key.getEncoded();
        String hexKey = HexTool.bytesToHex(aes256Key);

        checkNotNull(keyTextField).setText(hexKey);
        updateBlockSize();
    }

    public void generateIv() {
        iv = PhraserUtils.generateAesIv();
        String hexIv = HexTool.bytesToHex(iv);

        checkNotNull(ivTextField).setText(hexIv);
        updateBlockSize();
    }

    public void updateBlockSize() {
        try {
            Block block = Block.of(formKeyBlock());

            int bufferLength = FlatBufBlockEncoder.toFlatBufBlock(block).length;
            checkNotNull(blockSizeTextField).textProperty().set(Integer.toString(bufferLength));
        } catch(Exception e) {}
    }
}
