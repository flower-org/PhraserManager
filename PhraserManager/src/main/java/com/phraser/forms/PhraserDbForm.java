package com.phraser.forms;

import com.phraser.JavaFxUtils;
import com.phraser.ModalWindow;
import com.phraser.db.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.phraser.db.PhraserDB.BLOCKS_IN_DB;

public class PhraserDbForm extends AnchorPane {
    final static Logger LOGGER = LoggerFactory.getLogger(PhraserDbForm.class);
    public final static String NEW_BLOCK = "[NEW BLOCK]";

    @FXML @Nullable TableView<Block> dbBlocksTable;
    final ObservableList<Block> dbBlocks;

    final PhraserDB phraserDB;
    @Nullable Stage stage;
    final MainForm mainForm;
    @Nullable Tab tab;

    @Nullable Tab keyBlockTab;
    @Nullable Tab symbolSetsBlockTab;

    public PhraserDbForm(MainForm mainForm, String defaultDbName) {
        this(mainForm, List.of(), defaultDbName);
    }

    public PhraserDbForm(MainForm mainForm, List<Block> blocks, String defaultDbName) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("PhraserDbForm.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        dbBlocks = FXCollections.observableArrayList();
        if (blocks != null) {
            for (Block dbBlock : blocks) {
                addBlock(dbBlock);
            }
        }

        checkNotNull(dbBlocksTable).itemsProperty().set(dbBlocks);

        phraserDB = new PhraserDB(blocks, BLOCKS_IN_DB, defaultDbName, s -> checkNotNull(tab).setText(s));
        this.mainForm = mainForm;
    }

    public void addBlock(Block dbBlock) {
        dbBlocks.add(dbBlock);
        phraserDB.addBlock(dbBlock);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setTab(Tab tab) {
        this.tab = tab;
    }

    public void newBlockAction() {
        try {
            CreateNewBlockDialog createNewBlockDialog = new CreateNewBlockDialog();
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(stage),
                    stage -> { createNewBlockDialog.setStage(stage); return createNewBlockDialog; },
                    "New block");

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            BlockType blockType = createNewBlockDialog.getBlockType();
                            if (blockType != null) {
                                if (blockType == BlockType.KEY_BLOCK) {
                                    openKeyBlockForm();
                                } else if (blockType == BlockType.SYMBOL_SETS_BLOCK) {
                                    openSymbolSetsBlockForm();
                                } else {
                                    Alert alert = new Alert(Alert.AlertType.ERROR, "Unsupported block type: " + blockType, ButtonType.OK);
                                    LOGGER.error("Unsupported block type: " + blockType);
                                    alert.showAndWait();
                                }
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding known server: " + e, ButtonType.OK);
                            LOGGER.error("Error adding known server: ", e);
                            alert.showAndWait();
                        }
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding known server: " + e, ButtonType.OK);
            LOGGER.error("Error adding known server: ", e);
            alert.showAndWait();
        }
    }

    public void openKeyBlockForm() {
        if (keyBlockTab != null && checkNotNull(mainForm.getTabs()).getTabs().contains(keyBlockTab)) {
            checkNotNull(mainForm.getTabs()).getSelectionModel().select(keyBlockTab);
        } else {
            Block existingKeyBlock = phraserDB.getLastKeyBlock();
            if (existingKeyBlock != null) {
                if (JavaFxUtils.showYesNoDialog("KeyBlock exists, edit?") == JavaFxUtils.YesNo.NO) {
                    return;
                }
            }

            keyBlockTab = mainForm.openKeyBlockForm(existingKeyBlock, phraserDB,
                    keyBlock -> {
                        int blockId;
                        int version = 1;
                        Block lastKeyBlock = phraserDB.getLastKeyBlock();
                        if (lastKeyBlock != null) {
                            blockId = checkNotNull(lastKeyBlock.keyBlock()).blockId();
                            version = lastKeyBlock.keyBlock().version() + 1;
                        } else {
                            blockId = phraserDB.getNextBlockId();
                        }

                        KeyBlock blockWithVersionAndEntropy = ImmutableKeyBlock.builder()
                                .from(keyBlock)
                                .blockId(blockId)
                                .version(version)
                                .build();

                        Block block = Block.create(blockWithVersionAndEntropy);
                        addBlock(block);

                        checkNotNull(mainForm.getTabs()).getTabs().remove(keyBlockTab);
                        keyBlockTab = null;
                    });
        }
    }

    public void openSymbolSetsBlockForm() {
        if (symbolSetsBlockTab != null && checkNotNull(mainForm.getTabs()).getTabs().contains(symbolSetsBlockTab)) {
            checkNotNull(mainForm.getTabs()).getSelectionModel().select(symbolSetsBlockTab);
        } else {
            Block existingSymbolSetBlock = phraserDB.getLastSymbolSetBlock();
            if (existingSymbolSetBlock != null) {
                if (JavaFxUtils.showYesNoDialog("SymbolSetBlock exists, edit?") == JavaFxUtils.YesNo.NO) {
                    return;
                }
            }

            symbolSetsBlockTab = mainForm.openSymbolSetsBlockForm(existingSymbolSetBlock, phraserDB,
                    symbolSetsBlock -> {
                        int blockId;
                        int version = 1;
                        Block lastSymbolSetsBlock = phraserDB.getLastSymbolSetBlock();
                        if (lastSymbolSetsBlock != null) {
                            blockId = checkNotNull(lastSymbolSetsBlock.symbolSetsBlock()).blockId();
                            version = lastSymbolSetsBlock.symbolSetsBlock().version() + 1;
                        } else {
                            blockId = phraserDB.getNextBlockId();
                        }

                        SymbolSetsBlock blockWithVersionAndEntropy = ImmutableSymbolSetsBlock.builder()
                                .from(symbolSetsBlock)
                                .blockId(blockId)
                                .version(version)
                                .build();

                        Block block = Block.create(blockWithVersionAndEntropy);
                        addBlock(block);

                        checkNotNull(mainForm.getTabs()).getTabs().remove(symbolSetsBlockTab);
                        symbolSetsBlockTab = null;
                    });
        }
    }

    public void updateBlockAction() {
        //
    }

    public void tombstoneBlockAction() {
        //
    }

    public void defragmentDBAction() {
        //
    }

    public void exportDBAction() {
        //
    }
}
