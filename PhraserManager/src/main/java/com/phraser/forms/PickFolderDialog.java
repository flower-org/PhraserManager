package com.phraser.forms;

import com.phraser.db.FoldersBlock;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class PickFolderDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(PickFolderDialog.class);

    @Nullable Stage stage;

    @Nullable UIFolder folder;
    @Nullable @FXML TableView<UIFolder> foldersTableView;

    public static class UIFolder {
        final FoldersBlock.Folder folder;
        final List<FoldersBlock.Folder> folders;

        UIFolder(FoldersBlock.Folder folder, List<FoldersBlock.Folder> folders) {
            this.folder = folder;
            this.folders = folders;
        }

        public int getId() { return folder.folderId(); }
        public String getPath() {
            return FoldersBlock.getPath(folder, folders);
        }
    }

    public PickFolderDialog(List<FoldersBlock.Folder> folders) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("PickFolderDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        ObservableList<UIFolder> list = FXCollections.observableArrayList();
        list.addAll(folders.stream().map(f -> new UIFolder(f, folders)).toList());
        checkNotNull(foldersTableView).itemsProperty().set(list);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Nullable
    public UIFolder getFolder() {
        return folder;
    }

    public void okClose() {
        try {
            folder = checkNotNull(foldersTableView).getSelectionModel().getSelectedItem();
            checkNotNull(stage).close();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "PickFolderDialog close Error: " + e, ButtonType.OK);
            LOGGER.error("PickFolderDialog close Error:", e);
            alert.showAndWait();
        }
    }
}
