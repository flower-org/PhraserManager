package com.phraser.forms;

import com.flower.fxutils.JavaFxUtils;
import com.phraser.db.Block;
import com.phraser.db.KeyBlock;
import com.phraser.db.PhraseBlock;
import com.phraser.db.PhraseTemplatesBlock;
import com.phraser.db.SymbolSetsBlock;
import com.phraser.db.FoldersBlock;
import com.phraser.db.PhraserDB;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import javax.annotation.Nullable;

import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.flower.fxutils.JavaFxUtils.YesNo.YES;

public class MainForm {
    @Nullable Stage mainStage;
    @FXML @Nullable Label infoLabel;
    @FXML @Nullable TabPane tabs;
    int testFormCount = 0;
    int newDbCount = 0;

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
        boolean initDefaultConfig = false;
        if (YES == JavaFxUtils.showYesNoDialog("New DB", "Initialize default DB configuration?")) {
            initDefaultConfig = true;
        }

        String dbName = "New DB #" + (++newDbCount);

        PhraserDbForm phraserDbForm = new PhraserDbForm(this, dbName, initDefaultConfig);
        phraserDbForm.setStage(checkNotNull(mainStage));
        final Tab tab = new Tab(dbName, phraserDbForm);
        tab.setClosable(true);
        phraserDbForm.setTab(tab);

        addTab(tab);
    }

    public Tab openKeyBlockForm(@Nullable Block keyBlock, PhraserDB phraserDB, Consumer<KeyBlock> keyBlockCallback) {
        KeyBlockForm keyBlockForm = new KeyBlockForm(keyBlock, phraserDB, keyBlockCallback);
//        keyBlockForm.setStage(checkNotNull(mainStage));
        final Tab tab = new Tab("Key Block", keyBlockForm);
        tab.setClosable(true);

        addTab(tab);
        return tab;
    }

    public Tab openSymbolSetsBlockForm(@Nullable Block symbolSetsBlock, PhraserDB phraserDB, Consumer<SymbolSetsBlock> symbolSetsBlockCallback) {
        SymbolSetsBlockForm symbolSetsBlockForm = new SymbolSetsBlockForm(symbolSetsBlock, phraserDB, symbolSetsBlockCallback);
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
                phraserDB, phraseTemplatesBlockCallback);
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
        Alert alert = new Alert(Alert.AlertType.NONE, "Phraser Manager v0.1", ButtonType.OK);
        alert.showAndWait();
    }

    void addTab(Tab tab) {
        checkNotNull(tabs).getTabs().add(tab);
        tabs.getSelectionModel().select(tab);
    }

    public void quit() { checkNotNull(mainStage).close(); }

    public void closeAllTabs() {
        checkNotNull(tabs).getTabs().clear();
    }
}
