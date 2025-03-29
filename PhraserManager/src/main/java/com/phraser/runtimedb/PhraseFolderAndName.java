package com.phraser.runtimedb;

public class PhraseFolderAndName {
    public final int phraseBlockId;
    public final int folderId;
    public final String name;

    public PhraseFolderAndName(int phraseBlockId, int folderId, String name) {
        this.phraseBlockId = phraseBlockId;
        this.folderId = folderId;
        this.name = name;
    }
}
