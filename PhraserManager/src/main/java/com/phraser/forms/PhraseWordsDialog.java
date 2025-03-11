package com.phraser.forms;

import com.phraser.db.PhraseBlock;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class PhraseWordsDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(PhraseWordsDialog.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom(); // Single instance

    @Nullable Stage stage;

    @Nullable
    PhraseBlockForm.UIPhraseHistory phraseHistory;

    public static class DialogWord {
        public final String name;
        public final String value;
        public final boolean isGenerateable;
        public final @Nullable List<char[]> symbolSets;
        /** Incompatible words may occur when PhraseTemplates changes for phrases.
         * The values of such words are shown as a reference  */
        public final boolean isIncompatible;
        public final boolean isUserEditable;
        public final boolean isViewable;

        public final int minLength;
        public final int maxLength;

        @Nullable String newValue;

        public DialogWord(String name, String value, int minLength, int maxLength,
                          boolean isUserEditable, boolean isGenerateable, boolean isViewable,
                          @Nullable List<char[]> symbolSets, boolean isIncompatible) {
            this.name = name;
            this.value = value;
            this.isGenerateable = isGenerateable;
            this.isViewable = isViewable;
            this.symbolSets = symbolSets;
            this.isUserEditable = isUserEditable;
            this.isIncompatible = isIncompatible;
            this.minLength = minLength;
            this.maxLength = maxLength;
        }

        @Nullable
        public String getNewValue() {
            return newValue;
        }
    }

    @Nullable
    public PhraseBlockForm.UIPhraseHistory getPhraseHistory() {
        return phraseHistory;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
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
            Label keyLabel = new Label(word.name + (StringUtils.isBlank(word.name) ? "" : ":"));
            GridPane.setMargin(keyLabel, new javafx.geometry.Insets(0, 10, 0, 0)); // Right margin of 10px

            checkNotNull(wordsGridPane).add(keyLabel, 0, row);

            if (word.isIncompatible) {
                keyLabel.setStyle("-fx-text-fill: red;");

                TextField valueTextField = new TextField(word.value);
                valueTextField.editableProperty().set(false);
                valueTextField.setStyle("-fx-text-fill: red; -fx-background-color: grey;");
                wordsGridPane.add(valueTextField, 1, row);
                wordTextFields.add(valueTextField);
            } else {
                TextField valueTextField;
                if (word.isViewable) {
                    valueTextField = new TextField(word.value);
                } else {
                    valueTextField = new PasswordField();
                    valueTextField.textProperty().set(word.value);
                }
                if (!word.isUserEditable) {
                    valueTextField.editableProperty().set(false);
                    valueTextField.setStyle("-fx-background-color: lightgrey;");
                }
                wordsGridPane.add(valueTextField, 1, row);
                wordTextFields.add(valueTextField);

                if (word.isGenerateable) {
                    Button generateButton = new Button("Generate");
                    GridPane.setMargin(generateButton, new javafx.geometry.Insets(0, 0, 0, 10));
                    wordsGridPane.add(generateButton, 2, row);
                    generateButton.setOnAction(
                            event -> {
                                String randomString = generateRandomString(checkNotNull(word.symbolSets),
                                    word.minLength, word.maxLength);
                                    valueTextField.textProperty().set(randomString);
                            });
                }
            }
        }
    }

    public void okClose() {
        try {
            //TODO: create phrase history
            phraseHistory = null;
            checkNotNull(stage).close();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "PhraseWordsDialog close Error: " + e, ButtonType.OK);
            LOGGER.error("PhraseWordsDialog close Error:", e);
            alert.showAndWait();
        }
    }

    public static String generateRandomString(List<char[]> symbolSets, int minLength, int maxLength) {
        Set<Character> symbolSet = new HashSet<>();
        for (char[] symbolArray : symbolSets) {
            for (char c : symbolArray) {
                symbolSet.add(c);
            }
        }
        List<Character> symbols = symbolSet.stream().toList();

        // Generate a random length between minLength and maxLength
        int length = SECURE_RANDOM.nextInt(maxLength - minLength + 1) + minLength;

        StringBuilder randomString = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            // Randomly select a character from the list
            char randomChar = symbols.get(SECURE_RANDOM.nextInt(symbols.size()));
            randomString.append(randomChar);
        }

        return randomString.toString();
    }
}
