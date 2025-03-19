package com.phraser.forms;

import com.flower.fxutils.JavaFxUtils;
import com.flower.fxutils.ModalWindow;
import com.phraser.db.Block;
import com.phraser.db.FoldersBlock;
import com.phraser.db.Icon;
import com.phraser.db.ImmutablePhraseBlock;
import com.phraser.db.ImmutablePhraseHistory;
import com.phraser.db.ImmutableWord;
import com.phraser.db.PhraseBlock;
import com.phraser.db.PhraseTemplatesBlock;
import com.phraser.db.PhraserDB;
import com.phraser.db.SymbolSetsBlock;
import com.phraser.dbcodec.FlatBufBlockEncoder;
import com.phraser.utils.PhraserUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.phraser.db.Block.*;
import static com.phraser.forms.DefaultDBCreator.DEFAULT_SYMBOL_SETS;
import static com.phraser.forms.PhraserDbForm.NEW_BLOCK;
import static com.phraser.forms.PhraseWordsDialog.DialogWord;

public class PhraseBlockForm extends AnchorPane {
    final static Logger LOGGER = LoggerFactory.getLogger(PhraseBlockForm.class);
    final AtomicInteger COUNTER = new AtomicInteger(0);

    public class UIPhraseHistory {
        /** 0 - newest history, actual value */
        final int phraseTemplateId;
        final List<UIPhraseHistory> ownerList;
        final List<UIWord> words;
        final int ordinal;

        public UIPhraseHistory(int phraseTemplateId, List<UIPhraseHistory> ownerList, List<UIWord> words) {
            this.phraseTemplateId = phraseTemplateId;
            this.ownerList = ownerList;
            this.words = words;
            ordinal = COUNTER.incrementAndGet();
        }

        public int getIndex() {
            return ownerList.indexOf(this);
        }

        public int getOrdinal() {
            return ordinal;
        }
    }

    public static class UIWord {
        public final int wordTemplateId;
        public final String wordName;
        public final String value;
        public final byte permissions;

        public final boolean isTypeable;
        public final boolean isViewable;
        public final Icon icon;

        public UIWord(int wordTemplateId, String wordName, String value, byte permissions,
                      boolean isTypeable, boolean isViewable, Icon icon) {
            this.wordTemplateId = wordTemplateId;
            this.wordName = wordName;
            this.value = value;
            this.permissions = permissions;
            this.isTypeable = isTypeable;
            this.isViewable = isViewable;
            this.icon = icon;
        }

        public int getWordTemplateId() {
            return wordTemplateId;
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
    @FXML @Nullable TextField phraseNameTextField;
    @FXML @Nullable TextField folderTextField;
    @FXML @Nullable TextField phraseTemplateTextField;
    @FXML @Nullable CheckBox isTombstoneCheckBox;
    @FXML @Nullable TableColumn<UIWord, String> copyColumn;
    @FXML @Nullable TextField entropyTextField;

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
            checkNotNull(entropyTextField).textProperty().set(NEW_BLOCK);
        } else {
            checkNotNull(blockIdTextField).setText(Integer.toString(checkNotNull(phraseBlock.phraseBlock()).blockId()));
            checkNotNull(versionTextField).setText(Long.toString(checkNotNull(phraseBlock.phraseBlock()).version()));
            checkNotNull(entropyTextField).textProperty().set(Long.toString(phraseBlock.getEntropy()));
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
                    @Override
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

        if (phraseBlock != null) {
            initForm(phraseBlock);
        }

        updateBlockSize();
    }

    protected void initForm(Block phraseBlockMain) {
        PhraseBlock phraseBlock = checkNotNull(phraseBlockMain.phraseBlock());

        checkNotNull(phraseNameTextField).textProperty().set(phraseBlock.phraseName());
        checkNotNull(isTombstoneCheckBox).selectedProperty().set(phraseBlock.isTombstone());

        Optional<PhraseTemplatesBlock.PhraseTemplate> phraseTemplateOpt = phraseTemplatesBlock.phraseTemplates().stream()
                .filter(p -> phraseBlock.phraseTemplateId() == p.phraseTemplateId()).findAny();
        if (phraseTemplateOpt.isPresent()) {
            this.phraseTemplate = phraseTemplateOpt.get();
            checkNotNull(phraseTemplateTextField).textProperty().set(phraseTemplate.phraseTemplateName());
        }

        Optional<FoldersBlock.Folder> folderOpt = foldersBlock.folders().stream()
                .filter(f -> phraseBlock.folderId() == f.folderId()).findAny();
        if (folderOpt.isPresent()) {
            this.folder = folderOpt.get();
            checkNotNull(folderTextField).textProperty().set("[" + folder.folderId() + "] " +
                    FoldersBlock.getPath(folder, foldersBlock.folders()));
        }

        //Backwards order, oldest is ordinal 1, newest index 0
        for (int i = phraseBlock.history().size()-1; i >= 0; i--) {
            PhraseBlock.PhraseHistory historyEntry = phraseBlock.history().get(i);

            List<UIWord> words = new ArrayList<>();
            for (PhraseBlock.Word retWord : historyEntry.phrase()) {
                int wordId = retWord.wordTemplateId();
                String value = retWord.word();
                String wordName = retWord.name();
                byte permissions = retWord.permissions();
                Icon icon = retWord.icon();
                boolean isTypeable = PhraserUtils.isTypeable(permissions);
                boolean isViewable = PhraserUtils.isViewable(permissions);

                UIWord uiWord = new UIWord(wordId, wordName, value, permissions, isTypeable, isViewable, icon);
                words.add(uiWord);
            }

            UIPhraseHistory phraseHistory = new UIPhraseHistory(historyEntry.phraseTemplateId(), phraseHistoryList, words);
            phraseHistoryList.add(0, phraseHistory);
            checkNotNull(phraseHistoryTableView).getSelectionModel().select(0);
        }
    }

    protected void updateBlockSize() {
        Block block = Block.of(formPhraseBlock(false));

        int bufferLength = FlatBufBlockEncoder.toFlatBufBlock(block).length;
        checkNotNull(blockSizeTextField).textProperty().set(Integer.toString(bufferLength));
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    // ----------------------------------------------------------------------

    protected Optional<PhraseTemplatesBlock.WordTemplate> getWordTemplateOpt(int wordTemplateId) {
        return phraseTemplatesBlock.wordTemplates().stream()
                .filter(word -> wordTemplateId == word.wordTemplateId())
                .findFirst();
    }

    protected PhraseTemplatesBlock.WordTemplate getWordTemplate(int wordTemplateId) throws NoSuchElementException {
        return getWordTemplateOpt(wordTemplateId).get();
    }

    protected List<char[]> getSymbolSets(PhraseTemplatesBlock.WordTemplate wordTemplate) {
        List<char[]> symbolSets = wordTemplate.symbolSetIds().stream().map(
                symbolSetId -> {
                    Optional<char[]> symbolSetOpt = symbolSetsBlock.symbolSets().stream()
                            .filter(symbolSet -> symbolSetId == symbolSet.symbolSetId() )
                            .map(SymbolSetsBlock.SymbolSet::symbolSet)
                            .findFirst();
                    return symbolSetOpt.orElseGet(() -> new char[]{});
                }
        ).filter(ss -> ss.length > 0)
        .toList();

        if (symbolSets.isEmpty()) { symbolSets = DEFAULT_SYMBOL_SETS; }

        return symbolSets;
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
                                PhraserUtils.isUserEditable(wordTemplate.permissions()),
                                PhraserUtils.isGenerateable(wordTemplate.permissions()),
                                PhraserUtils.isViewable(wordTemplate.permissions()),
                                getSymbolSets(wordTemplate),
                                false
                            );
                    }
            ).toList();

            showPhraseWordsDialog(phraseTemplate.phraseTemplateId(), dialogWords);
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
                Queue<DialogWord> queue = existingWords.computeIfAbsent(word.wordTemplateId, k -> new ArrayDeque<>());

                Optional<PhraseTemplatesBlock.WordTemplate> wordTemplateOpt = getWordTemplateOpt(word.wordTemplateId);

                int minLength;
                int maxLength;
                List<char[]> symbolSets;
                if (wordTemplateOpt.isEmpty()) {
                    minLength = 0;
                    maxLength = 64;
                    symbolSets = DEFAULT_SYMBOL_SETS;
                } else {
                    PhraseTemplatesBlock.WordTemplate wordTemplate = wordTemplateOpt.get();
                    minLength = wordTemplate.minLength();
                    maxLength = wordTemplate.maxLength();
                    symbolSets = getSymbolSets(wordTemplate);
                }

                DialogWord dialogWord = new DialogWord(word.wordTemplateId,
                        word.wordName,
                        word.value,
                        minLength,
                        maxLength,
                        PhraserUtils.isUserEditable(word.permissions),
                        PhraserUtils.isGenerateable(word.permissions),
                        PhraserUtils.isViewable(word.permissions),
                        symbolSets,
                        !checkNotNull(phraseTemplate).wordTemplateIds().contains(word.wordTemplateId));

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
                            PhraserUtils.isUserEditable(wordTemplate.permissions()),
                            PhraserUtils.isGenerateable(wordTemplate.permissions()),
                            PhraserUtils.isViewable(wordTemplate.permissions()),
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

            showPhraseWordsDialog(phraseTemplate.phraseTemplateId(), dialogWords);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding Phrase History: " + e, ButtonType.OK);
            LOGGER.error("Error adding Phrase History: ", e);
            alert.showAndWait();
        }
    }

    protected void showPhraseWordsDialog(int phraseTemplateId, List<DialogWord> dialogWords) {
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
                                    byte permissions;
                                    Icon icon;

                                    if (wordTemplateOpt.isEmpty()) {
                                        wordName = "Unrecognized";
                                        isTypeable = false;
                                        isViewable = false;
                                        permissions = (byte)0xFF;
                                        icon = Icon.QUESTION;
                                    } else {
                                        PhraseTemplatesBlock.WordTemplate wordTemplate = wordTemplateOpt.get();
                                        wordName = wordTemplate.wordTemplateName();
                                        isTypeable = PhraserUtils.isTypeable(wordTemplate.permissions());
                                        isViewable = PhraserUtils.isViewable(wordTemplate.permissions());
                                        permissions = wordTemplate.permissions();
                                        icon = wordTemplate.icon();
                                    }

                                    UIWord uiWord = new UIWord(wordId, wordName, value, permissions, isTypeable, isViewable, icon);
                                    words.add(uiWord);
                                }
                            }

                            UIPhraseHistory phraseHistory = new UIPhraseHistory(phraseTemplateId, phraseHistoryList, words);
                            phraseHistoryList.add(0, phraseHistory);
                            checkNotNull(phraseHistoryTableView).getSelectionModel().select(0);
                        }
                        updateBlockSize();
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
            updateBlockSize();
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
                            updateBlockSize();
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
                            updateBlockSize();
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
        List<PhraseBlock.PhraseHistory> blockPhraseHistoryList = new ArrayList<>(this.phraseHistoryList.size());
        for (UIPhraseHistory phraseHistory : this.phraseHistoryList) {
            List<PhraseBlock.Word> phrase = new ArrayList<>();
            for (UIWord word : phraseHistory.words) {
                PhraseBlock.Word blockWord = ImmutableWord.builder()
                        .wordTemplateId(word.wordTemplateId)
                        .name(word.wordName)
                        .word(word.value)
                        .permissions(word.permissions)
                        .icon(word.icon)
                        .build();
                phrase.add(blockWord);
            }

            PhraseBlock.PhraseHistory blockPhraseHistory =
                    ImmutablePhraseHistory.builder()
                            .phraseTemplateId(phraseHistory.phraseTemplateId)
                            .phrase(phrase)
                            .build();

            blockPhraseHistoryList.add(blockPhraseHistory);
        }

        String phraseName = checkNotNull(phraseNameTextField).textProperty().get();
        int phraseTemplateId = phraseTemplate == null ? 0 : phraseTemplate.getId();
        int folderId = folder == null ? 0 : folder.folderId();
        boolean isTombstone = checkNotNull(isTombstoneCheckBox).selectedProperty().get();

        return ImmutablePhraseBlock.builder()
                .blockId(phraseBlock == null ? -1 : phraseBlock.getBlockId())
                .version(DUMMY_VERSION)
                .entropy(useRealEntropy ? PhraserUtils.generateEntropy() : DUMMY_ENTROPY)

                .phraseTemplateId(phraseTemplateId)
                .folderId(folderId)
                .isTombstone(isTombstone)
                .phraseName(phraseName)

                .history(blockPhraseHistoryList)

                .build();
    }

    public void saveToDb() {
        if (phraseTemplate == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "PhraseTemplate not selected", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        if (folder == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Folder not selected", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        String phraseName = checkNotNull(phraseNameTextField).textProperty().get();
        if (StringUtils.isBlank(phraseName)) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Phrase name can't be empty", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        if (this.phraseHistoryList == null || this.phraseHistoryList.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "PhraseHistory can't be empty", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        PhraseBlock newPhraseBlock = formPhraseBlock(true);

        Block block = Block.of(newPhraseBlock);
        int bufferLength = FlatBufBlockEncoder.toFlatBufBlock(block).length;

        if (bufferLength > DATA_BLOCK_SIZE) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Block size can't exceed "+DATA_BLOCK_SIZE+" bytes", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        // This call will close the form and process the formed block
        phraseBlockCallback.accept(newPhraseBlock);
    }

    public void phraseNameChanged() {
        updateBlockSize();
    }

    public void isTombstoneChanged() {
        updateBlockSize();
    }
}
