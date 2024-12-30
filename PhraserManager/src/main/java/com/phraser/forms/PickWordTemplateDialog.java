package com.phraser.forms;

import com.phraser.db.PhraseTemplatesBlock;
import com.phraser.db.SymbolSetsBlock;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class PickWordTemplateDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(SymbolSetDialog.class);

    @Nullable Stage stage;

    @Nullable PhraseTemplatesBlock.WordTemplate wordTemplate;
    @Nullable @FXML TableView<PhraseTemplatesBlock.WordTemplate> wordTemplateTableView;

    public PickWordTemplateDialog(List<PhraseTemplatesBlock.WordTemplate> wordTemplates) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("PickWordTemplateDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        ObservableList<PhraseTemplatesBlock.WordTemplate> list = FXCollections.observableArrayList();
        list.addAll(wordTemplates);
        checkNotNull(wordTemplateTableView).itemsProperty().set(list);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Nullable
    public PhraseTemplatesBlock.WordTemplate getWordTemplate() {
        return wordTemplate;
    }

    public void okClose() {
        try {
            wordTemplate = checkNotNull(wordTemplateTableView).getSelectionModel().getSelectedItem();
            checkNotNull(stage).close();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "PickWordTemplateDialog close Error: " + e, ButtonType.OK);
            LOGGER.error("PickWordTemplateDialog close Error:", e);
            alert.showAndWait();
        }
    }
}
