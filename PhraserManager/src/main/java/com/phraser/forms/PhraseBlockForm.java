package com.phraser.forms;

import com.phraser.JavaFxUtils;
import com.phraser.ModalWindow;
import com.phraser.db.Block;
import com.phraser.db.FoldersBlock;
import com.phraser.db.ImmutablePhraseBlock;
import com.phraser.db.PhraseBlock;
import com.phraser.db.PhraseTemplatesBlock;
import com.phraser.db.PhraserDB;
import com.phraser.db.SymbolSetsBlock;
import com.phraser.dbcodec.BlockEncoder;
import com.phraser.utils.PhraserUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.phraser.forms.PhraserDbForm.NEW_BLOCK;
import static com.phraser.forms.PhraseWordsDialog.DialogWord;

public class PhraseBlockForm extends AnchorPane {
    final static Logger LOGGER = LoggerFactory.getLogger(PhraseBlockForm.class);

    public static class UIPhraseHistory {
        static AtomicInteger COUNTER = new AtomicInteger(0);

        /** 0 - newest history, actual value */
        final List<UIPhraseHistory> ownerList;
        final List<UIWord> words;
        final int ordinal = COUNTER.incrementAndGet();

        public UIPhraseHistory(List<UIPhraseHistory> ownerList, List<UIWord> words) {
            this.ownerList = ownerList;
            this.words = words;
        }

        public int getIndex() {
            return ownerList.indexOf(this);
        }

        public int getOrdinal() {
            return ordinal;
        }
    }

    public static class UIWord {
        public final int wordId;
        public final String wordName;
        public final String value;

        public final boolean isTypeable;
        public final boolean isViewable;

        public UIWord(int wordId, String wordName, String value, boolean isTypeable, boolean isViewable) {
            this.wordId = wordId;
            this.wordName = wordName;
            this.value = value;
            this.isTypeable = isTypeable;
            this.isViewable = isViewable;
        }

        public int getWordId() {
            return wordId;
        }
        public String getWordName() {
            return wordName;
        }
        public String getValue() {
            if (isViewable) {
                return value;
            } else {
                return "*****";
            }
        }
    }

    @FXML @Nullable TextField blockIdTextField;
    @FXML @Nullable TextField versionTextField;
    @FXML @Nullable TextField blockSizeTextField;
    @FXML @Nullable TableView<UIPhraseHistory> phraseHistoryTableView;
    @FXML @Nullable TableView<UIWord> phraseHistoryWordsTableView;
    @FXML @Nullable TextField folderTextField;
    @FXML @Nullable TextField phraseTemplateTextField;
    @FXML @Nullable TableColumn<UIWord, String> copyColumn;

    ObservableList<UIPhraseHistory> phraseHistoryList;
    ObservableList<UIWord> phraseHistoryWordList;

    @Nullable final Block phraseBlock;
    final PhraserDB phraserDB;
    final Consumer<PhraseBlock> phraseBlockCallback;
    @Nullable Stage stage;

    final FoldersBlock foldersBlock;
    final PhraseTemplatesBlock phraseTemplatesBlock;
    final SymbolSetsBlock symbolSetsBlock;

    @Nullable PhraseTemplatesBlock.PhraseTemplate phraseTemplate;
    @Nullable FoldersBlock.Folder folder;

    public PhraseBlockForm(@Nullable Block phraseBlock,
                            FoldersBlock foldersBlock,
                            PhraseTemplatesBlock phraseTemplatesBlock,
                            SymbolSetsBlock symbolSetsBlock,
                            PhraserDB phraserDB,
                            Consumer<PhraseBlock> phraseBlockCallback) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("PhraseBlockForm.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        this.foldersBlock = foldersBlock;
        this.phraseTemplatesBlock = phraseTemplatesBlock;
        this.symbolSetsBlock = symbolSetsBlock;

        this.phraseBlock = phraseBlock;
        if (phraseBlock == null) {
            checkNotNull(blockIdTextField).setText(NEW_BLOCK);
            checkNotNull(versionTextField).setText(NEW_BLOCK);
        } else {
            checkNotNull(blockIdTextField).setText(Integer.toString(checkNotNull(phraseBlock.phraseTemplatesBlock()).blockId()));
            checkNotNull(versionTextField).setText(Long.toString(checkNotNull(phraseBlock.phraseTemplatesBlock()).version()));
        }

        this.phraserDB = phraserDB;
        this.phraseBlockCallback = phraseBlockCallback;

        this.phraseHistoryList = FXCollections.observableArrayList();
        checkNotNull(phraseHistoryTableView).itemsProperty().set(phraseHistoryList);

        this.phraseHistoryWordList = FXCollections.observableArrayList();
        checkNotNull(phraseHistoryWordsTableView).itemsProperty().set(phraseHistoryWordList);

        phraseHistoryTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    phraseHistoryWordList.clear();
                    if (newSelection != null) {
                        phraseHistoryWordList.addAll(newSelection.words);
                    }
        });

        checkNotNull(copyColumn).setCellFactory(new Callback<>() {
            @Override
            public TableCell<UIWord, String> call(TableColumn<UIWord, String> tableColumn) {
                return new TableCell<>() {
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        int index = getIndex();
                        List<UIWord> items = getTableView().getItems();
                        if (index >= 0 && index < items.size()) {
                            UIWord message = items.get(index);
                            if (message.isTypeable) {
                                setGraphic(getButton(message));
                                return;
                            }
                        }
                        setGraphic(null);
                    }

                    private Button getButton(UIWord message) {
                        Button button = new Button("Copy");
                        button.setOnAction(event -> {
                            JavaFxUtils.copyToClipboard(message.value);
                        });
                        return button;
                    }
                };
            }
        });

        // TODO: Init Form from phraseBlock

        updateBlockSize();
    }

    protected void updateBlockSize() {
        Block block = Block.create(formPhraseBlock(false));

        int bufferLength = BlockEncoder.toFlatBufBlock(block).length;
        checkNotNull(blockSizeTextField).textProperty().set(Integer.toString(bufferLength));
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    // ----------------------------------------------------------------------

    protected PhraseTemplatesBlock.WordTemplate getWordTemplate(int wordTemplateId) {
        return phraseTemplatesBlock.wordTemplates().stream()
                .filter(word -> wordTemplateId == word.wordTemplateId())
                .findFirst()
                .get();
    }

    protected List<char[]> getSymbolSets(PhraseTemplatesBlock.WordTemplate wordTemplate) {
        return wordTemplate.symbolSetIds().stream().map(
                symbolSetId -> {
                    Optional<char[]> symbolSetOpt = symbolSetsBlock.symbolSets().stream()
                            .filter(symbolSet -> symbolSetId == symbolSet.symbolSetId() )
                            .map(SymbolSetsBlock.SymbolSet::symbolSet)
                            .findFirst();
                    return symbolSetOpt.get();
                }
        ).toList();
    }

    public void newPhraseHistory() {
        try {
            if (phraseTemplate == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Phrase Template not chosen, please choose.", ButtonType.OK);
                LOGGER.error("Phrase Template not chosen");
                alert.showAndWait();
                return;
            }

            List<DialogWord> dialogWords = phraseTemplate.wordTemplateIds().stream()
                    .map(
                    wordTemplateId -> {
                        PhraseTemplatesBlock.WordTemplate wordTemplate = getWordTemplate(wordTemplateId);
                        return new DialogWord(wordTemplate.getId(),
                                wordTemplate.getName(),
                                "",
                                wordTemplate.minLength(),
                                wordTemplate.maxLength(),
                                PhraseTemplatesBlock.isUserEditable(wordTemplate.permissions()),
                                PhraseTemplatesBlock.isGenerateable(wordTemplate.permissions()),
                                PhraseTemplatesBlock.isViewable(wordTemplate.permissions()),
                                getSymbolSets(wordTemplate),
                                false
                            );
                    }
            ).toList();

            showPhraseWordsDialog(dialogWords);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding Phrase History: " + e, ButtonType.OK);
            LOGGER.error("Error adding Phrase History: ", e);
            alert.showAndWait();
        }
    }

    public void updatePhraseHistory() {
        try {
            UIPhraseHistory selectedItem = checkNotNull(phraseHistoryTableView).getSelectionModel().selectedItemProperty().get();
            if (selectedItem == null) {
                return;
            }

            if (phraseTemplate == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Phrase Template not chosen, please choose.", ButtonType.OK);
                LOGGER.error("Phrase Template not chosen");
                alert.showAndWait();
                return;
            }

            Map<Integer, Queue<DialogWord>> existingWords = new HashMap<>();
            for (UIWord word : selectedItem.words) {
                Queue<DialogWord> queue = existingWords.computeIfAbsent(word.wordId, k -> new ArrayDeque<>());

                PhraseTemplatesBlock.WordTemplate wordTemplate = getWordTemplate(word.wordId);
                DialogWord dialogWord = new DialogWord(word.wordId,
                        word.wordName,
                        word.value,
                        wordTemplate.minLength(),
                        wordTemplate.maxLength(),
                        PhraseTemplatesBlock.isUserEditable(wordTemplate.permissions()),
                        PhraseTemplatesBlock.isGenerateable(wordTemplate.permissions()),
                        PhraseTemplatesBlock.isViewable(wordTemplate.permissions()),
                        getSymbolSets(wordTemplate),
                        !checkNotNull(phraseTemplate).wordTemplateIds().contains(word.wordId));

                queue.add(dialogWord);
            }

            List<DialogWord> dialogWords = new ArrayList<>();
            List<Integer> wordTemplateIds = phraseTemplate.wordTemplateIds();
            for (int wordTemplateId : wordTemplateIds) {
                PhraseTemplatesBlock.WordTemplate wordTemplate = getWordTemplate(wordTemplateId);
                DialogWord dialogWord;
                Queue<DialogWord> dialogWordQueue = existingWords.get(wordTemplateId);
                if (dialogWordQueue != null && !dialogWordQueue.isEmpty()) {
                    dialogWord = dialogWordQueue.poll();
                } else {
                    dialogWord = new DialogWord(wordTemplate.getId(),
                            wordTemplate.getName(),
                            "",
                            wordTemplate.minLength(),
                            wordTemplate.maxLength(),
                            PhraseTemplatesBlock.isUserEditable(wordTemplate.permissions()),
                            PhraseTemplatesBlock.isGenerateable(wordTemplate.permissions()),
                            PhraseTemplatesBlock.isViewable(wordTemplate.permissions()),
                            getSymbolSets(wordTemplate),
                            false
                    );
                }
                dialogWords.add(dialogWord);
            }

            for (Queue<DialogWord> dialogWordQueue : existingWords.values()) {
                while (!dialogWordQueue.isEmpty()) {
                    dialogWords.add(dialogWordQueue.poll());
                }
            }

            showPhraseWordsDialog(dialogWords);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding Phrase History: " + e, ButtonType.OK);
            LOGGER.error("Error adding Phrase History: ", e);
            alert.showAndWait();
        }
    }

    protected void showPhraseWordsDialog(List<DialogWord> dialogWords) {
        PhraseWordsDialog phraseWordsDialog = new PhraseWordsDialog(dialogWords);
        Stage workspaceStage = ModalWindow.showModal(checkNotNull(stage),
                stage -> { phraseWordsDialog.setStage(stage); return phraseWordsDialog; },
                "Phrase",
                null,
                true);

        workspaceStage.setOnHidden(
                ev -> {
                    try {
                        List<PhraseWordsDialog.RetWord> phraseUpdate = phraseWordsDialog.getPhraseUpdate();
                        if (phraseUpdate != null) {
                            List<UIWord> words = new ArrayList<>();
                            for (PhraseWordsDialog.RetWord retWord : phraseUpdate) {
                                int wordId = retWord.wordId;
                                if (checkNotNull(phraseTemplate).wordTemplateIds().contains(wordId)) {
                                    String value = retWord.value;

                                    Optional<PhraseTemplatesBlock.WordTemplate> wordTemplateOpt =
                                            phraseTemplatesBlock.wordTemplates().stream()
                                                    .filter(w -> w.wordTemplateId() == wordId)
                                                    .findFirst();

                                    String wordName;
                                    boolean isTypeable, isViewable;

                                    if (wordTemplateOpt.isEmpty()) {
                                        wordName = "Unrecognized";
                                        isTypeable = false;
                                        isViewable = false;
                                    } else {
                                        PhraseTemplatesBlock.WordTemplate wordTemplate = wordTemplateOpt.get();
                                        wordName = wordTemplate.wordTemplateName();
                                        isTypeable = PhraseTemplatesBlock.isTypeable(wordTemplate.permissions());
                                        isViewable = PhraseTemplatesBlock.isViewable(wordTemplate.permissions());
                                    }

                                    UIWord uiWord = new UIWord(wordId, wordName, value, isTypeable, isViewable);
                                    words.add(uiWord);
                                }
                            }

                            UIPhraseHistory phraseHistory = new UIPhraseHistory(phraseHistoryList, words);
                            phraseHistoryList.add(0, phraseHistory);
                        }
                    } catch (Exception e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding Phrase History: " + e, ButtonType.OK);
                        LOGGER.error("Error adding Phrase History: ", e);
                        alert.showAndWait();
                    }
                }
        );
    }

    public void deletePhraseHistory() {
        try {
            UIPhraseHistory item = checkNotNull(phraseHistoryTableView).getSelectionModel().getSelectedItem();
            if (item != null) {
                phraseHistoryList.remove(item);
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error removing Phrase History: " + e, ButtonType.OK);
            LOGGER.error("Error removing Phrase History: ", e);
            alert.showAndWait();
        }
    }

    public void openPhraseTemplate() {
        try {
            PickPhraseTemplateDialog pickPhraseTemplateDialog = new PickPhraseTemplateDialog(phraseTemplatesBlock.phraseTemplates());
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(stage),
                    stage -> { pickPhraseTemplateDialog.setStage(stage); return pickPhraseTemplateDialog; },
                    "Pick Phrase Template");

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            PhraseTemplatesBlock.PhraseTemplate phraseTemplate = pickPhraseTemplateDialog.phraseTemplate;
                            if (phraseTemplate != null) {
                                this.phraseTemplate = phraseTemplate;
                                checkNotNull(phraseTemplateTextField).textProperty().set(phraseTemplate.phraseTemplateName());
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error picking Phrase Template: " + e, ButtonType.OK);
                            LOGGER.error("Error picking Phrase Template: ", e);
                            alert.showAndWait();
                        }
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error picking Phrase Template: " + e, ButtonType.OK);
            LOGGER.error("Error picking Phrase Template: ", e);
            alert.showAndWait();
        }
    }

    public void openFolder() {
        try {
            PickFolderDialog pickFolderDialog = new PickFolderDialog(foldersBlock.folders());
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(stage),
                    stage -> { pickFolderDialog.setStage(stage); return pickFolderDialog; },
                    "Pick Folder");

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            PickFolderDialog.UIFolder folder = pickFolderDialog.getFolder();
                            if (folder != null) {
                                this.folder = folder.folder;
                                checkNotNull(folderTextField).textProperty().set("[" + folder.getId() + "] " + folder.getPath());
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error picking folder: " + e, ButtonType.OK);
                            LOGGER.error("Error picking folder: ", e);
                            alert.showAndWait();
                        }
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error picking folder: " + e, ButtonType.OK);
            LOGGER.error("Error picking folder: ", e);
            alert.showAndWait();
        }
    }

    protected PhraseBlock formPhraseBlock(boolean useRealEntropy) {
        List<UIPhraseHistory> phraseHistoryList = new ArrayList<>(this.phraseHistoryList);

        return ImmutablePhraseBlock.builder()
                .blockId(phraseBlock == null ? -1 : phraseBlock.getBlockId())
                .version(123)
                .entropy(useRealEntropy ? PhraserUtils.generateEntropy() : 123L)

                /*
                TODO:
                    .phraseTemplateId(int)
                    .folderId(int)
                    .isTombstone(boolean)
                    .phraseName(String)
                    .history(phraseHistoryList)
                */

                .phraseTemplateId(0)
                .folderId(0)
                .isTombstone(true)
                .phraseName("String")

                .build();
    }

    public void saveToDb() {

    }
}
