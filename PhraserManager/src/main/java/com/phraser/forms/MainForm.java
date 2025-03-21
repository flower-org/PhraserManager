package com.phraser.forms;

import com.flower.fxutils.JavaFxUtils;
import com.flower.fxutils.ModalWindow;
import com.phraser.db.Block;
import com.phraser.db.KeyBlock;
import com.phraser.db.PhraseBlock;
import com.phraser.db.PhraseTemplatesBlock;
import com.phraser.db.SymbolSetsBlock;
import com.phraser.db.FoldersBlock;
import com.phraser.db.PhraserDB;
import com.phraser.dbcodec.DbFileManager;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

public class MainForm {
    final static Logger LOGGER = LoggerFactory.getLogger(MainForm.class);

    final static String UNTITLED_DB = "Untitled";
    final static String YES = "Yes";
    final static String NO = "No";
    final static String CANCEL = "Cancel";

    @Nullable Stage mainStage;
    @FXML @Nullable Label infoLabel;
    @FXML @Nullable TabPane tabs;

    public MainForm() {
        //This form is created automatically.
        //No need to load fxml explicitly
    }

    @Nullable
    public TabPane getTabs() {
        return tabs;
    }

    public void setMainStage(@Nullable Stage mainStage) {
        this.mainStage = mainStage;
    }

    public void setStatusText(String text) {
        checkNotNull(infoLabel).setText(text);
    }

    public void newDb() {
        String res = JavaFxUtils.showCustomDialog("New DB", "New DB",
                "Initialize default DB configuration?", YES, NO, CANCEL);
        boolean initDefaultConfig;
        if (YES.equals(res)) {
            initDefaultConfig = true;
        } else if (NO.equals(res)) {
            initDefaultConfig = false;
        } else {
            return;
        }

        PhraserDbForm phraserDbForm = new PhraserDbForm(this, UNTITLED_DB, initDefaultConfig);
        phraserDbForm.setStage(checkNotNull(mainStage));
        final Tab tab = new Tab(UNTITLED_DB, phraserDbForm);
        tab.setClosable(true);
        phraserDbForm.setTab(tab);

        addTab(tab);
    }

    public void importDb() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Phraser Database files (*.phr)", "*.phr"));
            fileChooser.setTitle("Import Database");
            File dbFile = fileChooser.showOpenDialog(checkNotNull(mainStage));
            if (dbFile == null) { return; }

            EnterPasswordDialog enterPasswordDialog = new EnterPasswordDialog();
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(mainStage),
                    stage -> { enterPasswordDialog.setStage(stage); return enterPasswordDialog; },
                    "Enter Database Password");

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            String password = enterPasswordDialog.getPassword();
                            if (password != null) {
                                List<Block> db = DbFileManager.loadBlocksFromFile(password, dbFile);

                                PhraserDbForm phraserDbForm = new PhraserDbForm(this, db);
                                phraserDbForm.setStage(checkNotNull(mainStage));
                                final Tab tab = new Tab(UNTITLED_DB, phraserDbForm);
                                tab.setClosable(true);
                                phraserDbForm.setTab(tab);

                                addTab(tab);
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

    public Tab openKeyBlockForm(@Nullable Block keyBlock, PhraserDB phraserDB, Consumer<KeyBlock> keyBlockCallback) {
        KeyBlockForm keyBlockForm = new KeyBlockForm(keyBlock, phraserDB, keyBlockCallback);
        final Tab tab = new Tab("Key Block", keyBlockForm);
        tab.setClosable(true);

        addTab(tab);
        return tab;
    }

    public Tab openSymbolSetsBlockForm(@Nullable Block symbolSetsBlock, Consumer<SymbolSetsBlock> symbolSetsBlockCallback) {
        SymbolSetsBlockForm symbolSetsBlockForm = new SymbolSetsBlockForm(symbolSetsBlock, symbolSetsBlockCallback);
        symbolSetsBlockForm.setStage(checkNotNull(mainStage));
        final Tab tab = new Tab("Symbol Sets Block", symbolSetsBlockForm);
        tab.setClosable(true);

        addTab(tab);
        return tab;
    }

    public Tab openFoldersBlockForm(@Nullable Block foldersBlock, PhraserDB phraserDB, Consumer<FoldersBlock> foldersBlockCallback) {
        FoldersBlockForm foldersBlockForm = new FoldersBlockForm(foldersBlock, phraserDB, foldersBlockCallback);
        foldersBlockForm.setStage(checkNotNull(mainStage));
        final Tab tab = new Tab("Folders Block", foldersBlockForm);
        tab.setClosable(true);

        addTab(tab);
        return tab;
    }

    public Tab openPhraseTemplatesBlockForm(@Nullable Block phraseTemplatesBlock,
                                            PhraserDB phraserDB, Consumer<PhraseTemplatesBlock> phraseTemplatesBlockCallback) {
        PhraseTemplatesBlockForm phraseTemplatesBlockForm = new PhraseTemplatesBlockForm(phraseTemplatesBlock,
                () -> {
                    Block symbolSetsBlock = phraserDB.getLastSymbolSetBlock();
                    if (symbolSetsBlock != null && symbolSetsBlock.symbolSetsBlock() != null) {
                        return symbolSetsBlock.symbolSetsBlock().symbolSets();
                    }
                    return null;
                },
                phraseTemplatesBlockCallback);
        phraseTemplatesBlockForm.setStage(checkNotNull(mainStage));
        final Tab tab = new Tab("Phrase Templates Block", phraseTemplatesBlockForm);
        tab.setClosable(true);

        addTab(tab);
        return tab;
    }

    public Tab openPhraseBlockForm(@Nullable Block phraseBlock, Block foldersDbBlock, Block phraseTemplatesDbBlock,
                                   Block symbolSetsBlock, PhraserDB phraserDB, Consumer<PhraseBlock> phraseBlockCallback) {
        PhraseBlockForm phraseBlockForm = new PhraseBlockForm(phraseBlock,
                checkNotNull(foldersDbBlock.foldersBlock()),
                checkNotNull(phraseTemplatesDbBlock.phraseTemplatesBlock()),
                checkNotNull(symbolSetsBlock.symbolSetsBlock()),
                phraserDB,
                phraseBlockCallback);
        phraseBlockForm.setStage(checkNotNull(mainStage));
        String phraseBlockTabName;
        if (phraseBlock != null) {
            phraseBlockTabName = "Phrase Block: " + checkNotNull(phraseBlock.phraseBlock()).phraseName();
        } else {
            phraseBlockTabName = "New Phrase Block";
        }

        final Tab tab = new Tab(phraseBlockTabName, phraseBlockForm);
        tab.setClosable(true);

        addTab(tab);
        return tab;
    }

    public void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.NONE, "Phraser Manager v0.0.2", ButtonType.OK);
        alert.showAndWait();
    }

    void addTab(Tab tab) {
        checkNotNull(tabs).getTabs().add(tab);
        tabs.getSelectionModel().select(tab);
    }

    public void quit() { checkNotNull(mainStage).close(); }

    public void closeAllTabs() {
        List<Tab> tabsToClose = new ArrayList<>(checkNotNull(tabs).getTabs());
        for (Tab tab : tabsToClose) {
            closeTab(tab);
        }
    }

    protected void closeTab(Tab tab) {
        EventHandler<Event> handler = tab.getOnCloseRequest();
        if (null != handler) {
            Event event = new Event(Tab.TAB_CLOSE_REQUEST_EVENT);
            handler.handle(event);
            if (!event.isConsumed()) {
                tab.getTabPane().getTabs().remove(tab);
            }
        } else {
            tab.getTabPane().getTabs().remove(tab);
        }
    }

    public void openDbInClientMode() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Phraser Database files (*.phr)", "*.phr"));
            fileChooser.setTitle("Open Database in Client Mode");
            File dbFile = fileChooser.showOpenDialog(checkNotNull(mainStage));
            if (dbFile == null) { return; }

            EnterPasswordDialog enterPasswordDialog = new EnterPasswordDialog();
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(mainStage),
                    stage -> { enterPasswordDialog.setStage(stage); return enterPasswordDialog; },
                    "Enter Database Password");

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            String password = enterPasswordDialog.getPassword();
                            if (password != null) {
                                ClientModeForm clientModeForm = new ClientModeForm(this, password, dbFile);
                                clientModeForm.setStage(checkNotNull(mainStage));

                                final Tab tab = new Tab(UNTITLED_DB, clientModeForm);
                                tab.setClosable(true);
                                clientModeForm.setTab(tab);

                                addTab(tab);
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error opening DB in client mode: " + e, ButtonType.OK);
                            LOGGER.error("Error opening DB in client mode: ", e);
                            alert.showAndWait();
                        }
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error opening DB in client mode: " + e, ButtonType.OK);
            LOGGER.error("Error opening DB in client mode: ", e);
            alert.showAndWait();
        }
    }

    public void downloadDb() {
        // TODO: download DB from Phraser token via USB serial and save it to file
    }

    public void uploadDb() {
        // TODO: upload DB to Phraser token via USB serial
    }
}
