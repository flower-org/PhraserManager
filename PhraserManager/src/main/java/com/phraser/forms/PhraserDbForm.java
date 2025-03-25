package com.phraser.forms;

import com.flower.fxutils.JavaFxUtils;
import com.flower.fxutils.ModalWindow;
import com.phraser.db.BlockType;
import com.phraser.db.FoldersBlock;
import com.phraser.db.ImmutableFoldersBlock;
import com.phraser.db.ImmutableKeyBlock;
import com.phraser.db.ImmutablePhraseBlock;
import com.phraser.db.ImmutablePhraseTemplatesBlock;
import com.phraser.db.ImmutableSymbolSetsBlock;
import com.phraser.db.KeyBlock;
import com.phraser.db.PhraseBlock;
import com.phraser.db.PhraserDB;
import com.phraser.db.PhraseTemplatesBlock;
import com.phraser.db.Block;
import com.phraser.db.SymbolSetsBlock;
import com.phraser.dbcodec.DbFileManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.flower.fxutils.JavaFxUtils.YesNo.YES;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.flower.fxutils.JavaFxUtils.YesNo.NO;
import static com.phraser.db.BlockType.KEY_BLOCK;
import static com.phraser.db.BlockType.PHRASE_BLOCK;
import static com.phraser.db.DefaultDBCreator.DEFAULT_BLOCKS_IN_DB;
import static com.phraser.db.DefaultDBCreator.initDefaultBlockConfig;

public class PhraserDbForm extends AnchorPane {
    final static Logger LOGGER = LoggerFactory.getLogger(PhraserDbForm.class);
    public final static String NEW_BLOCK = "[NEW BLOCK]";

    public final static String OPEN_OLD = "View this version";
    public final static String OPEN_LATEST = "View latest version";
    public final static String CANCEL = "Cancel";

    @FXML @Nullable TableView<Block> dbBlocksTable;

    @Nullable Stage stage;
    final MainForm mainForm;
    // Own tab - contains DB name
    @Nullable Tab tab;

    // Related opened block tabs
    @Nullable Tab keyBlockTab;
    @Nullable Tab symbolSetsBlockTab;
    @Nullable Tab foldersBlockTab;
    @Nullable Tab phraseTemplatesBlockTab;
    Map<Integer, Tab> phraseBlockTabMap = new HashMap<>();

    // DB structure
    final PhraserDB phraserDB;
    long dbExportVersion;

    public PhraserDbForm(MainForm mainForm, String defaultDbName, boolean initDefaultConfig) {
        this(mainForm, initDefaultConfig ? initDefaultBlockConfig(defaultDbName) : List.of());
    }

    public PhraserDbForm(MainForm mainForm, List<Block> blocks) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("PhraserDbForm.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        phraserDB = new PhraserDB(List.of(), DEFAULT_BLOCKS_IN_DB, null);
        if (blocks != null) {
            for (Block dbBlock : blocks) {
                addBlock(dbBlock);
            }
        }
        dbExportVersion = phraserDB.getLastVersion();

        checkNotNull(dbBlocksTable).setRowFactory(new Callback<>() {
            @Override
            public TableRow<Block> call(TableView<Block> blockTableView) {
                return new TableRow<>() {
                    @Override
                    protected void updateItem(Block block, boolean empty) {
                        super.updateItem(block, empty);
                        if (block != null) {
                            if (!phraserDB.isLatest(block)) {
                                styleProperty().setValue("-fx-background-color: salmon");
                            } else {
                                if (block.blockType() == PHRASE_BLOCK && checkNotNull(block.phraseBlock()).isTombstone()) {
                                    styleProperty().setValue("-fx-background-color: crimson");
                                } else {
                                    styleProperty().setValue("-fx-background-color: lightgreen");
                                }
                            }
                        }
                    }
                };
            }
        });
        checkNotNull(dbBlocksTable).itemsProperty().set(phraserDB.blocksObservableArray());
        this.mainForm = mainForm;
    }

    public void addBlock(Block dbBlock) {
        try {
            if (dbBlock.blockType() == KEY_BLOCK) {
                Block lastKey = phraserDB.getLastKeyBlock();
                if (lastKey != null) {
                    int oldBlockCount = checkNotNull(lastKey.keyBlock()).blockCount();
                    int newBlockCount = checkNotNull(dbBlock.keyBlock()).blockCount();
                    if (newBlockCount > oldBlockCount) {
                        if (YES != JavaFxUtils.showYesNoDialog("KeyBlock old version removal",
                                "You're tying to increase blockCount in the DB (" + oldBlockCount + " -> " + newBlockCount + ").\n" +
                                        "In order to increase blockCount we need to remove all previous versions of KeyBlock. Proceed?")) {
                            return;
                        }
                        phraserDB.removeAllKeyBlocks();
                    }
                }
            }

            phraserDB.addBlock(dbBlock);
            checkNotNull(dbBlocksTable).refresh();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error saving Block: " + e.getMessage(), ButtonType.OK);
            LOGGER.error("Error saving Block: ", e);
            alert.showAndWait();
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setTab(Tab tab) {
        this.tab = tab;
        phraserDB.setDbNameListener(s -> checkNotNull(tab).setText(s));
        // Intercept the close request for the tab
        tab.setOnCloseRequest(event -> {
            if (phraserDB.getLastVersion() > dbExportVersion) {
                if (YES != JavaFxUtils.showYesNoDialog("DB not exported", "DB was last exported at version ["
                        + dbExportVersion + "]; current version [" + phraserDB.getLastVersion() + "]. Your updates will be lost. Close anyway?")) {
                    event.consume(); // Prevent the tab from closing
                }
            }
        });
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
                                    newKeyBlockForm();
                                } else if (blockType == BlockType.SYMBOL_SETS_BLOCK) {
                                    newSymbolSetsBlockForm();
                                } else if (blockType == BlockType.FOLDERS_BLOCK) {
                                    newFoldersBlockForm();
                                } else if (blockType == BlockType.PHRASE_TEMPLATES_BLOCK) {
                                    newPhraseTemplatesBlockForm();
                                } else if (blockType == PHRASE_BLOCK) {
                                    newPhraseBlockForm();
                                } else {
                                    Alert alert = new Alert(Alert.AlertType.ERROR, "Unsupported block type: " + blockType, ButtonType.OK);
                                    LOGGER.error("Unsupported block type: " + blockType);
                                    alert.showAndWait();
                                }
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding Block: " + e, ButtonType.OK);
                            LOGGER.error("Error adding Block: ", e);
                            alert.showAndWait();
                        }
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding Block: " + e, ButtonType.OK);
            LOGGER.error("Error adding Block: ", e);
            alert.showAndWait();
        }
    }

    public void newKeyBlockForm() {
        if (keyBlockTab != null && checkNotNull(mainForm.getTabs()).getTabs().contains(keyBlockTab)) {
            checkNotNull(mainForm.getTabs()).getSelectionModel().select(keyBlockTab);
        } else {
            Block existingKeyBlock = phraserDB.getLastKeyBlock();
            if (existingKeyBlock != null) {
                if (JavaFxUtils.showYesNoDialog("KeyBlock exists, edit?") == NO) {
                    return;
                }
            }
            openKeyBlockForm(existingKeyBlock);
        }
    }

    public void openKeyBlockForm(@Nullable Block existingKeyBlock) {
        keyBlockTab = mainForm.openKeyBlockForm(existingKeyBlock, phraserDB,
                keyBlock -> {
                    int blockId;
                    long version = phraserDB.incrementAndGetVersion();
                    Block lastKeyBlock = phraserDB.getLastKeyBlock();
                    if (lastKeyBlock != null) {
                        blockId = checkNotNull(lastKeyBlock.keyBlock()).blockId();
                    } else {
                        blockId = phraserDB.incrementAndGetBlockId();
                    }

                    KeyBlock blockWithVersionAndEntropy = ImmutableKeyBlock.builder()
                            .from(keyBlock)
                            .blockId(blockId)
                            .version(version)
                            .build();

                    Block block = Block.of(blockWithVersionAndEntropy);
                    addBlock(block);

                    checkNotNull(mainForm.getTabs()).getTabs().remove(keyBlockTab);
                    keyBlockTab = null;
                });
    }

    public void newSymbolSetsBlockForm() {
        if (symbolSetsBlockTab != null && checkNotNull(mainForm.getTabs()).getTabs().contains(symbolSetsBlockTab)) {
            checkNotNull(mainForm.getTabs()).getSelectionModel().select(symbolSetsBlockTab);
        } else {
            Block existingSymbolSetBlock = phraserDB.getLastSymbolSetBlock();
            if (existingSymbolSetBlock != null) {
                if (JavaFxUtils.showYesNoDialog("SymbolSetBlock exists, edit?") == NO) {
                    return;
                }
            }
            openSymbolSetsBlockForm(existingSymbolSetBlock);
        }
    }

    public void openSymbolSetsBlockForm(@Nullable Block existingSymbolSetBlock) {
        symbolSetsBlockTab = mainForm.openSymbolSetsBlockForm(existingSymbolSetBlock,
                symbolSetsBlock -> {
                    int blockId;
                    long version = phraserDB.incrementAndGetVersion();
                    Block lastSymbolSetsBlock = phraserDB.getLastSymbolSetBlock();
                    if (lastSymbolSetsBlock != null) {
                        blockId = checkNotNull(lastSymbolSetsBlock.symbolSetsBlock()).blockId();
                    } else {
                        blockId = phraserDB.incrementAndGetBlockId();
                    }

                    SymbolSetsBlock blockWithVersionAndEntropy = ImmutableSymbolSetsBlock.builder()
                            .from(symbolSetsBlock)
                            .blockId(blockId)
                            .version(version)
                            .build();

                    Block block = Block.of(blockWithVersionAndEntropy);
                    addBlock(block);

                    checkNotNull(mainForm.getTabs()).getTabs().remove(symbolSetsBlockTab);
                    symbolSetsBlockTab = null;
                });
    }

    public void newFoldersBlockForm() {
        if (foldersBlockTab != null && checkNotNull(mainForm.getTabs()).getTabs().contains(foldersBlockTab)) {
            checkNotNull(mainForm.getTabs()).getSelectionModel().select(foldersBlockTab);
        } else {
            Block existingFoldersBlock = phraserDB.getLastFoldersBlock();
            if (existingFoldersBlock != null) {
                if (JavaFxUtils.showYesNoDialog("FoldersBlock exists, edit?") == NO) {
                    return;
                }
            }
            openFoldersBlockForm(existingFoldersBlock);
        }
    }

    public void openFoldersBlockForm(@Nullable Block existingFoldersBlock) {
        foldersBlockTab = mainForm.openFoldersBlockForm(existingFoldersBlock, phraserDB,
                foldersBlock -> {
                    int blockId;
                    long version = phraserDB.incrementAndGetVersion();
                    Block lastFoldersBlock = phraserDB.getLastFoldersBlock();
                    if (lastFoldersBlock != null) {
                        blockId = checkNotNull(lastFoldersBlock.foldersBlock()).blockId();
                    } else {
                        blockId = phraserDB.incrementAndGetBlockId();
                    }

                    FoldersBlock blockWithVersionAndEntropy = ImmutableFoldersBlock.builder()
                            .from(foldersBlock)
                            .blockId(blockId)
                            .version(version)
                            .build();

                    Block block = Block.of(blockWithVersionAndEntropy);
                    addBlock(block);

                    checkNotNull(mainForm.getTabs()).getTabs().remove(foldersBlockTab);
                    foldersBlockTab = null;
                });
    }

    public void newPhraseTemplatesBlockForm() {
        if (phraseTemplatesBlockTab != null && checkNotNull(mainForm.getTabs()).getTabs().contains(phraseTemplatesBlockTab)) {
            checkNotNull(mainForm.getTabs()).getSelectionModel().select(phraseTemplatesBlockTab);
        } else {
            Block existingPhraseTemplatesBlock = phraserDB.getLastPhraseTemplatesBlock();
            if (existingPhraseTemplatesBlock != null) {
                if (JavaFxUtils.showYesNoDialog("PhraseTemplatesBlock exists, edit?") == NO) {
                    return;
                }
            }
            openPhraseTemplatesBlockForm(existingPhraseTemplatesBlock);
        }
    }

    public void openPhraseTemplatesBlockForm(@Nullable Block existingPhraseTemplatesBlock) {
        phraseTemplatesBlockTab = mainForm.openPhraseTemplatesBlockForm(existingPhraseTemplatesBlock,
                phraserDB,
                phraseTemplatesBlock -> {
                    int blockId;
                    long version = phraserDB.incrementAndGetVersion();
                    Block lastPhraseTemplatesBlock = phraserDB.getLastPhraseTemplatesBlock();
                    if (lastPhraseTemplatesBlock != null) {
                        blockId = checkNotNull(lastPhraseTemplatesBlock.phraseTemplatesBlock()).blockId();
                    } else {
                        blockId = phraserDB.incrementAndGetBlockId();
                    }

                    PhraseTemplatesBlock blockWithVersionAndEntropy = ImmutablePhraseTemplatesBlock.builder()
                            .from(phraseTemplatesBlock)
                            .blockId(blockId)
                            .version(version)
                            .build();

                    Block block = Block.of(blockWithVersionAndEntropy);
                    addBlock(block);

                    checkNotNull(mainForm.getTabs()).getTabs().remove(phraseTemplatesBlockTab);
                    phraseTemplatesBlockTab = null;
                });
    }

    public void newPhraseBlockForm() {
        openPhraseBlockForm(null);
    }

    public void openPhraseBlockForm(@Nullable Block existingPhraseBlock) {
        String error = null;
        Block foldersDbBlock = phraserDB.getLastFoldersBlock();
        if (foldersDbBlock == null) {
            error = "FoldersBlock not found, please create";
        }
        Block phraseTemplatesDbBlock = phraserDB.getLastPhraseTemplatesBlock();
        if (phraseTemplatesDbBlock == null) {
            error = "PhraseTemplatesBlock not found, please create";
        }
        Block symbolSetsBlock = phraserDB.getLastSymbolSetBlock();
        if (symbolSetsBlock == null) {
            error = "SymbolSetsBlock not found, please create";
        }
        if (error != null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, error, ButtonType.OK);
            LOGGER.error(error);
            alert.showAndWait();
            return;
        }

        AtomicReference<Tab> phraseBlockTabRef = new AtomicReference<>();
        Tab phraseBlockTab = mainForm.openPhraseBlockForm(existingPhraseBlock,
            checkNotNull(foldersDbBlock),
            checkNotNull(phraseTemplatesDbBlock),
            checkNotNull(symbolSetsBlock),
            phraserDB,
            phraseBlock -> {
                int blockId;
                long version = phraserDB.incrementAndGetVersion();
                if (existingPhraseBlock != null) {
                    blockId = checkNotNull(existingPhraseBlock.phraseBlock()).blockId();
                } else {
                    blockId = phraserDB.incrementAndGetBlockId();
                }

                PhraseBlock blockWithVersionAndEntropy = ImmutablePhraseBlock.builder()
                        .from(phraseBlock)
                        .blockId(blockId)
                        .version(version)
                        .build();

                Block block = Block.of(blockWithVersionAndEntropy);
                addBlock(block);

                Tab phraseBlockTabFromMap = phraseBlockTabMap.get(blockId);
                if (phraseBlockTabFromMap == null) {
                    // e.g. new block
                    phraseBlockTabFromMap = phraseBlockTabRef.get();
                }
                checkNotNull(mainForm.getTabs()).getTabs().remove(phraseBlockTabFromMap);
                phraseBlockTabMap.remove(blockId);
            });
        phraseBlockTabRef.set(phraseBlockTab);
        if (existingPhraseBlock != null) {
            phraseBlockTabMap.put(checkNotNull(existingPhraseBlock.phraseBlock()).blockId(), phraseBlockTab);
        }
    }

    public void updateBlockAction() {
        try {
            Block block = checkNotNull(dbBlocksTable).selectionModelProperty().get().getSelectedItem();
            if (block == null) { return; }

            BlockType blockType = block.blockType();

            //1. If tab for the block exists, switch to existing tab
            switch (blockType) {
                case FOLDERS_BLOCK:
                    if (foldersBlockTab != null && checkNotNull(mainForm.getTabs()).getTabs().contains(foldersBlockTab)) {
                        checkNotNull(mainForm.getTabs()).getSelectionModel().select(foldersBlockTab);
                        return;
                    }
                    break;
                case SYMBOL_SETS_BLOCK:
                    if (symbolSetsBlockTab != null && checkNotNull(mainForm.getTabs()).getTabs().contains(symbolSetsBlockTab)) {
                        checkNotNull(mainForm.getTabs()).getSelectionModel().select(symbolSetsBlockTab);
                        return;
                    }
                    break;
                case PHRASE_TEMPLATES_BLOCK:
                    if (phraseTemplatesBlockTab != null && checkNotNull(mainForm.getTabs()).getTabs().contains(phraseTemplatesBlockTab)) {
                        checkNotNull(mainForm.getTabs()).getSelectionModel().select(phraseTemplatesBlockTab);
                        return;
                    }
                    break;
                case PHRASE_BLOCK:
                    Tab existingPhraseBlockTab = phraseBlockTabMap.get(block.getBlockId());
                    if (existingPhraseBlockTab != null && checkNotNull(mainForm.getTabs()).getTabs().contains(existingPhraseBlockTab)) {
                        checkNotNull(mainForm.getTabs()).getSelectionModel().select(existingPhraseBlockTab);
                        return;
                    } else if (existingPhraseBlockTab != null) {
                        phraseBlockTabMap.remove(block.getBlockId());
                    }
                    break;
                case KEY_BLOCK:
                    if (keyBlockTab != null && checkNotNull(mainForm.getTabs()).getTabs().contains(keyBlockTab)) {
                        checkNotNull(mainForm.getTabs()).getSelectionModel().select(keyBlockTab);
                        return;
                    }
                    break;
                default: throw new RuntimeException("Unknown Block Type " + block.blockType());
            }

            //2. If there is no tab open for the block, open a new tab
            //Make sure we're opening the desired version
            long lastBlockVersion = phraserDB.getLastBlockVersion(block.getBlockId());
            boolean findLatestVersion;
            if (lastBlockVersion > block.getVersion()) {
                String result = JavaFxUtils.showCustomDialog("Old Block", "Old Block",
                        "You're viewing/updating a block off its old version.",
                        OPEN_OLD, OPEN_LATEST, CANCEL
                        );
                if (result == null) { throw new RuntimeException("Unknown dialog result: " + result); }
                switch (result) {
                    case CANCEL: return;
                    case OPEN_OLD: findLatestVersion = false; break;
                    case OPEN_LATEST: findLatestVersion = true; break;
                    default: throw new RuntimeException("Unknown dialog result: " + result);
                }
            } else { findLatestVersion = false; }

            // Get latest version if needed and warn if it's tombstoned (PhraseBlock only)
            if (findLatestVersion || blockType == PHRASE_BLOCK) {
                Block latestBlock = checkNotNull(phraserDB.getLastBlock(block.getBlockId()));
                if (findLatestVersion) {
                    block = latestBlock;
                }
                if (blockType == PHRASE_BLOCK) {
                    if (checkNotNull(latestBlock.phraseBlock()).isTombstone()) {
                        if (NO == JavaFxUtils.showYesNoDialog("Tombstoned PhraseBlock", "The latest version of this PhraseBlock is tombstoned. Proceed?")) {
                            return;
                        }
                    }
                }
            }

            //Open the block form
            switch (block.blockType()) {
                case FOLDERS_BLOCK:
                    openFoldersBlockForm(block);
                    break;
                case SYMBOL_SETS_BLOCK:
                    openSymbolSetsBlockForm(block);
                    break;
                case PHRASE_TEMPLATES_BLOCK:
                    openPhraseTemplatesBlockForm(block);
                    break;
                case PHRASE_BLOCK:
                    openPhraseBlockForm(block);
                    break;
                case KEY_BLOCK:
                    openKeyBlockForm(block);
                    break;
                default: throw new RuntimeException("Unknown Block Type " + block.blockType());
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error updating Block: " + e, ButtonType.OK);
            LOGGER.error("Error updating Block: ", e);
            alert.showAndWait();
        }
    }

    public void compactDBAction() {
        try {
            phraserDB.compact();
            checkNotNull(dbBlocksTable).refresh();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error compacting DB: " + e, ButtonType.OK);
            LOGGER.error("Error compacting DB: ", e);
            alert.showAndWait();
        }
    }

    public void exportDBAction() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Phraser Database files (*.phr)", "*.phr"));
            fileChooser.setTitle("Export Database");
            File saveFile = fileChooser.showSaveDialog(checkNotNull(stage));
            if (saveFile == null) { return; }

            if (!saveFile.getName().endsWith(".phr")) {
                saveFile = new File(saveFile.getPath()  + ".phr");
            }

            EnterPasswordDialog enterPasswordDialog = new EnterPasswordDialog();
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(stage),
                    stage -> { enterPasswordDialog.setStage(stage); return enterPasswordDialog; },
                    "Enter Database Password");

            final File finalSaveFile = saveFile;
            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            String password = enterPasswordDialog.getPassword();
                            if (password != null) {
                                int iterations = checkNotNull(enterPasswordDialog.getPbkdf2IterationCount());
                                DbFileManager.writeBlocksToFile(phraserDB.blocksObservableArray(), password, iterations, finalSaveFile);

                                String successfulMessage = "DB `" + phraserDB.dbName() + "` exported to " + finalSaveFile.getPath() + ".";
                                Alert alert = new Alert(Alert.AlertType.INFORMATION, successfulMessage, ButtonType.OK);
                                LOGGER.info(successfulMessage);
                                alert.showAndWait();

                                dbExportVersion = phraserDB.getLastVersion();
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error exporting DB: " + e, ButtonType.OK);
                            LOGGER.error("Error exporting DB: ", e);
                            alert.showAndWait();
                        }
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error exporting DB: " + e, ButtonType.OK);
            LOGGER.error("Error exporting DB: ", e);
            alert.showAndWait();
        }
    }
}
