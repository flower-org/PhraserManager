package com.phraser.forms;

import com.phraser.db.Block;
import com.phraser.db.KeyBlock;
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

public class MainForm {
    @Nullable Stage mainStage;
    @FXML @Nullable Label serverInfoLabel;
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
        checkNotNull(serverInfoLabel).setText(text);
    }

    public void newDb() {
        // TODO: request DB name via dialog
        String dbName = "New DB #" + (++newDbCount);

        PhraserDbForm phraserDbForm = new PhraserDbForm(this, dbName);
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
