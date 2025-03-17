package com.phraser.forms;

import com.phraser.runtimedb.DbRuntime;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClientModeForm extends AnchorPane {
    final static Logger LOGGER = LoggerFactory.getLogger(ClientModeForm.class);

    @Nullable Stage stage;
    @Nullable Tab tab;
    final MainForm mainForm;
    final DbRuntime dbRuntime;

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
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setTab(Tab tab) {
        this.tab = tab;
        checkNotNull(tab).setText(dbRuntime.getDbName());
    }
}
