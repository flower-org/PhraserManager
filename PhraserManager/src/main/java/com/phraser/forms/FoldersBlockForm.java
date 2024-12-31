package com.phraser.forms;

import com.phraser.db.Block;
import com.phraser.db.FoldersBlock;
import com.phraser.db.ImmutableFolder;
import com.phraser.db.ImmutableFoldersBlock;
import com.phraser.db.PhraserDB;
import com.phraser.utils.PhraserUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.phraser.db.Block.DATA_BLOCK_SIZE;
import static com.phraser.forms.PhraserDbForm.NEW_BLOCK;

public class FoldersBlockForm extends AnchorPane {
    final static Logger LOGGER = LoggerFactory.getLogger(FoldersBlockForm.class);

    @FXML @Nullable TreeView<FolderStruct> foldersTreeView;
    TreeItem<FolderStruct> rootItem;
    @Nullable TreeItem<FolderStruct> currentItem = null;


    @FXML @Nullable TextField blockIdTextField;
    @FXML @Nullable TextField versionTextField;
    @FXML @Nullable TextField blockSizeTextField;
    @FXML @Nullable TextField folderNameTextField;
    @FXML @Nullable TextField subFolderNameTextField;

    @Nullable Stage stage;
    final PhraserDB phraserDB;
    final Consumer<FoldersBlock> foldersBlockCallback;

    @Nullable final Block foldersBlock;

    int maxFolderId = 0;

    static class FolderStruct {
        final int folderId;
        String folderName;

        public FolderStruct(int folderId, String folderName) {
            this.folderId = folderId;
            this.folderName = folderName;
        }

        @Override
        public String toString() {
            return "(" + folderId + ") " + folderName;
        }
    }

    public FoldersBlockForm(@Nullable Block foldersBlock, PhraserDB phraserDB,
                            Consumer<FoldersBlock> foldersBlockCallback) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("FoldersBlockForm.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        rootItem = new TreeItem<>(new FolderStruct(0, "/"));
        checkNotNull(foldersTreeView).rootProperty().set(rootItem);

        checkNotNull(foldersTreeView).getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                currentItem = newValue;
                checkNotNull(folderNameTextField).setText(currentItem.getValue().folderName);
            }
        );

        this.foldersBlock = foldersBlock;
        if (foldersBlock != null) {
            checkNotNull(blockIdTextField).textProperty().set(Integer.toString(foldersBlock.getBlockId()));
            checkNotNull(versionTextField).textProperty().set(Integer.toString(foldersBlock.getVersion()));

            fillFolders(foldersBlock);
        } else {
            checkNotNull(blockIdTextField).textProperty().set(NEW_BLOCK);
            checkNotNull(versionTextField).textProperty().set(NEW_BLOCK);
        }

        this.phraserDB = phraserDB;
        this.foldersBlockCallback = foldersBlockCallback;

        checkNotNull(folderNameTextField).setTextFormatter(new TextFormatter<>(change -> {
                if (currentItem == null ||
                    (currentItem.parentProperty().getValue() == null && !change.getText().equals(currentItem.getValue().folderName) )) {
                    return null;
                }

                String newText = change.getControlNewText();
                if (!newText.startsWith(" ") && newText.matches("[0-9a-zA-Z!#$%&'()-@^_{}~ ]*")) {
                    return change;
                }
                return null;
            }
        ));

        checkNotNull(subFolderNameTextField).setTextFormatter(new TextFormatter<>(change -> {
                String newText = change.getControlNewText();
                if (!newText.startsWith(" ") && newText.matches("[0-9a-zA-Z!#$%&'()-@^_{}~ ]*")) {
                    return change;
                }
                return null;
            }
        ));

        checkNotNull(foldersTreeView).getSelectionModel().select(rootItem);
        rootItem.expandedProperty().set(true);

        updateBlockSize();
    }

    public void fillFolders(Block foldersBlock) {
        List<FoldersBlock.Folder> folders = checkNotNull(foldersBlock.foldersBlock()).folders();

        Map<Integer, List<FoldersBlock.Folder>> foldersByParent = new HashMap<>();
        for (FoldersBlock.Folder folder : folders) {
            foldersByParent.computeIfAbsent(folder.parentFolderId(), k -> new ArrayList<>()).add(folder);
        }

        fillFolders(0, rootItem, foldersByParent);
    }

    public void fillFolders(int parentId, TreeItem<FolderStruct> parentNode,
                            Map<Integer, List<FoldersBlock.Folder>> foldersByParent) {
        List<FoldersBlock.Folder> childFolders = foldersByParent.get(parentId);

        if (childFolders != null) {
            childFolders.sort(Comparator.comparingInt(FoldersBlock.Folder::folderId));

            for (FoldersBlock.Folder childFolder : childFolders) {
                int childFolderId = childFolder.folderId();
                String childFolderName = childFolder.folderName();

                TreeItem<FolderStruct> childNode = new TreeItem<>(new FolderStruct(childFolderId, childFolderName));
                fillFolders(childFolderId, childNode, foldersByParent);

                parentNode.getChildren().add(childNode);
            }
        }
    }

    public void saveToDb() {
        FoldersBlock newFoldersBlock = formFoldersBlock(true);
        Block block = Block.create(newFoldersBlock);

        int bufferLength = block.toFlatBufBlock().length;

        if (bufferLength > DATA_BLOCK_SIZE) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Block size can't exceed "+DATA_BLOCK_SIZE+" bytes", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        // This call will close the form and process the formed block
        foldersBlockCallback.accept(newFoldersBlock);
    }

    public void addSubFolder() {
        if (currentItem != null) {
            String folderName = checkNotNull(subFolderNameTextField).getText();
            if (StringUtils.isBlank(folderName)) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "FolderName can't be blank", ButtonType.OK);
                alert.showAndWait();
                return;
            }

            maxFolderId++;
            currentItem.getChildren().add(new TreeItem<>(new FolderStruct(maxFolderId, folderName)));
            currentItem.expandedProperty().set(true);

            updateBlockSize();
        }
    }

    public void removeFolder() {
        if (currentItem != null) {
            TreeItem<FolderStruct> parent = currentItem.parentProperty().getValue();
            if (parent != null) {
                parent.getChildren().remove(currentItem);
            }

            updateBlockSize();
        }
    }

    public void updateFolderName() {
        if (currentItem != null && currentItem.parentProperty().getValue() != null) {
            String folderName = checkNotNull(folderNameTextField).getText();
            if (StringUtils.isBlank(folderName)) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "FolderName can't be blank", ButtonType.OK);
                alert.showAndWait();
                return;
            }

            FolderStruct oldFolder = currentItem.getValue();
            FolderStruct newFolder = new FolderStruct(oldFolder.folderId, folderName);

            currentItem.setValue(newFolder);

            updateBlockSize();
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    void updateBlockSize() {
        Block block = Block.create(formFoldersBlock(false));

        int bufferLength = block.toFlatBufBlock().length;
        checkNotNull(blockSizeTextField).textProperty().set(Integer.toString(bufferLength));
    }

    List<FoldersBlock.Folder> formFolderList() {
        List<FoldersBlock.Folder> folderlist = new ArrayList<>();
        addFoldersToList(folderlist, rootItem);
        return folderlist;
    }

    void addFoldersToList(List<FoldersBlock.Folder> foldersList, TreeItem<FolderStruct> folderItem) {
        int parentFolderId = folderItem.getValue().folderId;

        for (TreeItem<FolderStruct> child : folderItem.getChildren()) {
            FoldersBlock.Folder childFolder = ImmutableFolder.builder()
                    .folderId(child.getValue().folderId)
                    .parentFolderId(parentFolderId)
                    .folderName(child.getValue().folderName)
                    .build();

            foldersList.add(childFolder);

            addFoldersToList(foldersList, child);
        }
    }

    FoldersBlock formFoldersBlock(boolean useRealEntropy) {
        return ImmutableFoldersBlock.builder()
                .blockId(foldersBlock == null ? -1 : foldersBlock.getBlockId())
                .version(123)
                .entropy(useRealEntropy ? PhraserUtils.generateEntropy() : 123L)
                .addAllFolders(formFolderList())
                .build();
    }
}
