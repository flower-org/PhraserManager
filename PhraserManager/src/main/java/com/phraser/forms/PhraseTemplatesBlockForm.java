package com.phraser.forms;

import com.phraser.ModalWindow;
import com.phraser.db.Block;
import com.phraser.db.ImmutablePhraseTemplate;
import com.phraser.db.ImmutablePhraseTemplatesBlock;
import com.phraser.db.ImmutableSymbolSet;
import com.phraser.db.ImmutableWordTemplate;
import com.phraser.db.PhraseTemplatesBlock;
import com.phraser.db.PhraserDB;
import com.phraser.db.SymbolSetsBlock;
import com.phraser.utils.PhraserUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.phraser.db.Block.DATA_BLOCK_SIZE;
import static com.phraser.forms.PhraserDbForm.NEW_BLOCK;

public class PhraseTemplatesBlockForm extends AnchorPane {
    final static Logger LOGGER = LoggerFactory.getLogger(PhraseTemplatesBlockForm.class);

    final static byte GENERATEABLE = 1;
    final static byte TYPEABLE = 2;
    final static byte VIEWABLE = 4;
    final static byte USER_EDITABLE = 8;

    @FXML @Nullable Button addUpdateWordTemplateButton;

    @FXML @Nullable TextField blockIdTextField;
    @FXML @Nullable TextField versionTextField;
    @FXML @Nullable TextField blockSizeTextField;

    @FXML @Nullable Button newPhraseTemplateButton;
    @FXML @Nullable Button removePhraseTemplateButton;

    @FXML @Nullable Button addUpdatePhraseTemplateButton;

    @FXML @Nullable TableView<PhraseTemplatesBlock.PhraseTemplate> phraseTemplatesTableView;
    ObservableList<PhraseTemplatesBlock.PhraseTemplate> phraseTemplates;
    @Nullable PhraseTemplatesBlock.PhraseTemplate phraseTemplate = null;

    @FXML @Nullable TextField phraseTemplateIdTextField;
    @FXML @Nullable TextField phraseTemplateNameTextField;
    @FXML @Nullable TableView<PhraseTemplatesBlock.WordTemplate> phraseTemplateWordsTableView;
    ObservableList<PhraseTemplatesBlock.WordTemplate> phraseTemplateWords;

    @FXML @Nullable Button addPhraseTemplateWordButton;
    @FXML @Nullable Button removePhraseTemplateWordButton;

    @FXML @Nullable Button newWordTemplateButton;
    @FXML @Nullable Button removeWordTemplateButton;
    @FXML @Nullable Button updateWordTemplateButton;

    @FXML @Nullable TableView<PhraseTemplatesBlock.WordTemplate> wordTemplatesTableView;
    ObservableList<PhraseTemplatesBlock.WordTemplate> wordTemplates;
    @Nullable PhraseTemplatesBlock.WordTemplate oldWordTemplate = null;

    @FXML @Nullable TextField wordTemplateIdTextField;
    @FXML @Nullable TextField wordTemplateNameTextField;
    @FXML @Nullable ComboBox<String> wordTemplateIconComboBox;
    @FXML @Nullable TextField wordTemplateMinLengthTextField;
    @FXML @Nullable TextField wordTemplateMaxLengthTextField;

    @FXML @Nullable CheckBox wordTemplateIsGenerateableCheckBox;
    @FXML @Nullable CheckBox wordTemplateIsTypeableCheckBox;
    @FXML @Nullable CheckBox wordTemplateIsViewableCheckBox;
    @FXML @Nullable CheckBox wordTemplateIsUserEditableCheckBox;

    @FXML @Nullable TableView<SymbolSetsBlock.SymbolSet> wordTemplateSymbolSetsTableView;
    ObservableList<SymbolSetsBlock.SymbolSet> wordTemplateSymbolSets;

    @FXML @Nullable Button addWordTemplateSymbolSetButton;
    @FXML @Nullable Button removeWordTemplateSymbolSetButton;

    @Nullable Stage stage;

    @Nullable final Block phraseTemplatesBlock;
    final PhraserDB phraserDB;

    final Consumer<PhraseTemplatesBlock> phraseTemplatesBlockCallback;
    //TODO: phraserDB.getLastSymbolSetBlock()

    int nextWordTemplateId = 0;
    int nextPhraseTemplateId = 0;

    public PhraseTemplatesBlockForm(@Nullable Block phraseTemplatesBlock,
                                    PhraserDB phraserDB,
                                    Consumer<PhraseTemplatesBlock> phraseTemplatesBlockCallback) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("PhraseTemplatesBlockForm.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        checkNotNull(wordTemplateMinLengthTextField).setTextFormatter(new TextFormatter<>(change -> {
                String newText = change.getControlNewText();
                if (newText.length() <= 3 && newText.matches("[0-9]*")) {
                    change.setText(change.getText().toLowerCase());
                    return change;
                }
                return null;
            }
        ));
        checkNotNull(wordTemplateMaxLengthTextField).setTextFormatter(new TextFormatter<>(change -> {
                String newText = change.getControlNewText();
                if (newText.length() <= 3 && newText.matches("[0-9]*")) {
                    change.setText(change.getText().toLowerCase());
                    return change;
                }
                return null;
            }
        ));

        this.phraseTemplatesBlock = phraseTemplatesBlock;
        if (phraseTemplatesBlock == null) {
            checkNotNull(blockIdTextField).setText(NEW_BLOCK);
            checkNotNull(versionTextField).setText(NEW_BLOCK);
        } else {
            checkNotNull(blockIdTextField).setText(Integer.toString(checkNotNull(phraseTemplatesBlock.phraseTemplatesBlock()).blockId()));
            checkNotNull(versionTextField).setText(Integer.toString(checkNotNull(phraseTemplatesBlock.phraseTemplatesBlock()).version()));
        }

        this.phraserDB = phraserDB;
        this.phraseTemplatesBlockCallback = phraseTemplatesBlockCallback;

        this.wordTemplateSymbolSets = FXCollections.observableArrayList();
        this.phraseTemplateWords = FXCollections.observableArrayList();

        checkNotNull(wordTemplatesTableView).getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                oldWordTemplate = newValue;
                showWordTemplate(oldWordTemplate);
            }
        });
        checkNotNull(phraseTemplatesTableView).getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                phraseTemplate = newValue;
                showPhraseTemplate(phraseTemplate);
            }
        });

        if (phraseTemplatesBlock == null) {
            newWordTemplate();
            newPhraseTemplate();

            this.phraseTemplates = FXCollections.observableArrayList();
            checkNotNull(phraseTemplatesTableView).itemsProperty().set(phraseTemplates);
            this.wordTemplates = FXCollections.observableArrayList();
            checkNotNull(wordTemplatesTableView).itemsProperty().set(wordTemplates);
        } else {
            List<PhraseTemplatesBlock.WordTemplate> wordTemplates =
                    checkNotNull(phraseTemplatesBlock.phraseTemplatesBlock()).wordTemplates();
            if (wordTemplates == null || wordTemplates.isEmpty()) {
                this.wordTemplates = FXCollections.observableArrayList();
                newWordTemplate();
            } else {
                this.wordTemplates = FXCollections.observableArrayList();
                for (PhraseTemplatesBlock.WordTemplate wt : wordTemplates) {
                    addWordTemplate(wt, false);
                }

                this.wordTemplateSymbolSets = FXCollections.observableArrayList();
                checkNotNull(wordTemplateSymbolSetsTableView).itemsProperty().set(this.wordTemplateSymbolSets);
            }
            checkNotNull(wordTemplatesTableView).itemsProperty().set(this.wordTemplates);

            List<PhraseTemplatesBlock.PhraseTemplate> phraseTemplates =
                    checkNotNull(phraseTemplatesBlock.phraseTemplatesBlock()).phraseTemplates();
            if (phraseTemplates == null || phraseTemplates.isEmpty()) {
                this.phraseTemplates = FXCollections.observableArrayList();
                newPhraseTemplate();
            } else {
                this.phraseTemplates = FXCollections.observableArrayList();
                for (PhraseTemplatesBlock.PhraseTemplate pt : phraseTemplates) {
                    addPhraseTemplate(pt, false);
                }

                this.phraseTemplateWords = FXCollections.observableArrayList();
                checkNotNull(phraseTemplateWordsTableView).itemsProperty().set(this.phraseTemplateWords);
            }
            checkNotNull(phraseTemplatesTableView).itemsProperty().set(this.phraseTemplates);
        }

        checkNotNull(wordTemplateMinLengthTextField).textProperty().set("10");
        //maxLength
        checkNotNull(wordTemplateMaxLengthTextField).textProperty().set("35");

        updateBlockSize();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    void updateBlockSize() {
        Block block = Block.create(formPhraseTemplatesBlock(false));

        int bufferLength = block.toFlatBufBlock().length;
        checkNotNull(blockSizeTextField).textProperty().set(Integer.toString(bufferLength));
    }

    public void saveToDb() {
        PhraseTemplatesBlock newPhraseTemplatesBlock = formPhraseTemplatesBlock(true);
        if (newPhraseTemplatesBlock.phraseTemplates() == null || newPhraseTemplatesBlock.phraseTemplates().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Block must contain PhraseTemplates", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        Block block = Block.create(newPhraseTemplatesBlock);
        int bufferLength = block.toFlatBufBlock().length;

        if (bufferLength > DATA_BLOCK_SIZE) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Block size can't exceed "+DATA_BLOCK_SIZE+" bytes", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        // This call will close the form and process the formed block
        phraseTemplatesBlockCallback.accept(newPhraseTemplatesBlock);
    }

    public void newWordTemplate() {
        oldWordTemplate = null;

        //id
        checkNotNull(wordTemplateIdTextField).textProperty().set("[NEW WORD TEMPLATE]");
        //name
        checkNotNull(wordTemplateNameTextField).textProperty().set("");
        //icon
        checkNotNull(wordTemplateIconComboBox).getSelectionModel().select(0);
        //minLength
        checkNotNull(wordTemplateMinLengthTextField).textProperty().set("10");
        //maxLength
        checkNotNull(wordTemplateMaxLengthTextField).textProperty().set("35");
        //permissions
        checkNotNull(wordTemplateIsGenerateableCheckBox).selectedProperty().addListener((observable, oldValue, newValue) -> {
            boolean isGenerateable = newValue;
            disableSymbolSets(!isGenerateable);
        });
        checkNotNull(wordTemplateIsGenerateableCheckBox).selectedProperty().set(true);
        checkNotNull(wordTemplateIsTypeableCheckBox).selectedProperty().set(true);
        checkNotNull(wordTemplateIsViewableCheckBox).selectedProperty().set(false);
        checkNotNull(wordTemplateIsUserEditableCheckBox).selectedProperty().set(true);

        //symbol sets
        wordTemplateSymbolSets = FXCollections.observableArrayList();
        checkNotNull(wordTemplateSymbolSetsTableView).itemsProperty().set(wordTemplateSymbolSets);
    }

    public void removeWordTemplate() {
        PhraseTemplatesBlock.WordTemplate selectedItem =
                checkNotNull(wordTemplatesTableView).getSelectionModel().getSelectedItem();
        wordTemplates.remove(selectedItem);
        if (wordTemplates.isEmpty()) {
            newWordTemplate();
        }
    }

    void disableSymbolSets(boolean disable) {;
        checkNotNull(addWordTemplateSymbolSetButton).setDisable(disable);
        checkNotNull(removeWordTemplateSymbolSetButton).setDisable(disable);
        checkNotNull(wordTemplateSymbolSetsTableView).setDisable(disable);
    }

    public void showWordTemplate(PhraseTemplatesBlock.WordTemplate wordTemplate) {
        //id
        checkNotNull(wordTemplateIdTextField).textProperty().set(Integer.toString(wordTemplate.wordTemplateId()));
        //name
        checkNotNull(wordTemplateNameTextField).textProperty().set(wordTemplate.wordTemplateName());
        //icon
        checkNotNull(wordTemplateIconComboBox).getSelectionModel().select(wordTemplate.icon().toString());
        //minLength
        checkNotNull(wordTemplateMinLengthTextField).textProperty().set(Integer.toString(wordTemplate.minLength()));
        //maxLength
        checkNotNull(wordTemplateMaxLengthTextField).textProperty().set(Integer.toString(wordTemplate.maxLength()));
        //permissions
        //TODO: DRY
        checkNotNull(wordTemplateIsGenerateableCheckBox).selectedProperty().addListener((observable, oldValue, newValue) -> {
            boolean isGenerateable = newValue;
            disableSymbolSets(!isGenerateable);
        });
        byte permissions = wordTemplate.permissions();
        boolean isGenerateable = (permissions & GENERATEABLE) == GENERATEABLE;
        disableSymbolSets(!isGenerateable);
        checkNotNull(wordTemplateIsGenerateableCheckBox).selectedProperty().set(isGenerateable);
        checkNotNull(wordTemplateIsTypeableCheckBox).selectedProperty().set((permissions & TYPEABLE) == TYPEABLE);
        checkNotNull(wordTemplateIsViewableCheckBox).selectedProperty().set((permissions & VIEWABLE) == VIEWABLE);
        checkNotNull(wordTemplateIsUserEditableCheckBox).selectedProperty().set((permissions & USER_EDITABLE) == USER_EDITABLE);

        //symbol sets
        wordTemplateSymbolSets = FXCollections.observableArrayList();

        Block symbolSetsBlock = phraserDB.getLastSymbolSetBlock();
        if (symbolSetsBlock != null && symbolSetsBlock.symbolSetsBlock() != null) {
            Map<Integer, SymbolSetsBlock.SymbolSet> symbolSetsMap = new HashMap<>();
            for (SymbolSetsBlock.SymbolSet sset : symbolSetsBlock.symbolSetsBlock().symbolSets()) {
                symbolSetsMap.put(sset.symbolSetId(), sset);
            }

            for (int symbolSetId : wordTemplate.symbolSetIds()) {
                SymbolSetsBlock.SymbolSet sset = symbolSetsMap.get(symbolSetId);
                if (sset != null) {
                    wordTemplateSymbolSets.add(sset);
                } else {
                    wordTemplateSymbolSets.add(ImmutableSymbolSet.builder()
                            .symbolSetId(symbolSetId)
                            .symbolSetName("SYMBOL SET NOT FOUND")
                            .symbolSet()
                            .build());
                }
            }
        }

        checkNotNull(wordTemplateSymbolSetsTableView).itemsProperty().set(wordTemplateSymbolSets);
    }

    public void showPhraseTemplate(PhraseTemplatesBlock.PhraseTemplate phraseTemplate) {
        //id
        checkNotNull(phraseTemplateIdTextField).textProperty().set(Integer.toString(phraseTemplate.phraseTemplateId()));
        //name
        checkNotNull(phraseTemplateNameTextField).textProperty().set(phraseTemplate.phraseTemplateName());

        //symbol sets
        phraseTemplateWords = FXCollections.observableArrayList();

        Map<Integer, PhraseTemplatesBlock.WordTemplate> wordTemplateMap = new HashMap<>();
        for (PhraseTemplatesBlock.WordTemplate wTemplate : wordTemplates) {
            wordTemplateMap.put(wTemplate.wordTemplateId(), wTemplate);
        }

        for (int wordTemplateId : phraseTemplate.wordTemplateIds()) {
            PhraseTemplatesBlock.WordTemplate wTemplate = wordTemplateMap.get(wordTemplateId);
            if (wTemplate != null) {
                phraseTemplateWords.add(wTemplate);
            } else {
                phraseTemplateWords.add(ImmutableWordTemplate.builder()
                        .wordTemplateId(wordTemplateId)
                        .wordTemplateName("WORD TEMPLATE NOT FOUND")
                        .addSymbolSetIds()
                        .permissions((byte)0)
                        .icon(PhraseTemplatesBlock.Icon.X)
                        .minLength(0)
                        .maxLength(0)
                        .build());
            }
        }

        checkNotNull(phraseTemplateWordsTableView).itemsProperty().set(phraseTemplateWords);
    }

    public void newPhraseTemplate() {
        phraseTemplate = null;

        checkNotNull(phraseTemplateIdTextField).textProperty().set("[NEW PHRASE TEMPLATE]");
        checkNotNull(phraseTemplateNameTextField).textProperty().set("");

        //symbol sets
        phraseTemplateWords = FXCollections.observableArrayList();
        checkNotNull(phraseTemplateWordsTableView).itemsProperty().set(phraseTemplateWords);
    }

    public void removePhraseTemplate() {
        PhraseTemplatesBlock.PhraseTemplate selectedItem =
                checkNotNull(phraseTemplatesTableView).getSelectionModel().getSelectedItem();
        phraseTemplates.remove(selectedItem);
        if (phraseTemplates.isEmpty()) {
            newPhraseTemplate();
        }
    }

    public void addSymbolSet() {
        try {
            Block symbolSetsBlock = phraserDB.getLastSymbolSetBlock();
            if (symbolSetsBlock == null || symbolSetsBlock.symbolSetsBlock() == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Symbol Sets Block not found, please create.", ButtonType.OK);
                alert.showAndWait();
                return;
            }

            PickSymbolSetDialog pickSymbolSetDialog = new PickSymbolSetDialog(symbolSetsBlock.symbolSetsBlock());
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(stage),
                    stage -> { pickSymbolSetDialog.setStage(stage); return pickSymbolSetDialog; },
                    "Pick Symbol Set");

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            SymbolSetsBlock.SymbolSet symbolSet = pickSymbolSetDialog.getSymbolSet();
                            if (symbolSet != null) {
                                if (!wordTemplateSymbolSets.contains(symbolSet)) {
                                    wordTemplateSymbolSets.add(symbolSet);
                                }
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding known server: " + e, ButtonType.OK);
                            LOGGER.error("Error adding known server: ", e);
                            alert.showAndWait();
                        }

                        updateBlockSize();
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding known server: " + e, ButtonType.OK);
            LOGGER.error("Error adding known server: ", e);
            alert.showAndWait();
        }
    }

    public void removeSymbolSet() {
        try {
            SymbolSetsBlock.SymbolSet symbolSet = checkNotNull(wordTemplateSymbolSetsTableView).getSelectionModel().getSelectedItem();
            if (symbolSet != null) {
                wordTemplateSymbolSets.remove(symbolSet);
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding known server: " + e, ButtonType.OK);
            LOGGER.error("Error adding known server: ", e);
            alert.showAndWait();
        }
    }

    public void addUpdateWordTemplate() {
        try {
            PhraseTemplatesBlock.WordTemplate newWordTemplate = formWordTemplate();
            addWordTemplate(newWordTemplate, true);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
            LOGGER.error("Error in addUpdateWordTemplate: ", e);
            alert.showAndWait();
        }
    }

    void addWordTemplate(PhraseTemplatesBlock.WordTemplate newWordTemplate, boolean selectNewTemplate) {
        int index = -1;
        if (oldWordTemplate != null) {
            index = wordTemplates.indexOf(oldWordTemplate);
        }

        if (index == -1) {
            wordTemplates.add(newWordTemplate);
        } else {
            wordTemplates.set(index, newWordTemplate);
        }

        nextWordTemplateId = Math.max(nextWordTemplateId, newWordTemplate.wordTemplateId());

        if (selectNewTemplate) {
            oldWordTemplate = newWordTemplate;
            checkNotNull(wordTemplatesTableView).getSelectionModel().select(newWordTemplate);
        }
    }

    PhraseTemplatesBlock.WordTemplate formWordTemplate() {
        int wordTemplateId;
        if (oldWordTemplate == null) {
            wordTemplateId = nextWordTemplateId + 1;
        } else {
            wordTemplateId = oldWordTemplate.wordTemplateId();
        }

        byte permissions = getWordPermissions();
        PhraseTemplatesBlock.Icon icon = PhraseTemplatesBlock.Icon.valueOf(checkNotNull(wordTemplateIconComboBox).getSelectionModel().getSelectedItem());
        int minLength = Integer.parseInt(checkNotNull(wordTemplateMinLengthTextField).textProperty().get());
        int maxLength = Integer.parseInt(checkNotNull(wordTemplateMaxLengthTextField).textProperty().get());
        if (minLength > maxLength) {
            throw new RuntimeException("MinLength should be <= to MaxLength");
        }
        String wordTemplateName = checkNotNull(wordTemplateNameTextField).getText();
        if (StringUtils.isBlank(wordTemplateName)) {
            throw new RuntimeException("Word template name is empty");
        }

        List<Integer> symbolSetIds = new ArrayList<>();
        for (SymbolSetsBlock.SymbolSet symbolSet : wordTemplateSymbolSets) {
            symbolSetIds.add(symbolSet.symbolSetId());
        }
        if (((permissions & GENERATEABLE) == GENERATEABLE) && symbolSetIds.isEmpty()) {
            throw new RuntimeException("Generateable words should have symbol sets attached.");
        }

        return ImmutableWordTemplate.builder()
                    .wordTemplateId(wordTemplateId)
                    .permissions(permissions)
                    .icon(icon)
                    .minLength(minLength)
                    .maxLength(maxLength)
                    .wordTemplateName(wordTemplateName)
                    .symbolSetIds(symbolSetIds)
                .build();
    }

    byte getWordPermissions() {
        boolean generateableOrEditable = false;
        boolean typeableOrViewable = false;
        int getWordPermissions = 0;
        if (checkNotNull(wordTemplateIsGenerateableCheckBox).selectedProperty().get()) {
            getWordPermissions = getWordPermissions | GENERATEABLE;
            generateableOrEditable = true;
        }
        if (checkNotNull(wordTemplateIsTypeableCheckBox).selectedProperty().get()) {
            getWordPermissions = getWordPermissions | TYPEABLE;
            typeableOrViewable = true;
        }
        if (checkNotNull(wordTemplateIsViewableCheckBox).selectedProperty().get()) {
            getWordPermissions = getWordPermissions | VIEWABLE;
            typeableOrViewable = true;
        }
        if (checkNotNull(wordTemplateIsUserEditableCheckBox).selectedProperty().get()) {
            getWordPermissions = getWordPermissions | USER_EDITABLE;
            generateableOrEditable = true;
        }

        if (!generateableOrEditable) {
            throw new RuntimeException("Word should be either Generateable or UserEditable or both");
        }
        if (!typeableOrViewable) {
            throw new RuntimeException("Word should be either Typeable or Viewable or both");
        }

        return (byte)getWordPermissions;
    }

    public void addPhraseTemplateWord() {
        try {
            PickWordTemplateDialog pickWordTemplateDialog = new PickWordTemplateDialog(wordTemplates);
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(stage),
                    stage -> { pickWordTemplateDialog.setStage(stage); return pickWordTemplateDialog; },
                    "Pick Word Template");

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            PhraseTemplatesBlock.WordTemplate wordTemplate = pickWordTemplateDialog.getWordTemplate();
                            if (wordTemplate != null) {
                                if (!phraseTemplateWords.contains(wordTemplate)) {
                                    phraseTemplateWords.add(wordTemplate);
                                }
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error picking Word Template: " + e, ButtonType.OK);
                            LOGGER.error("Error picking Word Template: ", e);
                            alert.showAndWait();
                        }

                        updateBlockSize();
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error picking Word Template: " + e, ButtonType.OK);
            LOGGER.error("Error picking Word Template: ", e);
            alert.showAndWait();
        }
    }

    public void removePhraseTemplateWord() {
        PhraseTemplatesBlock.PhraseTemplate selectedItem =
                checkNotNull(phraseTemplatesTableView).getSelectionModel().getSelectedItem();
        phraseTemplates.remove(selectedItem);
        if (phraseTemplates.isEmpty()) {
            newPhraseTemplate();
        }
    }

    public void addUpdatePhraseTemplate() {
        try {
            PhraseTemplatesBlock.PhraseTemplate newPhraseTemplate = formPhraseTemplate();
            addPhraseTemplate(newPhraseTemplate, true);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
            LOGGER.error("addUpdatePhraseTemplate: ", e);
            alert.showAndWait();
        }
    }

    PhraseTemplatesBlock.PhraseTemplate formPhraseTemplate() {
        int phraseTemplateId;
        if (phraseTemplate == null) {
            phraseTemplateId = nextPhraseTemplateId + 1;
        } else {
            phraseTemplateId = phraseTemplate.phraseTemplateId();
        }

        String phraseTemplateName = checkNotNull(phraseTemplateNameTextField).getText();
        if (StringUtils.isBlank(phraseTemplateName)) {
            throw new RuntimeException("Phrase template name is empty");
        }

        if (phraseTemplateWords.isEmpty()) {
            throw new RuntimeException("Phrase doesn't have any words.");
        }
        List<Integer> wordTemplateIds = new ArrayList<>();
        for (PhraseTemplatesBlock.WordTemplate wordTemplate : phraseTemplateWords) {
            wordTemplateIds.add(wordTemplate.wordTemplateId());
        }

        return ImmutablePhraseTemplate.builder()
                    .phraseTemplateId(phraseTemplateId)
                    .phraseTemplateName(phraseTemplateName)
                    .wordTemplateIds(wordTemplateIds)
                .build();
    }

    void addPhraseTemplate(PhraseTemplatesBlock.PhraseTemplate newPhraseTemplate, boolean selectPhraseTemplate) {
        int index = -1;
        if (phraseTemplate != null) {
            index = phraseTemplates.indexOf(phraseTemplate);
        }

        if (index == -1) {
            phraseTemplates.add(newPhraseTemplate);
        } else {
            phraseTemplates.set(index, newPhraseTemplate);
        }

        nextPhraseTemplateId = Math.max(nextWordTemplateId, newPhraseTemplate.phraseTemplateId());

        if (selectPhraseTemplate) {
            phraseTemplate = newPhraseTemplate;
            checkNotNull(phraseTemplatesTableView).getSelectionModel().select(phraseTemplate);
        }
    }

    // ----------------------------------------------------------------------

    PhraseTemplatesBlock formPhraseTemplatesBlock(boolean useRealEntropy) {
        List<PhraseTemplatesBlock.PhraseTemplate> phraseTemplates = new ArrayList<>(this.phraseTemplates);
        List<PhraseTemplatesBlock.WordTemplate> wordTemplates = new ArrayList<>(this.wordTemplates);

        return ImmutablePhraseTemplatesBlock.builder()
                .blockId(phraseTemplatesBlock == null ? -1 : phraseTemplatesBlock.getBlockId())
                .version(123)
                .entropy(useRealEntropy ? PhraserUtils.generateEntropy() : 123L)
                .addAllPhraseTemplates(phraseTemplates)
                .addAllWordTemplates(wordTemplates)
                .build();
    }
}
