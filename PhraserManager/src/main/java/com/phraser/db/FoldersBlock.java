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

    default String getPath(FoldersBlock.Folder folder) {
        return getPath(folder, folders());
    }

    static String getPath(FoldersBlock.Folder folder, List<FoldersBlock.Folder> folders) {
        if (folder.parentFolderId() == 0) {
            return "/" + folder.folderName();
        } else {
            FoldersBlock.Folder parentFolder = folders.stream()
                    .filter(f -> f.folderId() == folder.parentFolderId()).findFirst()
                    .get();

            String parentPath = getPath(parentFolder, folders);
            return parentPath + "/" + folder.folderName();
        }
    }
}
