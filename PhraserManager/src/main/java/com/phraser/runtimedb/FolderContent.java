package com.phraser.runtimedb;

import com.phraser.db.FoldersBlock;

import java.util.List;

public class FolderContent {
    public final List<FoldersBlock.Folder> subFolders;
    public final List<PhraseFolderAndName> phrases;

    public FolderContent(List<FoldersBlock.Folder> subFolders, List<PhraseFolderAndName> phrases) {
        this.subFolders = subFolders;
        this.phrases = phrases;
    }
}

