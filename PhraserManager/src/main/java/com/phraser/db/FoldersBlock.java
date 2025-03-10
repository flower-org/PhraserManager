package com.phraser.db;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
public interface FoldersBlock extends StoreBlock {
    @Value.Immutable
    interface Folder {
        /** 16 bits */
        int folderId();
        /** 16 bits.
         * Use id=0 for root folder. */
        int parentFolderId();
        String folderName();

        static Folder of(int folderId, int parentFolderId, String folderName) {
            return ImmutableFolder.builder()
                    .folderId(folderId)
                    .parentFolderId(parentFolderId)
                    .folderName(folderName)
                    .build();
        }
    }

    List<Folder> folders();
}
