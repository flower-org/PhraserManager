package com.phraser.forms;

import com.phraser.db.ImmutableSymbolSet;
import com.phraser.db.SymbolSetsBlock;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public class SymbolSetDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(SymbolSetDialog.class);
    final static String NEW_SYMBOL_SET = "[NEW SYMBOL SET]";

    @FXML @Nullable TextField idTextField;
    @FXML @Nullable TextField nameTextField;
    @FXML @Nullable TextField symbolSetTextField;
    @FXML @Nullable TextField regexTextField;

    @Nullable Stage stage;

    @Nullable SymbolSetsBlock.SymbolSet symbolSet;

    public SymbolSetDialog(@Nullable SymbolSetsBlock.SymbolSet originalSymbolSet) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SymbolSetDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        checkNotNull(symbolSetTextField).setTextFormatter(new TextFormatter<>(change -> {
                String newText = change.getControlNewText();
                boolean hasDuplicateSymbols = newText.chars().distinct().count() < newText.length();
                if (hasDuplicateSymbols) {
                    return null;
                }
                return change;
            }
        ));

        if (originalSymbolSet != null) {
            checkNotNull(idTextField).textProperty().set(Integer.toString(originalSymbolSet.symbolSetId()));
            checkNotNull(nameTextField).textProperty().set(originalSymbolSet.symbolSetName());
            checkNotNull(symbolSetTextField).textProperty().set(new String(originalSymbolSet.symbolSet()));
        } else {
            checkNotNull(idTextField).textProperty().set(NEW_SYMBOL_SET);
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Nullable
    public SymbolSetsBlock.SymbolSet getSymbolSet() {
        return symbolSet;
    }

    public void okClose() {
        try {
            String idStr = checkNotNull(idTextField).textProperty().get();
            int symbolSetId = NEW_SYMBOL_SET.equals(idStr) ? -1 : Integer.parseInt(idStr);

            String symbolSetName = checkNotNull(nameTextField).textProperty().get();
            if (symbolSetName.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "SymbolSet Name can't be empty.", ButtonType.OK);
                alert.showAndWait();
                return;
            }

            String symbolSetStr = checkNotNull(symbolSetTextField).textProperty().get();
            if (symbolSetStr.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "SymbolSet can't be empty.", ButtonType.OK);
                alert.showAndWait();
                return;
            }
            char[] symbolSetChars = symbolSetStr.toCharArray();

            symbolSet = ImmutableSymbolSet.builder()
                    .symbolSetId(symbolSetId)
                    .symbolSetName(symbolSetName)
                    .symbolSet(symbolSetChars)
                    .build();

            checkNotNull(stage).close();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "SymbolSetDialog close Error: " + e, ButtonType.OK);
            LOGGER.error("SymbolSetDialog close Error:", e);
            alert.showAndWait();
        }
    }

    public void regex() {
        try {
            String regex = checkNotNull(regexTextField).getText();
            String allowedSymbols = regexToAllowedSymbols(regex);
            checkNotNull(symbolSetTextField).textProperty().set(allowedSymbols);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error building regex: " + e, ButtonType.OK);
            LOGGER.error("Error building regex: ", e);
            alert.showAndWait();
        }
    }

    public String regexToAllowedSymbols(String regex) {
        StringBuilder builder = new StringBuilder();

        Pattern pattern = Pattern.compile(regex);
        for (int i = 0; i <= 0x7F; i++) {
            char c = (char)i;
            String charAsString = String.valueOf(c);

            if (pattern.matcher(charAsString).matches()) {
                builder.append(c);
            }
        }

        return builder.toString();
    }
}
