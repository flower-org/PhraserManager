package com.phraser.forms;

import com.phraser.db.FoldersBlock;
import com.phraser.runtimedb.DbRuntime;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClientModeForm extends AnchorPane {
    final static Logger LOGGER = LoggerFactory.getLogger(ClientModeForm.class);

    public enum ExplorerNodeType {
        UP,
        FOLDER,
        PHRASE
    }
    public static class ExplorerNode {
        final ExplorerNodeType type;
        final String name;
        final int id;

        ExplorerNode(ExplorerNodeType type, String name, int id) {
            this.type = type;
            this.name = name;
            this.id = id;
        }

        public ExplorerNodeType getType() { return type; }
        public String getName() { return name; }
        public String getId() { return type == ExplorerNodeType.UP ? "" : Integer.toString(id); }
    }

    @FXML @Nullable TitledPane foldersTitledPane;
    @FXML @Nullable TableView<ExplorerNode> foldersTableView;
    final ObservableList<ExplorerNode> folderContent;

    @FXML @Nullable TitledPane phraseTitledPane;
    @FXML @Nullable TableView phraseTableView;

    @FXML @Nullable TitledPane phraseHistoryTitledPane;
    @FXML @Nullable TableView phraseHistoryTableView;

    @Nullable Stage stage;
    @Nullable Tab tab;
    final MainForm mainForm;
    final DbRuntime dbRuntime;
    final Stack<String> path;

    protected int currentFolderId;

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

                }
            }
        }
    }

    public void phraseHistoryTableViewClicked(MouseEvent mouseEvent) {
    }

    public void phraseTableViewClicked(MouseEvent mouseEvent) {
    }
}
