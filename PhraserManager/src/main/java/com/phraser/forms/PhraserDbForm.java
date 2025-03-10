package com.phraser.forms;

import com.phraser.JavaFxUtils;
import com.phraser.ModalWindow;
import com.phraser.db.BlockType;
import com.phraser.db.FoldersBlock;
import com.phraser.db.Icon;
import com.phraser.db.ImmutableFoldersBlock;
import com.phraser.db.ImmutableKeyBlock;
import com.phraser.db.ImmutablePhraseTemplatesBlock;
import com.phraser.db.ImmutableSymbolSetsBlock;
import com.phraser.db.KeyBlock;
import com.phraser.db.PhraserDB;
import com.phraser.db.PhraseTemplatesBlock;
import com.phraser.db.Block;
import com.phraser.db.SymbolSetsBlock;
import com.phraser.utils.PhraserUtils;
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
import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.phraser.db.PhraseTemplatesBlock.getWordPermissions;
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
    @Nullable Tab foldersBlockTab;
    @Nullable Tab phraseTemplatesBlockTab;

    static List<Block> initDefaultBlockConfig(String dbName) {
        // 1. KeyBlock
        int bucketCount = 256;
        SecretKey aesKey = PhraserUtils.getAes256Key();
        byte[] key = aesKey.getEncoded();
        byte[] iv = PhraserUtils.generateAesIv();
        KeyBlock storeKeyBlock = ImmutableKeyBlock.builder()
                .blockId(1)
                .version(1)
                .bucketCount(bucketCount)
                .entropy(PhraserUtils.generateEntropy())
                .key(key)
                .iv(iv)
                .dbName(dbName)
                .build();
        Block keyBlock = Block.create(storeKeyBlock);

        // 2. SymbolSetsBlock
        List<SymbolSetsBlock.SymbolSet> symbolSets = List.of(
                SymbolSetsBlock.SymbolSet.of(1, "Digits", "0123456789".toCharArray()),
                SymbolSetsBlock.SymbolSet.of(2, "Letters", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray()),
                SymbolSetsBlock.SymbolSet.of(3, "Uppercase", "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()),
                SymbolSetsBlock.SymbolSet.of(4, "Lowercase", "abcdefghijklmnopqrstuvwxyz".toCharArray()),
                SymbolSetsBlock.SymbolSet.of(5, "Special", "%#!*^@$&".toCharArray()),
                SymbolSetsBlock.SymbolSet.of(6, "Min special", "#!?".toCharArray()),
                SymbolSetsBlock.SymbolSet.of(7, "Ext special", "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".toCharArray())
        );
        SymbolSetsBlock storeSymbolSetsBlock = ImmutableSymbolSetsBlock.builder()
                .blockId(2)
                .version(1)
                .entropy(PhraserUtils.generateEntropy())
                .addAllSymbolSets(symbolSets)
                .build();
        Block symbolSetsBlock = Block.create(storeSymbolSetsBlock);

        // 3. FoldersBlock
        List<FoldersBlock.Folder> folders = List.of(
            FoldersBlock.Folder.of(1, 0, "Websites"),
            FoldersBlock.Folder.of(2, 0, "Computers"),
            FoldersBlock.Folder.of(3, 1, "Banking"),
            FoldersBlock.Folder.of(4, 1, "Social"),
            FoldersBlock.Folder.of(5, 2, "Laptops"),
            FoldersBlock.Folder.of(6, 2, "Servers")
        );
        FoldersBlock storeFoldersBlock = ImmutableFoldersBlock.builder()
                .blockId(3)
                .version(1)
                .entropy(PhraserUtils.generateEntropy())
                .addAllFolders(folders)
                .build();
        Block foldersBlock = Block.create(storeFoldersBlock);

        // 4. PhraseTemplatesBlock
        List<PhraseTemplatesBlock.WordTemplate> wordTemplates = List.of(
            PhraseTemplatesBlock.WordTemplate.of(1,
                getWordPermissions(false, true, true, true),
                Icon.LOGIN,
                4,
                256,
                "username",
                List.of()
            ),
            PhraseTemplatesBlock.WordTemplate.of(2,
                getWordPermissions(true, false, true, false),
                Icon.KEY,
                24,
                64,
                "password",
                List.of(1,2,7)
            ),
            PhraseTemplatesBlock.WordTemplate.of(3,
                getWordPermissions(false, true, false, true),
                Icon.QUESTION,
                0,
                512,
                "question",
                List.of()
            ),
            PhraseTemplatesBlock.WordTemplate.of(4,
                getWordPermissions(true, true, true, true),
                Icon.MESSAGE,
                24,
                64,
                "answer",
                List.of(1,2,7)
            )
        );

        List<PhraseTemplatesBlock.PhraseTemplate> phraseTemplates = List.of(
            PhraseTemplatesBlock.PhraseTemplate.of(1,
                "LoginPass",
                List.of(1, 2)),
            PhraseTemplatesBlock.PhraseTemplate.of(2,
                "3 Security questions",
                List.of(1, 2, 3, 4, 3, 4, 3, 4))
        );

        PhraseTemplatesBlock storePhraseTemplatesBlock = ImmutablePhraseTemplatesBlock.builder()
                .blockId(4)
                .version(1)
                .entropy(PhraserUtils.generateEntropy())
                .addAllPhraseTemplates(phraseTemplates)
                .addAllWordTemplates(wordTemplates)
                .build();
        Block phraseTemplatesBlock = Block.create(storePhraseTemplatesBlock);

        return List.of(keyBlock, symbolSetsBlock, foldersBlock, phraseTemplatesBlock);
    }

    public PhraserDbForm(MainForm mainForm, String defaultDbName, boolean initDefaultConfig) {
        this(mainForm, initDefaultConfig ? initDefaultBlockConfig(defaultDbName) : List.of(), defaultDbName);
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
        phraserDB = new PhraserDB(List.of(), BLOCKS_IN_DB, defaultDbName, null);
        if (blocks != null) {
            for (Block dbBlock : blocks) {
                addBlock(dbBlock);
            }
        }

        checkNotNull(dbBlocksTable).itemsProperty().set(dbBlocks);
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
        phraserDB.setDbNameListener(s -> checkNotNull(tab).setText(s));
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
                                } else if (blockType == BlockType.FOLDERS_BLOCK) {
                                    openFoldersBlockForm();
                                } else if (blockType == BlockType.PHRASE_TEMPLATES_BLOCK) {
                                    openPhraseTemplatesBlockForm();
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

    public void openFoldersBlockForm() {
        if (foldersBlockTab != null && checkNotNull(mainForm.getTabs()).getTabs().contains(foldersBlockTab)) {
            checkNotNull(mainForm.getTabs()).getSelectionModel().select(foldersBlockTab);
        } else {
            Block existingFoldersBlock = phraserDB.getLastFoldersBlock();
            if (existingFoldersBlock != null) {
                if (JavaFxUtils.showYesNoDialog("FoldersBlock exists, edit?") == JavaFxUtils.YesNo.NO) {
                    return;
                }
            }

            foldersBlockTab = mainForm.openFoldersBlockForm(existingFoldersBlock, phraserDB,
                    foldersBlock -> {
                        int blockId;
                        int version = 1;
                        Block lastFoldersBlock = phraserDB.getLastFoldersBlock();
                        if (lastFoldersBlock != null) {
                            blockId = checkNotNull(lastFoldersBlock.foldersBlock()).blockId();
                            version = lastFoldersBlock.foldersBlock().version() + 1;
                        } else {
                            blockId = phraserDB.getNextBlockId();
                        }

                        FoldersBlock blockWithVersionAndEntropy = ImmutableFoldersBlock.builder()
                                .from(foldersBlock)
                                .blockId(blockId)
                                .version(version)
                                .build();

                        Block block = Block.create(blockWithVersionAndEntropy);
                        addBlock(block);

                        checkNotNull(mainForm.getTabs()).getTabs().remove(foldersBlockTab);
                        foldersBlockTab = null;
                    });
        }
    }

    public void openPhraseTemplatesBlockForm() {
        if (phraseTemplatesBlockTab != null && checkNotNull(mainForm.getTabs()).getTabs().contains(phraseTemplatesBlockTab)) {
            checkNotNull(mainForm.getTabs()).getSelectionModel().select(phraseTemplatesBlockTab);
        } else {
            Block existingPhraseTemplatesBlock = phraserDB.getLastPhraseTemplatesBlock();
            if (existingPhraseTemplatesBlock != null) {
                if (JavaFxUtils.showYesNoDialog("PhraseTemplatesBlock exists, edit?") == JavaFxUtils.YesNo.NO) {
                    return;
                }
            }

            phraseTemplatesBlockTab = mainForm.openPhraseTemplatesBlockForm(existingPhraseTemplatesBlock,
                    phraserDB,
                    phraseTemplatesBlock -> {
                        int blockId;
                        int version = 1;
                        Block lastPhraseTemplatesBlock = phraserDB.getLastPhraseTemplatesBlock();
                        if (lastPhraseTemplatesBlock != null) {
                            blockId = checkNotNull(lastPhraseTemplatesBlock.phraseTemplatesBlock()).blockId();
                            version = lastPhraseTemplatesBlock.phraseTemplatesBlock().version() + 1;
                        } else {
                            blockId = phraserDB.getNextBlockId();
                        }

                        PhraseTemplatesBlock blockWithVersionAndEntropy = ImmutablePhraseTemplatesBlock.builder()
                                .from(phraseTemplatesBlock)
                                .blockId(blockId)
                                .version(version)
                                .build();

                        Block block = Block.create(blockWithVersionAndEntropy);
                        addBlock(block);

                        checkNotNull(mainForm.getTabs()).getTabs().remove(phraseTemplatesBlockTab);
                        phraseTemplatesBlockTab = null;
                    });
        }
    }

    public void updateBlockAction() {
        //
    }

    public void defragmentDBAction() {
        //
    }

    public void exportDBAction() {
        //
    }
}
