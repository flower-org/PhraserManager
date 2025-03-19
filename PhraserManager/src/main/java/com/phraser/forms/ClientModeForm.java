package com.phraser.forms;

import com.flower.fxutils.JavaFxUtils;
import com.flower.fxutils.ModalWindow;
import com.phraser.db.Block;
import com.phraser.db.FoldersBlock;
import com.phraser.db.Icon;
import com.phraser.db.PhraseBlock;
import com.phraser.db.PhraseTemplatesBlock;
import com.phraser.db.SymbolSetsBlock;
import com.phraser.runtimedb.DbRuntime;
import com.phraser.utils.PhraserUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.flower.fxutils.JavaFxUtils.YesNo.YES;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.phraser.forms.DefaultDBCreator.DEFAULT_SYMBOL_SETS;
import static com.phraser.utils.PhraserUtils.removeGenerateable;
import static com.phraser.utils.PhraserUtils.removeUserEditable;

public class ClientModeForm extends AnchorPane {
    final static Logger LOGGER = LoggerFactory.getLogger(ClientModeForm.class);

    public enum ExplorerNodeType {
        UP,
        FOLDER,
        PHRASE,
        HISTORY,
        HISTORY_ENTRY,
        WORD
    }

    public static class ExplorerNode {
        final ExplorerNodeType type;
        final String name;
        final int id;
        @Nullable final UIWord word;

        ExplorerNode(ExplorerNodeType type, String name, int id) {
            this.type = type;
            this.id = id;
            this.name = name;
            this.word = null;
        }

        ExplorerNode(UIWord word) {
            this.type = ExplorerNodeType.WORD;
            this.id = word.wordTemplateId;
            this.name = word.wordName;
            this.word = word;
        }

        public ExplorerNodeType getType() { return type; }
        public String getName() { return name; }
        public String getId() { return type == ExplorerNodeType.UP || type == ExplorerNodeType.HISTORY ? "" : Integer.toString(id); }
        public String getValue() {
            return word == null ? "" : word.getValue();
        }
    }

    public static class UIWord {
        public final int wordTemplateId;
        public final String wordName;
        public final String value;
        public final byte permissions;
        public final Icon icon;
        public final char[] symbolSet;

        public final boolean isTypeable;
        public final boolean isViewable;
        public final boolean isGenerateable;
        public final boolean isUserEditable;

        public final boolean isPartOfTemplate;

        public UIWord(int wordTemplateId, String wordName, String value, byte permissions, Icon icon, char[] symbolSet, boolean isPartOfTemplate) {
            this.wordTemplateId = wordTemplateId;
            this.wordName = wordName;
            this.value = value;

            // Force not generateable, not editable for non-template words
            if (!isPartOfTemplate) {
                permissions = removeGenerateable(permissions);
                permissions = removeUserEditable(permissions);
            }
            this.permissions = permissions;
            this.icon = icon;
            this.symbolSet = symbolSet;
            this.isPartOfTemplate = isPartOfTemplate;

            this.isTypeable = PhraserUtils.isTypeable(permissions);
            this.isViewable = PhraserUtils.isViewable(permissions);
            this.isGenerateable = PhraserUtils.isGenerateable(permissions);
            this.isUserEditable = PhraserUtils.isUserEditable(permissions);
        }

        public String getValue() {
            if (isViewable) {
                return value;
            } else {
                return StringUtils.isBlank(value) ? "" : "*****";
            }
        }
    }

    @FXML @Nullable TitledPane foldersTitledPane;
    @FXML @Nullable TableView<ExplorerNode> foldersTableView;
    final ObservableList<ExplorerNode> folderContent;

    @FXML @Nullable TitledPane phraseTitledPane;
    @FXML @Nullable TableView<ExplorerNode> phraseTableView;
    final ObservableList<ExplorerNode> phraseContent;

    @FXML @Nullable TitledPane phraseHistoryTitledPane;
    @FXML @Nullable TableView<ExplorerNode> phraseHistoryTableView;
    final ObservableList<ExplorerNode> phraseHistoryContent;

    @FXML @Nullable Button renamePhrasePhrasePaneButton;
    @FXML @Nullable Button deletePhrasePhrasePaneButton;
    @FXML @Nullable Button changeTemplatePhrasePaneButton;
    @FXML @Nullable Button changeFolderPhrasePaneButton;

    @FXML @Nullable TableColumn<ExplorerNode, String> copyColumn;
    @FXML @Nullable TableColumn<ExplorerNode, String> generateColumn;
    @FXML @Nullable TableColumn<ExplorerNode, String> editColumn;

    @Nullable Stage stage;
    @Nullable Tab tab;
    final MainForm mainForm;
    final DbRuntime dbRuntime;
    final Stack<String> path;

    // UI markers
    protected int currentFolderId;
    protected int currentPhraseId;
    @Nullable protected PhraseBlock currentPhraseBlock;
    protected int currentHistoryIndex;
    protected boolean isHistoryView;

    public ClientModeForm(MainForm mainForm, String dbPassword, File dbFile) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ClientModeForm.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        this.dbRuntime = new DbRuntime(dbFile, dbPassword);
        this.mainForm = mainForm;
        this.path = new Stack<>();

        this.phraseContent = FXCollections.observableArrayList();
        checkNotNull(phraseTableView).itemsProperty().set(phraseContent);

        checkNotNull(copyColumn).setCellFactory(new Callback<>() {
            @Override
            public TableCell<ExplorerNode, String> call(TableColumn<ExplorerNode, String> tableColumn) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        int index = getIndex();
                        List<ExplorerNode> items = getTableView().getItems();
                        if (index >= 0 && index < items.size()) {
                            ExplorerNode node = items.get(index);
                            if (node.word != null && node.word.isTypeable) {
                                setGraphic(getCopyButton(node.word));
                                return;
                            }
                        }
                        setGraphic(null);
                    }

                    private Button getCopyButton(UIWord word) {
                        Button button = new Button("Copy");
                        button.setOnAction(event -> {
                            JavaFxUtils.copyToClipboard(word.value);
                        });
                        return button;
                    }
                };
            }
        });

        checkNotNull(generateColumn).setCellFactory(new Callback<>() {
            @Override
            public TableCell<ExplorerNode, String> call(TableColumn<ExplorerNode, String> tableColumn) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        int index = getIndex();
                        List<ExplorerNode> items = getTableView().getItems();
                        if (index >= 0 && index < items.size()) {
                            ExplorerNode node = items.get(index);
                            if (node.word != null && node.word.isGenerateable) {
                                setGraphic(getGenerateButton(node.word));
                                return;
                            }
                        }
                        setGraphic(null);
                    }

                    private Button getGenerateButton(UIWord word) {
                        Button button = new Button("Generate");
                        button.setOnAction(event -> {
                            JavaFxUtils.copyToClipboard(word.value);
                        });
                        return button;
                    }
                };
            }
        });

        checkNotNull(editColumn).setCellFactory(new Callback<>() {
            @Override
            public TableCell<ExplorerNode, String> call(TableColumn<ExplorerNode, String> tableColumn) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        int index = getIndex();
                        List<ExplorerNode> items = getTableView().getItems();
                        if (index >= 0 && index < items.size()) {
                            ExplorerNode node = items.get(index);
                            if (node.word != null && node.word.isUserEditable) {
                                setGraphic(getEditButton(node.word));
                                return;
                            }
                        }
                        setGraphic(null);
                    }

                    private Button getEditButton(UIWord word) {
                        Button button = new Button("Edit");
                        button.setOnAction(event -> {
                            JavaFxUtils.copyToClipboard(word.value);
                        });
                        return button;
                    }
                };
            }
        });

        checkNotNull(phraseTableView).setRowFactory(new Callback<>() {
            @Override
            public TableRow<ExplorerNode> call(TableView<ExplorerNode> blockTableView) {
                return new TableRow<>() {
                    @Override
                    protected void updateItem(ExplorerNode node, boolean empty) {
                        super.updateItem(node, empty);
                        boolean notAPart = false;
                        if (node != null) {
                            if (!isHistoryView) {
                                if (node.word != null && !node.word.isPartOfTemplate) {
                                    notAPart = true;
                                }
                            }
                        }

                        if (notAPart) {
                            styleProperty().setValue("-fx-background-color: salmon");
                        } else {
                            styleProperty().setValue("");
                        }
                    }
                };
            }
        });

        phraseHistoryContent = FXCollections.observableArrayList();
        checkNotNull(phraseHistoryTableView).itemsProperty().set(phraseHistoryContent);

        this.folderContent = FXCollections.observableArrayList();
        checkNotNull(foldersTableView).itemsProperty().set(folderContent);
        currentFolderId = 0;
        loadFolders();
    }

    protected String currentPath() {
        if (path.isEmpty()) { return "/"; }
        StringBuilder pathBuilder = new StringBuilder();
        for (String pathElem : path) {
            pathBuilder.append("/").append(pathElem);
        }
        return pathBuilder.toString();
    }

    public void loadFolders() {
        checkNotNull(foldersTitledPane).textProperty().set(currentPath());

        DbRuntime.FolderContent folderContentObj = dbRuntime.getFolderContent(currentFolderId);
        List<ExplorerNode> folderContentList = new ArrayList<>();

        if (currentFolderId != 0) {
            folderContentList.add(new ExplorerNode(ExplorerNodeType.UP, "..", currentFolderId));
        }
        for (FoldersBlock.Folder subFolder : folderContentObj.subFolders) {
            folderContentList.add(new ExplorerNode(ExplorerNodeType.FOLDER, subFolder.folderName(), subFolder.folderId()));
        }
        for (DbRuntime.PhraseFolderAndName phrase : folderContentObj.phrases) {
            folderContentList.add(new ExplorerNode(ExplorerNodeType.PHRASE, phrase.name, phrase.phraseBlockId));
        }

        folderContent.clear();
        folderContent.addAll(folderContentList);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setTab(Tab tab) {
        this.tab = tab;
        checkNotNull(tab).setText(dbRuntime.getDbName());
    }

    public void foldersTableViewClicked(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            ExplorerNode selectedItem = checkNotNull(foldersTableView).getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                if (selectedItem.type == ExplorerNodeType.UP) {
                    currentFolderId = checkNotNull(dbRuntime.getFolder(currentFolderId)).parentFolderId();
                    path.pop();
                    loadFolders();
                } else if (selectedItem.type == ExplorerNodeType.FOLDER) {
                    path.push(selectedItem.name);
                    currentFolderId = selectedItem.id;
                    loadFolders();
                } else if (selectedItem.type == ExplorerNodeType.PHRASE) {
                    path.push(selectedItem.name);
                    currentPhraseId = selectedItem.id;
                    try {
                        currentPhraseBlock = dbRuntime.getPhrase(currentPhraseId);
                        if (currentPhraseBlock == null) { throw new RuntimeException("PhraseBlock not found"); }
                        loadPhrase();
                        checkNotNull(foldersTitledPane).visibleProperty().set(false);
                        checkNotNull(phraseTitledPane).visibleProperty().set(true);
                    } catch (Exception e) {
                        path.pop();
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Error loading phrase " + currentPhraseId + ": " + e, ButtonType.OK);
                        LOGGER.error("Error loading phrase " + currentPhraseId, e);
                        alert.showAndWait();
                    }
                }
            }
        }
    }

    List<UIWord> getPhraseWords(PhraseBlock phraseBlock, PhraseBlock.PhraseHistory history) {
        PhraseTemplatesBlock.PhraseTemplate phraseTemplate =
                dbRuntime.getPhraseTemplate(phraseBlock.phraseTemplateId());

        Map<Integer, Queue<PhraseBlock.Word>> map = new HashMap<>();
        for (PhraseBlock.Word word : history.phrase()) {
            map.computeIfAbsent(word.wordTemplateId(), k -> new ArrayDeque<>()).add(word);
        }

        List<UIWord> words = new ArrayList<>();
        if (phraseTemplate != null) {
            for (int wordTemplateIds : phraseTemplate.wordTemplateIds()) {
                PhraseTemplatesBlock.WordTemplate wordTemplate = checkNotNull(dbRuntime.getWordTemplate(wordTemplateIds));

                Queue<PhraseBlock.Word> q = map.get(wordTemplateIds);
                PhraseBlock.Word oldWord = q == null ? null : q.poll();

                int wordTemplateId = wordTemplate.wordTemplateId();
                String wordName = wordTemplate.wordTemplateName();
                String value = oldWord == null ? "" : oldWord.word();
                byte permissions = wordTemplate.permissions();
                Icon icon = wordTemplate.icon();
                char[] symbolSet = getSymbolSet(wordTemplate);
                boolean isPartOfTemplate = true;

                UIWord word = new UIWord(wordTemplateId, wordName, value, permissions, icon, symbolSet, isPartOfTemplate);
                words.add(word);
            }
        }

        for (PhraseBlock.Word historyWord : history.phrase()) {
            Queue<PhraseBlock.Word> q = map.get(historyWord.wordTemplateId());
            if (q != null && !q.isEmpty()) {
                int wordTemplateId = historyWord.wordTemplateId();
                String wordName = historyWord.name();
                String value = historyWord.word();
                byte permissions = historyWord.permissions();
                Icon icon = historyWord.icon();
                char[] symbolSet = getSymbolSet(historyWord.wordTemplateId());
                boolean isPartOfTemplate = false;

                UIWord word = new UIWord(wordTemplateId, wordName, value, permissions, icon, symbolSet, isPartOfTemplate);
                words.add(word);
            }
        }

        return words;
    }

    List<UIWord> getHistoryWords(PhraseBlock.PhraseHistory history) {
        List<UIWord> words = new ArrayList<>();
        for (PhraseBlock.Word historyWord : history.phrase()) {
            int wordTemplateId = historyWord.wordTemplateId();
            String wordName = historyWord.name();
            String value = historyWord.word();
            byte permissions = historyWord.permissions();
            Icon icon = historyWord.icon();
            char[] symbolSet = getSymbolSet(historyWord.wordTemplateId());
            boolean isPartOfTemplate = false;

            UIWord word = new UIWord(wordTemplateId, wordName, value, permissions, icon, symbolSet, isPartOfTemplate);
            words.add(word);
        }

        return words;
    }

    protected char[] mergeSymbolSets(List<char[]> symbolSets) {
        Set<Character> chars = new HashSet<>();
        for (char[] symbolSet : symbolSets) {
            if (symbolSet != null) {
                for (char c : symbolSet) {
                    chars.add(c);
                }
            }
        }

        char[] retVal = new char[chars.size()];
        int i = 0;
        for (Character c : chars) {
            retVal[i++] = c;
        }

        return retVal;
    }

    protected char[] getSymbolSet(int wordTemplateId) {
        PhraseTemplatesBlock.WordTemplate wordTemplate = dbRuntime.getWordTemplate(wordTemplateId);
        if (wordTemplate == null) {
            return mergeSymbolSets(DEFAULT_SYMBOL_SETS);
        } else {
            return getSymbolSet(wordTemplate);
        }
    }

    protected char[] getSymbolSet(PhraseTemplatesBlock.WordTemplate wordTemplate) {
        List<char[]> symbolSets = new ArrayList<>();
        for (int symbolSetId : wordTemplate.symbolSetIds()) {
            SymbolSetsBlock.SymbolSet symbolSet = dbRuntime.getSymbolSet(symbolSetId);
            if (symbolSet != null) {
                symbolSets.add(symbolSet.symbolSet());
            }
        }

        return mergeSymbolSets(symbolSets);
    }

    public void switchPhraseContext(boolean on) {
        checkNotNull(renamePhrasePhrasePaneButton).visibleProperty().set(on);
        checkNotNull(deletePhrasePhrasePaneButton).visibleProperty().set(on);
        checkNotNull(changeTemplatePhrasePaneButton).visibleProperty().set(on);
        checkNotNull(changeFolderPhrasePaneButton).visibleProperty().set(on);
    }

    public void loadPhraseHistoryEntry() {
        switchPhraseContext(false);
        checkNotNull(phraseTitledPane).textProperty().set(currentPath());

        List<UIWord> phraseWords = getHistoryWords(checkNotNull(currentPhraseBlock).history().get(currentHistoryIndex));
        List<ExplorerNode> phraseContent = new ArrayList<>();
        if (currentFolderId != 0) {
            phraseContent.add(new ExplorerNode(ExplorerNodeType.UP, "..", currentPhraseId));
        }
        for (UIWord phraseWord : phraseWords) {
            phraseContent.add(new ExplorerNode(phraseWord));
        }

        this.phraseContent.clear();
        this.phraseContent.addAll(phraseContent);
    }

    public void loadPhrase() {
        switchPhraseContext(true);
        checkNotNull(phraseTitledPane).textProperty().set(currentPath());

        List<UIWord> phraseWords = getPhraseWords(checkNotNull(currentPhraseBlock), currentPhraseBlock.history().get(0));
        List<ExplorerNode> phraseContent = new ArrayList<>();
        if (currentFolderId >= 0) {
            phraseContent.add(new ExplorerNode(ExplorerNodeType.UP, "..", currentPhraseId));
        }
        for (UIWord phraseWord : phraseWords) {
            phraseContent.add(new ExplorerNode(phraseWord));
        }
        phraseContent.add(new ExplorerNode(ExplorerNodeType.HISTORY, "-> History", currentPhraseId));

        this.phraseContent.clear();
        this.phraseContent.addAll(phraseContent);
    }

    public void loadPhraseHistory() {
        checkNotNull(phraseHistoryTitledPane).textProperty().set(currentPath());

        List<ExplorerNode> phraseHistoryContent = new ArrayList<>();
        if (currentFolderId != 0) {
            phraseHistoryContent.add(new ExplorerNode(ExplorerNodeType.UP, "..", currentPhraseId));
        }
        for (int i = 0; i < checkNotNull(currentPhraseBlock).history().size(); i++) {
            String name = i == 0 ? "History 0 (current)" : "History " + i;
            phraseHistoryContent.add(new ExplorerNode(ExplorerNodeType.HISTORY_ENTRY, name, i));
        }

        this.phraseHistoryContent.clear();
        this.phraseHistoryContent.addAll(phraseHistoryContent);
    }

    public void phraseHistoryTableViewClicked(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            ExplorerNode selectedItem = checkNotNull(phraseHistoryTableView).getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                if (selectedItem.type == ExplorerNodeType.UP) {
                    path.pop();
                    loadPhrase();
                    checkNotNull(phraseHistoryTitledPane).visibleProperty().set(false);
                    checkNotNull(phraseTitledPane).visibleProperty().set(true);
                } else if (selectedItem.type == ExplorerNodeType.HISTORY_ENTRY) {
                    path.push(selectedItem.name);
                    currentHistoryIndex = selectedItem.id;
                    isHistoryView = true;
                    loadPhraseHistoryEntry();
                    checkNotNull(phraseHistoryTitledPane).visibleProperty().set(false);
                    checkNotNull(phraseTitledPane).visibleProperty().set(true);
                }
            }
        }
    }

    protected void switchToFolderViewFromPhraseView() {
        path.pop();
        currentFolderId = checkNotNull(currentPhraseBlock).folderId();
        checkNotNull(phraseTitledPane).visibleProperty().set(false);
        checkNotNull(foldersTitledPane).visibleProperty().set(true);
        loadFolders();
    }

    public void phraseTableViewClicked(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            ExplorerNode selectedItem = checkNotNull(phraseTableView).getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                if (selectedItem.type == ExplorerNodeType.UP && !isHistoryView) {
                    switchToFolderViewFromPhraseView();
                } else if (selectedItem.type == ExplorerNodeType.UP) {
                    path.pop();
                    loadPhraseHistory();
                    isHistoryView = false;
                    checkNotNull(phraseTitledPane).visibleProperty().set(false);
                    checkNotNull(phraseHistoryTitledPane).visibleProperty().set(true);
                } else if (selectedItem.type == ExplorerNodeType.HISTORY) {
                    path.push("[History]");
                    loadPhraseHistory();
                    checkNotNull(phraseTitledPane).visibleProperty().set(false);
                    checkNotNull(phraseHistoryTitledPane).visibleProperty().set(true);
                }
            }
        }
    }

    public void updatePhraseTemplatesBlock() {
        try {
            Block phraseTemplatesBlock = dbRuntime.readPhraseTemplatesBlock();
            AtomicReference<Stage> workspaceStage = new AtomicReference<>();
            Consumer<PhraseTemplatesBlock> phraseTemplatesBlockCallback = newPhraseTemplatesBlock -> {
                try {
                    dbRuntime.updateBlock(Block.of(newPhraseTemplatesBlock));

                    Stage stage = workspaceStage.get();
                    while (stage == null) { stage = workspaceStage.get(); }
                    stage.close();
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error updating phrase templates block: " + e, ButtonType.OK);
                    LOGGER.error("Error updating phrase templates block: ", e);
                    alert.showAndWait();
                }
            };

            PhraseTemplatesBlockForm phraseTemplatesBlockForm = new PhraseTemplatesBlockForm(phraseTemplatesBlock,
                    dbRuntime::getSymbolSets, phraseTemplatesBlockCallback);
            workspaceStage.set(ModalWindow.showModal(checkNotNull(stage),
                    stage -> { phraseTemplatesBlockForm.setStage(stage); return phraseTemplatesBlockForm; },
                    "Update PhraseTemplatesBlock",
                    null,
                    true));
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error updating phrase templates block: " + e, ButtonType.OK);
            LOGGER.error("Error updating phrase templates block: ", e);
            alert.showAndWait();
        }
    }

    public void updateSymbolSetsBlock() {
        try {
            Block symbolSetsBlock = dbRuntime.readSymbolSetsBlock();
            AtomicReference<Stage> workspaceStage = new AtomicReference<>();
            Consumer<SymbolSetsBlock> symbolSetsBlockCallback = newSymbolSetsBlock -> {
                try {
                    dbRuntime.updateBlock(Block.of(newSymbolSetsBlock));

                    Stage stage = workspaceStage.get();
                    while (stage == null) { stage = workspaceStage.get(); }
                    stage.close();
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error updating symbol sets block: " + e, ButtonType.OK);
                    LOGGER.error("Error updating symbol sets block: ", e);
                    alert.showAndWait();
                }
            };

            SymbolSetsBlockForm symbolSetsBlockForm = new SymbolSetsBlockForm(symbolSetsBlock, symbolSetsBlockCallback);
            workspaceStage.set(ModalWindow.showModal(checkNotNull(stage),
                    stage -> { symbolSetsBlockForm.setStage(stage); return symbolSetsBlockForm; },
                    "Update SymbolSetsBlock"));
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error updating symbol sets block: " + e, ButtonType.OK);
            LOGGER.error("Error updating symbol sets block: ", e);
            alert.showAndWait();
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    public void addFolder() {
        // 1. Obtain new folder name
        GenericNameDialog genericNameDialog = new GenericNameDialog("Set Folder Name", null);
        Stage workspaceStage = ModalWindow.showModal(checkNotNull(stage),
                stage -> { genericNameDialog.setStage(stage); return genericNameDialog; },
                "New Folder");

        workspaceStage.setOnHidden(
                ev -> {
                    try {
                        String name = genericNameDialog.getName();
                        if (!StringUtils.isBlank(name)) {
                            // 2. Add subfolder of a current folder
                            dbRuntime.addFolder(name, currentFolderId);

                            // 3. Reload UI
                            loadFolders();
                        }
                    } catch (Exception e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding folder: " + e, ButtonType.OK);
                        LOGGER.error("Error adding folder: ", e);
                        alert.showAndWait();
                    }
                }
        );
    }

    public void renameFolder() {
        try {
            ExplorerNode selectedItem = checkNotNull(foldersTableView).getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                if (selectedItem.type == ExplorerNodeType.FOLDER) {
                    int folderId = selectedItem.id;
                    String folderName = selectedItem.name;

                    // 1. Obtain new folder name
                    GenericNameDialog genericNameDialog = new GenericNameDialog("Set Folder Name", folderName);
                    Stage workspaceStage = ModalWindow.showModal(checkNotNull(stage),
                            stage -> { genericNameDialog.setStage(stage); return genericNameDialog; },
                            "Rename Folder");

                    workspaceStage.setOnHidden(
                            ev -> {
                                try {
                                    String name = genericNameDialog.getName();
                                    if (!StringUtils.isBlank(name)) {
                                        // 2. Rename folder
                                        dbRuntime.renameFolder(folderId, name);

                                        // 3. Reload UI
                                        loadFolders();
                                    }
                                } catch (Exception e) {
                                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error renaming folder: " + e, ButtonType.OK);
                                    LOGGER.error("Error renaming folder: ", e);
                                    alert.showAndWait();
                                }
                            }
                    );
                }
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error renaming folder: " + e, ButtonType.OK);
            LOGGER.error("Error renaming folder: ", e);
            alert.showAndWait();
        }
    }

    public void deleteFolder() {
        try {
            ExplorerNode selectedItem = checkNotNull(foldersTableView).getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                if (selectedItem.type == ExplorerNodeType.FOLDER) {
                    int folderId = selectedItem.id;
                    String folderName = selectedItem.name;

                    // 1. Check folder is not empty
                    if (!dbRuntime.isFolderEmpty(folderId)) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Can't delete folder [" + folderId + " / " + folderName + "]: folder not empty ", ButtonType.OK);
                        LOGGER.error("Can't delete folder [" + folderName + "]: folder not empty ");
                        alert.showAndWait();
                        return;
                    }

                    // 2. Get user confirmation
                    if (YES == JavaFxUtils.showYesNoDialog("Delete folder [" + folderId + " / " + folderName + "]?")) {
                        // 3. Delete folder
                        dbRuntime.removeFolder(folderId);

                        // 4. Reload UI
                        loadFolders();
                    }
                }
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "deleteFolder error: " + e, ButtonType.OK);
            LOGGER.error("deleteFolder error: ", e);
            alert.showAndWait();
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    public void tombstonePhraseFoldersForm() {
        try {
            ExplorerNode selectedItem = checkNotNull(foldersTableView).getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                if (selectedItem.type == ExplorerNodeType.PHRASE) {
                    int phraseId = selectedItem.id;
                    String phraseName = selectedItem.name;

                    // 1. Get user confirmation
                    if (YES == JavaFxUtils.showYesNoDialog("Delete phrase [" + phraseId + " / " + phraseName + "]?")) {
                        // 2. Tombstone phrase
                        dbRuntime.tombstonePhrase(phraseId);

                        // 3. Reload UI
                        loadFolders();
                    }
                }
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "tombstonePhraseFoldersForm error: " + e, ButtonType.OK);
            LOGGER.error("tombstonePhraseFoldersForm error: ", e);
            alert.showAndWait();
        }
    }

    public void tombstonePhrase() {
        try {
            if (currentPhraseBlock != null) {
                int phraseId = currentPhraseBlock.blockId();
                String phraseName = currentPhraseBlock.phraseName();

                // 1. Get user confirmation
                if (YES == JavaFxUtils.showYesNoDialog("Delete phrase [" + phraseId + " / " + phraseName + "]?")) {
                    // 2. Tombstone phrase
                    dbRuntime.tombstonePhrase(phraseId);

                    // 3. Switch back to folder UI and reload
                    switchToFolderViewFromPhraseView();
                }
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "tombstonePhrase error: " + e, ButtonType.OK);
            LOGGER.error("tombstonePhrase error: ", e);
            alert.showAndWait();
        }
    }

    public void changePhraseTemplate() {
        try {
            if (currentPhraseBlock != null) {
                int phraseId = currentPhraseBlock.blockId();
                int currentPhraseTemplateId = currentPhraseBlock.phraseTemplateId();

                // 1. Obtain new phrase template
                PickPhraseTemplateDialog pickPhraseTemplateDialog =
                        new PickPhraseTemplateDialog(dbRuntime.getPhraseTemplates(), currentPhraseTemplateId);
                Stage workspaceStage = ModalWindow.showModal(checkNotNull(stage),
                        stage -> { pickPhraseTemplateDialog.setStage(stage); return pickPhraseTemplateDialog; },
                        "Change Phrase Template");

                workspaceStage.setOnHidden(
                        ev -> {
                            try {
                                PhraseTemplatesBlock.PhraseTemplate phraseTemplate = pickPhraseTemplateDialog.phraseTemplate;
                                if (phraseTemplate != null) {
                                    // 2. Update phrase template
                                    dbRuntime.updatePhraseTemplate(phraseId, phraseTemplate.phraseTemplateId());

                                    // 3. Reload phrase block and refresh UI
                                    currentPhraseBlock = dbRuntime.getPhrase(currentPhraseId);
                                    if (currentPhraseBlock == null) { throw new RuntimeException("PhraseBlock not found"); }

                                    loadPhrase();
                                }
                            } catch (Exception e) {
                                Alert alert = new Alert(Alert.AlertType.ERROR, "changePhraseTemplate error: " + e, ButtonType.OK);
                                LOGGER.error("changePhraseTemplate error: ", e);
                                alert.showAndWait();
                            }
                        }
                );
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "changePhraseTemplate error: " + e, ButtonType.OK);
            LOGGER.error("changePhraseTemplate error: ", e);
            alert.showAndWait();
        }
    }

    public void changePhraseFolder() {
        try {
            if (currentPhraseBlock != null) {
                int phraseId = currentPhraseBlock.blockId();
                int currentFolderId = currentPhraseBlock.folderId();

                // 1. Obtain new phrase template
                PickFolderDialog pickFolderDialog =
                        new PickFolderDialog(dbRuntime.getFolders(), currentFolderId);
                Stage workspaceStage = ModalWindow.showModal(checkNotNull(stage),
                        stage -> { pickFolderDialog.setStage(stage); return pickFolderDialog; },
                        "Change Folder");

                workspaceStage.setOnHidden(
                        ev -> {
                            try {
                                if (pickFolderDialog.folder != null) {
                                    FoldersBlock.Folder folder = pickFolderDialog.folder.folder;

                                    // 2. Update folder
                                    dbRuntime.updatePhraseFolder(phraseId, folder.folderId());

                                    // 3. Reload phrase block
                                    currentPhraseBlock = dbRuntime.getPhrase(currentPhraseId);
                                    if (currentPhraseBlock == null) { throw new RuntimeException("PhraseBlock not found"); }

                                    // 4. Update path to reflect new folder path
                                    Stack<String> reversePathStack = new Stack<>();
                                    reversePathStack.push(currentPhraseBlock.phraseName());
                                    int folderId = currentPhraseBlock.folderId();
                                    while (folderId > 0) {
                                        FoldersBlock.Folder nextFolder = dbRuntime.getFolder(folderId);
                                        if (nextFolder == null) {
                                            break;
                                        }
                                        reversePathStack.push(nextFolder.folderName());
                                        folderId = nextFolder.parentFolderId();
                                    }

                                    path.clear();
                                    while (!reversePathStack.isEmpty()) {
                                        path.push(reversePathStack.pop());
                                    }

                                    // 5. Refresh UI
                                    loadPhrase();
                                }
                            } catch (Exception e) {
                                Alert alert = new Alert(Alert.AlertType.ERROR, "changePhraseTemplate error: " + e, ButtonType.OK);
                                LOGGER.error("changePhraseTemplate error: ", e);
                                alert.showAndWait();
                            }
                        }
                );
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "changePhraseTemplate error: " + e, ButtonType.OK);
            LOGGER.error("changePhraseTemplate error: ", e);
            alert.showAndWait();
        }
    }

    public void renamePhrase() {
        try {
            if (currentPhraseBlock != null) {
                int phraseId = currentPhraseBlock.blockId();
                String phraseName = currentPhraseBlock.phraseName();

                // 1. Obtain new phrase name
                GenericNameDialog genericNameDialog = new GenericNameDialog("Set Phrase Name", phraseName);
                Stage workspaceStage = ModalWindow.showModal(checkNotNull(stage),
                        stage -> { genericNameDialog.setStage(stage); return genericNameDialog; },
                        "Rename Phrase");

                workspaceStage.setOnHidden(
                        ev -> {
                            try {
                                String newPhraseName = genericNameDialog.getName();
                                if (!StringUtils.isBlank(newPhraseName)) {
                                    // 2. Rename folder
                                    dbRuntime.renamePhrase(phraseId, newPhraseName);

                                    // 3. update path to reflect new phrase name
                                    path.pop();
                                    path.push(newPhraseName);

                                    // 3. Reload UI
                                    currentPhraseBlock = dbRuntime.getPhrase(currentPhraseId);
                                    if (currentPhraseBlock == null) { throw new RuntimeException("PhraseBlock not found"); }

                                    loadPhrase();
                                }
                            } catch (Exception e) {
                                Alert alert = new Alert(Alert.AlertType.ERROR, "Error renaming folder: " + e, ButtonType.OK);
                                LOGGER.error("Error renaming folder: ", e);
                                alert.showAndWait();
                            }
                        }
                );
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error renaming folder: " + e, ButtonType.OK);
            LOGGER.error("Error renaming folder: ", e);
            alert.showAndWait();
        }
    }

    public void addPhraseFoldersForm() {
        try {
            // 1. Obtain new phrase template
            PickPhraseTemplateDialog pickPhraseTemplateDialog =
                    new PickPhraseTemplateDialog(dbRuntime.getPhraseTemplates(), null);
            Stage phraseTemplateStage = ModalWindow.showModal(checkNotNull(stage),
                    stage -> { pickPhraseTemplateDialog.setStage(stage); return pickPhraseTemplateDialog; },
                    "Choose Phrase Template");

            phraseTemplateStage.setOnHidden(
                ev1 -> {
                    try {
                        PhraseTemplatesBlock.PhraseTemplate phraseTemplate = pickPhraseTemplateDialog.phraseTemplate;
                        if (phraseTemplate != null) {
                            // 1. Obtain new phrase name
                            GenericNameDialog genericNameDialog = new GenericNameDialog("Set Phrase Name", null);
                            Stage workspaceStage = ModalWindow.showModal(checkNotNull(stage),
                                    stage -> { genericNameDialog.setStage(stage); return genericNameDialog; },
                                    "Enter Phrase Name");

                            workspaceStage.setOnHidden(
                                    ev3 -> {
                                        try {
                                            String newPhraseName = genericNameDialog.getName();
                                            if (!StringUtils.isBlank(newPhraseName)) {
                                                // 2. Create new folder
                                                dbRuntime.createPhrase(phraseTemplate.phraseTemplateId(), currentFolderId, newPhraseName);

                                                // 3. Reload UI
                                                loadFolders();
                                            }
                                        } catch (Exception e) {
                                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error renaming folder: " + e, ButtonType.OK);
                                            LOGGER.error("Error renaming folder: ", e);
                                            alert.showAndWait();
                                        }
                                    }
                            );
                        }
                    } catch (Exception e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "addPhraseFoldersForm error: " + e, ButtonType.OK);
                        LOGGER.error("addPhraseFoldersForm error: ", e);
                        alert.showAndWait();
                    }
                }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "addPhraseFoldersForm folder: " + e, ButtonType.OK);
            LOGGER.error("addPhraseFoldersForm folder: ", e);
            alert.showAndWait();
        }
    }

    public void deleteHistoryEntry() {
        try {
            ExplorerNode selectedItem = checkNotNull(phraseHistoryTableView).getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                if (selectedItem.type == ExplorerNodeType.HISTORY_ENTRY) {
                    int historyEntryIndex = selectedItem.id;
                    String historyEntryName = selectedItem.name;

                    if (historyEntryIndex == 0) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Can't delete history entry 0 - it contains current data", ButtonType.OK);
                        alert.showAndWait();
                        return;
                    }

                    // 1. Get user confirmation
                    if (YES == JavaFxUtils.showYesNoDialog("Delete history entry [" + historyEntryIndex + " / " + historyEntryName + "]?")) {
                        // 2. Delete history entry
                        dbRuntime.deleteHistoryEntry(currentPhraseId, historyEntryIndex);

                        // 3. Reload UI
                        currentPhraseBlock = dbRuntime.getPhrase(currentPhraseId);
                        if (currentPhraseBlock == null) { throw new RuntimeException("PhraseBlock not found"); }

                        loadPhraseHistory();
                    }
                }
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "deleteHistoryEntry error: " + e, ButtonType.OK);
            LOGGER.error("deleteHistoryEntry error: ", e);
            alert.showAndWait();
        }
    }
}
