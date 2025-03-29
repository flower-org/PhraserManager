package com.phraser.runtimedb;

import com.phraser.db.Block;
import com.phraser.db.FoldersBlock;
import com.phraser.db.PhraseBlock;
import com.phraser.db.PhraseTemplatesBlock;
import com.phraser.db.SymbolSetsBlock;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public interface DbRuntime {
    FolderContent getFolderContent(int folderId);
    @Nullable FoldersBlock.Folder getFolder(int folderId);
    String getDbName();
    @Nullable PhraseBlock getPhrase(int phraseBlockId) throws IOException;
    @Nullable PhraseTemplatesBlock.PhraseTemplate getPhraseTemplate(int phraseTemplateId);
    @Nullable PhraseTemplatesBlock.WordTemplate getWordTemplate(int wordTemplateId);
    @Nullable SymbolSetsBlock.SymbolSet getSymbolSet(int symbolSetId);
    Block readPhraseTemplatesBlock() throws IOException;
    void updateBlock(Block mainBlock);
    List<SymbolSetsBlock.SymbolSet> getSymbolSets();
    Block readSymbolSetsBlock() throws IOException;
    void addFolder(String folderName, int parentFolderId) throws IOException;
    void renameFolder(int folderId, String newFolderName) throws IOException;
    boolean isFolderEmpty(int folderId);
    void removeFolder(int folderId) throws IOException;
    void tombstonePhrase(int phraseBlockId) throws IOException;
    List<PhraseTemplatesBlock.PhraseTemplate> getPhraseTemplates();
    void updatePhraseTemplate(int phraseBlockId, int phraseTemplateId) throws IOException;
    List<FoldersBlock.Folder> getFolders();
    void updatePhraseFolder(int phraseBlockId, int folderId) throws IOException;
    void renamePhrase(int phraseBlockId, String newPhraseName) throws IOException, MaxBlockSizeExceededException;
    void createPhrase(int phraseTemplateId, int folderId, String phraseName);
    void makeHistoryEntryCurrent(int phraseBlockId, int historyEntryIndex) throws IOException;
    void deleteHistoryEntry(int phraseBlockId, int historyEntryIndex) throws IOException;
    void generatePhraseWord(int phraseBlockId, int wordTemplateIdToUpdate, int wordTemplateOrdinal, boolean autoTruncateHistory) throws IOException, BlockDataSizeExceededException;
    void updatePhraseWord(int phraseBlockId, int wordTemplateIdToUpdate, int wordTemplateOrdinal, String newWord, boolean autoTruncateHistory) throws IOException, BlockDataSizeExceededException;
}
