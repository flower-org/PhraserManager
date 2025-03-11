package com.phraser.forms;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class PhraseWordsDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(PhraseWordsDialog.class);

    public static class DialogWord {
        public final String name;
        public final String value;
        public final boolean isGenerateable;
        /** Incompatible words may occur when PhraseTemplates changes for phrases.
         * The values of such words are shown as a reference  */
        public final boolean isIncompatible;

        @Nullable String newValue;

        public DialogWord(String name, String value, boolean isGenerateable, boolean isIncompatible) {
            this.name = name;
            this.value = value;
            this.isGenerateable = isGenerateable;
            this.isIncompatible = isIncompatible;
        }

        @Nullable
        public String getNewValue() {
            return newValue;
        }
    }

    @FXML @Nullable GridPane wordsGridPane;

    final List<TextField> wordTextFields;

    public PhraseWordsDialog(List<DialogWord> words) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("PhraseWordsDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        wordTextFields = new ArrayList<>();
        for (int row = 0; row < words.size(); row++) {
            DialogWord word = words.get(row);
            Label keyLabel = new Label(word.name);
            TextField valueTextField = new TextField(word.value);
            Button actionButton = new Button("Action");

            checkNotNull(wordsGridPane).add(keyLabel, 0, row);
            wordsGridPane.add(valueTextField, 1, row);
            wordsGridPane.add(actionButton, 2, row);

            wordTextFields.add(valueTextField);
        }
    }
}
