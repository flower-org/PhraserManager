package com.phraser.forms;

import com.flower.fxutils.ModalWindow;
import com.phraser.db.Block;
import com.phraser.db.ImmutableSymbolSet;
import com.phraser.db.ImmutableSymbolSetsBlock;
import com.phraser.db.PhraserDB;
import com.phraser.db.SymbolSetsBlock;
import com.phraser.dbcodec.FlatBufBlockEncoder;
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
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.phraser.db.Block.DATA_BLOCK_SIZE;
import static com.phraser.forms.PhraserDbForm.NEW_BLOCK;

public class SymbolSetsBlockForm extends AnchorPane {
    final static Logger LOGGER = LoggerFactory.getLogger(SymbolSetsBlockForm.class);

    @FXML @Nullable TableView<SymbolSetsBlock.SymbolSet> symbolSetTableView;
    final ObservableList<SymbolSetsBlock.SymbolSet> symbolSets;

    @FXML @Nullable TextField blockIdTextField;
    @FXML @Nullable TextField versionTextField;
    @FXML @Nullable TextField blockSizeTextField;
    @FXML @Nullable TextField entropyTextField;
    @Nullable Stage stage;
    @Nullable final Block symbolSetsBlock;
    final PhraserDB phraserDB;
    final Consumer<SymbolSetsBlock> symbolSetsBlockCallback;

    int maxSymbolSetId = 0;

    public SymbolSetsBlockForm(@Nullable Block symbolSetsBlock, PhraserDB phraserDB,
                               Consumer<SymbolSetsBlock> symbolSetsBlockCallback) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SymbolSetsBlockForm.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        this.symbolSets = FXCollections.observableArrayList();
        this.symbolSetsBlock = symbolSetsBlock;
        if (symbolSetsBlock != null) {
            checkNotNull(blockIdTextField).textProperty().set(Integer.toString(symbolSetsBlock.getBlockId()));
            checkNotNull(versionTextField).textProperty().set(Long.toString(symbolSetsBlock.getVersion()));
            for (SymbolSetsBlock.SymbolSet symbolSet : checkNotNull(symbolSetsBlock.symbolSetsBlock()).symbolSets()) {
                addSymbolSetToList(symbolSet);
            }
            checkNotNull(entropyTextField).textProperty().set(Long.toString(symbolSetsBlock.getEntropy()));
        } else {
            checkNotNull(blockIdTextField).textProperty().set(NEW_BLOCK);
            checkNotNull(versionTextField).textProperty().set(NEW_BLOCK);
            checkNotNull(entropyTextField).textProperty().set(NEW_BLOCK);
        }

        checkNotNull(symbolSetTableView).itemsProperty().set(symbolSets);

        this.phraserDB = phraserDB;
        this.symbolSetsBlockCallback = symbolSetsBlockCallback;

        updateBlockSize();
    }

    public void saveToDb() {
        SymbolSetsBlock newSymbolSetsBlock = formSymbolSetsBlock(true);
        Block block = Block.create(newSymbolSetsBlock);

        int bufferLength = FlatBufBlockEncoder.toFlatBufBlock(block).length;

        if (bufferLength > DATA_BLOCK_SIZE) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Block size can't exceed "+DATA_BLOCK_SIZE+" bytes", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        // This call will close the form and process the formed block
        symbolSetsBlockCallback.accept(newSymbolSetsBlock);
    }

    public void addSymbolSetToList(SymbolSetsBlock.SymbolSet symbolSet) {
        maxSymbolSetId = Math.max(maxSymbolSetId, symbolSet.symbolSetId());
        symbolSets.add(symbolSet);
    }

    public void add() {
        try {
            SymbolSetDialog symbolSetDialog = new SymbolSetDialog(null);
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(stage),
                    stage -> { symbolSetDialog.setStage(stage); return symbolSetDialog; },
                    "Symbol Set");

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            SymbolSetsBlock.SymbolSet symbolSet = symbolSetDialog.getSymbolSet();
                            if (symbolSet != null) {
                                maxSymbolSetId++;
                                symbolSet = ImmutableSymbolSet.builder().from(symbolSet).symbolSetId(maxSymbolSetId).build();

                                addSymbolSetToList(symbolSet);
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding Symbol Set: " + e, ButtonType.OK);
                            LOGGER.error("Error adding Symbol Set: ", e);
                            alert.showAndWait();
                        }

                        updateBlockSize();
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding Symbol Set: " + e, ButtonType.OK);
            LOGGER.error("Error adding Symbol Set: ", e);
            alert.showAndWait();
        }
    }

    public void remove() {
        SymbolSetsBlock.SymbolSet selected = checkNotNull(symbolSetTableView).getSelectionModel().getSelectedItem();
        if (selected != null) {
            symbolSets.remove(selected);
        }
        updateBlockSize();
    }

    public void update() {
        SymbolSetsBlock.SymbolSet selectedSymbolSet = checkNotNull(symbolSetTableView).getSelectionModel().getSelectedItem();
        if (selectedSymbolSet != null) {
            int selectedIndex = symbolSets.indexOf(selectedSymbolSet);

            SymbolSetDialog symbolSetDialog = new SymbolSetDialog(selectedSymbolSet);
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(stage),
                    stage -> { symbolSetDialog.setStage(stage); return symbolSetDialog; },
                    "Symbol Set");

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            SymbolSetsBlock.SymbolSet updatedSymbolSet = symbolSetDialog.getSymbolSet();
                            if (updatedSymbolSet != null) {
                                symbolSets.set(selectedIndex, updatedSymbolSet);
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error updating Symbol Set: " + e, ButtonType.OK);
                            LOGGER.error("Error updating Symbol Set: ", e);
                            alert.showAndWait();
                        }
                        updateBlockSize();
                    }
            );
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    void updateBlockSize() {
        Block block = Block.create(formSymbolSetsBlock(false));

        int bufferLength = FlatBufBlockEncoder.toFlatBufBlock(block).length;
        checkNotNull(blockSizeTextField).textProperty().set(Integer.toString(bufferLength));
    }

    SymbolSetsBlock formSymbolSetsBlock(boolean useRealEntropy) {
        return ImmutableSymbolSetsBlock.builder()
                .blockId(symbolSetsBlock == null ? -1 : symbolSetsBlock.getBlockId())
                .version(123)
                .entropy(useRealEntropy ? PhraserUtils.generateEntropy() : 123L)
                .addAllSymbolSets(symbolSets)
                .build();
    }
}
