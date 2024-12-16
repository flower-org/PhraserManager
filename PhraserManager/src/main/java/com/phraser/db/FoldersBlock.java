package com.phraser.db;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
public interface FoldersBlock extends StoreBlock {
    @Value.Immutable
    interface Folder {
        /** 16 bits */
        int folderId();
        /** 16 bits */
        int parentFolderId();
        String folderName();
    }

    List<Folder> folders();
}
