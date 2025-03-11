package com.phraser.forms;

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
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.phraser.forms.PhraserDbForm.NEW_BLOCK;
import static com.phraser.forms.PhraseWordsDialog.DialogWord;

public class PhraseBlockForm extends AnchorPane {
    final static Logger LOGGER = LoggerFactory.getLogger(PhraseBlockForm.class);

    public static class UIPhraseHistory {
        /** 0 - newest history, actual value */
        int index;
        List<UIPhraseHistory> ownerList;

        public UIPhraseHistory(int index, List<UIPhraseHistory> ownerList) {
            this.index = index;
            this.ownerList = ownerList;
        }

        public int getIndex() {
            return ownerList.indexOf(this);
        }
    }

    public static class UIWord {
        public final int wordId;
        public final String wordName;
        public final String value;

        public final boolean isGenerateable;
        public final boolean isUserEditable;
        public final boolean isTypeable;
        public final boolean isViewable;

        public UIWord(int wordId, String wordName, String value,
                      boolean isGenerateable, boolean isUserEditable, boolean isTypeable, boolean isViewable) {
            this.wordId = wordId;
            this.wordName = wordName;
            this.value = value;
            this.isGenerateable = isGenerateable;
            this.isUserEditable = isUserEditable;
            this.isTypeable = isTypeable;
            this.isViewable = isViewable;
        }

        public String getWordName() {
            return wordName;
        }
        public String getValue() {
            return value;
        }
    }

    @FXML @Nullable TextField blockIdTextField;
    @FXML @Nullable TextField versionTextField;
    @FXML @Nullable TextField blockSizeTextField;
    @FXML @Nullable TableView<UIPhraseHistory> phraseHistoryTableView;
    @FXML @Nullable TableView<UIWord> phraseHistoryWordsTableView;
    @FXML @Nullable TextField folderTextField;
    @FXML @Nullable TextField phraseTemplateTextField;

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

        // TODO: Init Form from phraseBlock

        updateBlockSize();
    }

    void updateBlockSize() {
        Block block = Block.create(formPhraseBlock(false));

        int bufferLength = BlockEncoder.toFlatBufBlock(block).length;
        checkNotNull(blockSizeTextField).textProperty().set(Integer.toString(bufferLength));
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    // ----------------------------------------------------------------------

    PhraseBlock formPhraseBlock(boolean useRealEntropy) {
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

    public void newPhraseHistory() {
        try {
            if (phraseTemplate == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Phrase Template not chosen, please choose.", ButtonType.OK);
                LOGGER.error("Phrase Template not chosen");
                alert.showAndWait();
                return;
            }

            phraseTemplatesBlock.wordTemplates();
            List<DialogWord> dialogWords = phraseTemplate.wordTemplateIds().stream()
                    .map(
                    wordTemplateId -> {
                        Optional<PhraseTemplatesBlock.WordTemplate> wordTemplateOpt = phraseTemplatesBlock.wordTemplates().stream()
                                .filter(word -> wordTemplateId.equals(word.wordTemplateId()))
                                .findFirst();
                        PhraseTemplatesBlock.WordTemplate wordTemplate = checkNotNull(wordTemplateOpt).get();
                        List<char[]> symbolSets = wordTemplate.symbolSetIds().stream().map(
                                symbolSetId -> {
                                    Optional<char[]> symbolSetOpt = symbolSetsBlock.symbolSets().stream()
                                        .filter(symbolSet -> symbolSetId == symbolSet.symbolSetId() )
                                        .map(SymbolSetsBlock.SymbolSet::symbolSet)
                                        .findFirst();
                                    return symbolSetOpt.get();
                                }
                        ).toList();

                        return new DialogWord(wordTemplate.getName(),
                                "",
                                wordTemplate.minLength(),
                                wordTemplate.maxLength(),
                                PhraseTemplatesBlock.isUserEditable(wordTemplate.permissions()),
                                PhraseTemplatesBlock.isGenerateable(wordTemplate.permissions()),
                                PhraseTemplatesBlock.isViewable(wordTemplate.permissions()),
                                symbolSets,
                                false
                            );
                    }
            ).toList();

            //TODO: remove test
            /*dialogWords = List.of(
                    new DialogWord("login", "hello", 10, 10, true, false, true, null, false),
                    new DialogWord("password", "world", 10, 10, false, true, false,
                            List.of("qwertyuiopasdfghjklzxcvbnm1234567890".toCharArray()), false),
                    new DialogWord("unknown", "incompatible", 10, 10, true, true, false, null, true),
                    new DialogWord("new", "wow", 10, 10, false, true, true, List.of("qwertyuiopasdfghjklzxcvbnm1234567890".toCharArray()), false),
                    new DialogWord("2new", "2wow", 10, 10, true, true, false, List.of("qwertyuiopasdfghjklzxcvbnm1234567890".toCharArray()), false)
            );*/

            PhraseWordsDialog phraseWordsDialog = new PhraseWordsDialog(dialogWords);
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(stage),
                    stage -> { phraseWordsDialog.setStage(stage); return phraseWordsDialog; },
                    "Phrase",
                    null,
                    true);

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            UIPhraseHistory phraseHistory = phraseWordsDialog.getPhraseHistory();
                            if (phraseHistory != null) {
                                phraseHistoryList.add(phraseHistory);
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error picking Symbol Set: " + e, ButtonType.OK);
                            LOGGER.error("Error picking Symbol Set: ", e);
                            alert.showAndWait();
                        }
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error picking Symbol Set: " + e, ButtonType.OK);
            LOGGER.error("Error picking Symbol Set: ", e);
            alert.showAndWait();
        }
    }

    public void updatePhraseHistory() {
        //
    }

    public void deletePhraseHistory() {
        //
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
/*        try {
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
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error picking Symbol Set: " + e, ButtonType.OK);
                            LOGGER.error("Error picking Symbol Set: ", e);
                            alert.showAndWait();
                        }
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error picking Symbol Set: " + e, ButtonType.OK);
            LOGGER.error("Error picking Symbol Set: ", e);
            alert.showAndWait();
        }*/
    }
}
