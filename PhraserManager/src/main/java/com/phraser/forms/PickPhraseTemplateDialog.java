package com.phraser.forms;

import com.phraser.db.PhraseTemplatesBlock;
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

public class PickPhraseTemplateDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(PickPhraseTemplateDialog.class);

    @Nullable Stage stage;

    @Nullable PhraseTemplatesBlock.PhraseTemplate phraseTemplate;
    @Nullable @FXML TableView<PhraseTemplatesBlock.PhraseTemplate> phraseTemplateTableView;

    public PickPhraseTemplateDialog(List<PhraseTemplatesBlock.PhraseTemplate> phraseTemplates,
                                    @Nullable Integer selectedPhraseTemplate) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("PickPhraseTemplateDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        ObservableList<PhraseTemplatesBlock.PhraseTemplate> list = FXCollections.observableArrayList();
        list.addAll(phraseTemplates);
        checkNotNull(phraseTemplateTableView).itemsProperty().set(list);
        if (selectedPhraseTemplate != null) {
            int selectedIndex = -1;
            for (int i = 0; i < phraseTemplates.size(); i++) {
                PhraseTemplatesBlock.PhraseTemplate p = phraseTemplates.get(i);
                if (p.phraseTemplateId() == selectedPhraseTemplate) {
                    selectedIndex = i;
                    break;
                }
            }
            if (selectedIndex > -1) {
                phraseTemplateTableView.getSelectionModel().select(selectedIndex);
            }
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Nullable
    public PhraseTemplatesBlock.PhraseTemplate getPhraseTemplates() {
        return phraseTemplate;
    }

    public void okClose() {
        try {
            phraseTemplate = checkNotNull(phraseTemplateTableView).getSelectionModel().getSelectedItem();
            checkNotNull(stage).close();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "PickPhraseTemplateDialog close Error: " + e, ButtonType.OK);
            LOGGER.error("PickPhraseTemplateDialog close Error:", e);
            alert.showAndWait();
        }
    }
}
