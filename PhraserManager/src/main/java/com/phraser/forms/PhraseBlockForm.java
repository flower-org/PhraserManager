package com.phraser.forms;

import com.phraser.db.Block;
import com.phraser.db.ImmutablePhraseBlock;
import com.phraser.db.PhraseBlock;
import com.phraser.db.PhraserDB;
import com.phraser.dbcodec.BlockEncoder;
import com.phraser.utils.PhraserUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.phraser.forms.PhraserDbForm.NEW_BLOCK;

public class PhraseBlockForm extends AnchorPane {
    @FXML @Nullable TextField blockIdTextField;
    @FXML @Nullable TextField versionTextField;
    @FXML @Nullable TextField blockSizeTextField;
    @FXML @Nullable TableView<PhraseBlock.PhraseHistory> phraseHistoryTableView;

    ObservableList<PhraseBlock.PhraseHistory> phraseHistoryList;

    @Nullable final Block phraseBlock;
    final PhraserDB phraserDB;
    final Consumer<PhraseBlock> phraseBlockCallback;
    @Nullable Stage stage;

    public PhraseBlockForm(@Nullable Block phraseBlock,
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

        // TODO: init form

        this.phraseHistoryList = FXCollections.observableArrayList();
        checkNotNull(phraseHistoryTableView).itemsProperty().set(phraseHistoryList);

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
        List<PhraseBlock.PhraseHistory> phraseHistoryList = new ArrayList<>(this.phraseHistoryList);

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
                */

                .phraseTemplateId(0)
                .folderId(0)
                .isTombstone(true)
                .phraseName("String")

                .history(phraseHistoryList)
                .build();
    }
}
