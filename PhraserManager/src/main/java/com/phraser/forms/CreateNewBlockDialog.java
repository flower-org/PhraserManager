package com.phraser.forms;

import com.phraser.db.BlockType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class CreateNewBlockDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(CreateNewBlockDialog.class);

    final static String PHRASE_BLOCK = "PhraseBlock";
    final static String PHRASE_TEMPLATES_BLOCK = "PhraseTemplatesBlock";
    final static String FOLDERS_BLOCK = "FoldersBlock";
    final static String SYMBOL_SETS_BLOCK = "SymbolSetsBlock";
    final static String KEY_BLOCK = "KeyBlock";

    @FXML @Nullable ComboBox<String> blockTypeComboBox;
    @Nullable volatile BlockType blockType = null;

    @Nullable Stage stage;

    public CreateNewBlockDialog() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("CreateNewBlockDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void okClose() {
        try {
            String selected = checkNotNull(blockTypeComboBox).getSelectionModel().getSelectedItem();

            switch (selected) {
                case FOLDERS_BLOCK: blockType = BlockType.FOLDERS_BLOCK; break;
                case SYMBOL_SETS_BLOCK: blockType = BlockType.SYMBOL_SETS_BLOCK; break;
                case PHRASE_TEMPLATES_BLOCK: blockType = BlockType.PHRASE_TEMPLATES_BLOCK; break;
                case PHRASE_BLOCK: blockType = BlockType.PHRASE_BLOCK; break;
                case KEY_BLOCK: blockType = BlockType.KEY_BLOCK; break;
                default: throw new RuntimeException("Unknown block type " + selected);
            }
            checkNotNull(stage).close();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "WorkspaceDialog close Error: " + e, ButtonType.OK);
            LOGGER.error("WorkspaceDialog close Error:", e);
            alert.showAndWait();
        }
    }

    @Nullable public BlockType getBlockType() {
        return blockType;
    }
}
