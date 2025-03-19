package com.phraser.forms;

import com.phraser.db.Block;
import com.phraser.db.FoldersBlock;
import com.phraser.db.Icon;
import com.phraser.db.ImmutableFoldersBlock;
import com.phraser.db.ImmutableKeyBlock;
import com.phraser.db.ImmutablePhraseBlock;
import com.phraser.db.ImmutablePhraseHistory;
import com.phraser.db.ImmutablePhraseTemplatesBlock;
import com.phraser.db.ImmutableSymbolSetsBlock;
import com.phraser.db.ImmutableWord;
import com.phraser.db.KeyBlock;
import com.phraser.db.PhraseBlock;
import com.phraser.db.PhraseTemplatesBlock;
import com.phraser.db.SymbolSetsBlock;
import com.phraser.utils.PhraserUtils;

import javax.crypto.SecretKey;
import java.util.List;

import static com.phraser.db.Block.FLASH_SECTOR_SIZE;
import static com.phraser.utils.PhraserUtils.getWordPermissions;

public class DefaultDBCreator {
    // TODO: determine a practical default DB size rather than based on total usable space on device.
    //  E.g. 64 blocks is much faster startup, or 128 blocks might be a good compromise.
    //  The goal would be to find the best balance between practical performance (notably, startup time)
    //  on target HW, while still maintaining enough DB capacity for all practical purposes.
    public static final int DEFAULT_BLOCKS_IN_DB = (1024 * 1024) / FLASH_SECTOR_SIZE; // 256 blocks in 1 mb

    public static final char[] DIGITS = "0123456789".toCharArray();
    public static final char[] LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    public static final char[] UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    public static final char[] LOWERCASE = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    public static final char[] SPECIAL = "%#!*^@$&".toCharArray();
    public static final char[] MIN_SPECIAL = "#!?".toCharArray();
    public static final char[] EXT_SPECIAL = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".toCharArray();
    public static final char[] SPACE = " ".toCharArray();

    public static final List<char[]> DEFAULT_SYMBOL_SETS = List.of(DIGITS, LETTERS, MIN_SPECIAL);

    public static List<Block> initDefaultBlockConfig(String dbName) {
        Block keyBlock = getKeyBlock(dbName);
        Block symbolSetsBlock = getSymbolSetsBlock();
        Block foldersBlock = getFoldersBlock();
        Block phraseTemplatesBlock = getPhraseTemplatesBlock();

        return List.of(keyBlock, symbolSetsBlock, foldersBlock, phraseTemplatesBlock);
    }

    public static List<Block> initDefaultBlockConfigWithPhrase(String dbName) {
        Block keyBlock = getKeyBlock(dbName);
        Block symbolSetsBlock = getSymbolSetsBlock();
        Block foldersBlock = getFoldersBlock();
        Block phraseTemplatesBlock = getPhraseTemplatesBlock();
        Block phraseBlock = getPhraseBlock();

        return List.of(keyBlock, symbolSetsBlock, foldersBlock, phraseTemplatesBlock, phraseBlock);
    }

    public static Block getKeyBlock(String dbName) {
        // 1. KeyBlock
        SecretKey aesKey = PhraserUtils.getAes256Key();
        byte[] key = aesKey.getEncoded();
        byte[] iv = PhraserUtils.generateAesIv();
        KeyBlock storeKeyBlock = ImmutableKeyBlock.builder()
                .blockId(1)
                .version(1)
                .entropy(PhraserUtils.generateEntropy())
                .blockCount(DEFAULT_BLOCKS_IN_DB)
                .key(key)
                .iv(iv)
                .dbName(dbName)
                .build();
        return Block.of(storeKeyBlock);
    }

    public static Block getSymbolSetsBlock() {
        // 2. SymbolSetsBlock
        List<SymbolSetsBlock.SymbolSet> symbolSets = List.of(
                SymbolSetsBlock.SymbolSet.of(1, "Digits", DIGITS),
                SymbolSetsBlock.SymbolSet.of(2, "Letters", LETTERS),
                SymbolSetsBlock.SymbolSet.of(3, "Uppercase", UPPERCASE),
                SymbolSetsBlock.SymbolSet.of(4, "Lowercase", LOWERCASE),
                SymbolSetsBlock.SymbolSet.of(5, "Special", SPECIAL),
                SymbolSetsBlock.SymbolSet.of(6, "Min special", MIN_SPECIAL),
                SymbolSetsBlock.SymbolSet.of(7, "Ext special", EXT_SPECIAL),
                SymbolSetsBlock.SymbolSet.of(8, "Space", SPACE)
        );

        SymbolSetsBlock storeSymbolSetsBlock = ImmutableSymbolSetsBlock.builder()
                .blockId(2)
                .version(2)
                .entropy(PhraserUtils.generateEntropy())
                .addAllSymbolSets(symbolSets)
                .build();
        return Block.of(storeSymbolSetsBlock);
    }

    public static Block getFoldersBlock() {
        // 3. FoldersBlock
        List<FoldersBlock.Folder> folders = List.of(
                FoldersBlock.Folder.of(1, 0, "Websites"),
                FoldersBlock.Folder.of(2, 0, "Computers"),
                FoldersBlock.Folder.of(3, 1, "Social"),
                FoldersBlock.Folder.of(4, 1, "Finance"),
                FoldersBlock.Folder.of(5, 2, "Laptops"),
                FoldersBlock.Folder.of(6, 2, "Servers")
        );
        FoldersBlock storeFoldersBlock = ImmutableFoldersBlock.builder()
                .blockId(3)
                .version(3)
                .entropy(PhraserUtils.generateEntropy())
                .addAllFolders(folders)
                .build();
        return Block.of(storeFoldersBlock);
    }

    public static Block getPhraseTemplatesBlock() {
        // 4. PhraseTemplatesBlock
        List<PhraseTemplatesBlock.WordTemplate> wordTemplates = List.of(
                PhraseTemplatesBlock.WordTemplate.of(1,
                        getWordPermissions(false, true, true, true),
                        Icon.LOGIN,
                        4,
                        256,
                        "username",
                        List.of()
                ),
                PhraseTemplatesBlock.WordTemplate.of(2,
                        getWordPermissions(true, false, true, false),
                        Icon.KEY,
                        24,
                        64,
                        "password",
                        List.of(1,2,7)
                ),
                PhraseTemplatesBlock.WordTemplate.of(3,
                        getWordPermissions(false, true, false, true),
                        Icon.QUESTION,
                        0,
                        256,
                        "question",
                        List.of()//empty since it's not generateable
                ),
                PhraseTemplatesBlock.WordTemplate.of(4,
                        getWordPermissions(true, true, true, true),
                        Icon.MESSAGE,
                        24,
                        64,
                        "answer",
                        List.of(1,2,8)
                ),
                PhraseTemplatesBlock.WordTemplate.of(5,
                        getWordPermissions(true, false, true, false),
                        Icon.LOCK,
                        24,
                        64,
                        "drive password",
                        List.of(1,2,5)
                ),
                PhraseTemplatesBlock.WordTemplate.of(6,
                        getWordPermissions(true, true, true, true),
                        Icon.LOGIN,
                        8,
                        24,
                        "generated login",
                        List.of(1,2)
                ),
                PhraseTemplatesBlock.WordTemplate.of(7,
                        getWordPermissions(true, true, true, true),
                        Icon.SETTINGS,
                        8,
                        10,
                        "bios password",
                        List.of(1,2,6)
                )
        );

        List<PhraseTemplatesBlock.PhraseTemplate> phraseTemplates = List.of(
                PhraseTemplatesBlock.PhraseTemplate.of(1,
                        "Login/Pass",
                        List.of(1, 2)),
                PhraseTemplatesBlock.PhraseTemplate.of(2,
                        "Computer",
                        List.of(1, 2, 5, 7)),
                PhraseTemplatesBlock.PhraseTemplate.of(3,
                        "3 Security questions",
                        List.of(1, 2, 3, 4, 3, 4, 3, 4)),
                PhraseTemplatesBlock.PhraseTemplate.of(4,
                        "Generated Login/Pass",
                        List.of(6, 2))
        );

        PhraseTemplatesBlock storePhraseTemplatesBlock = ImmutablePhraseTemplatesBlock.builder()
                .blockId(4)
                .version(4)
                .entropy(PhraserUtils.generateEntropy())
                .addAllPhraseTemplates(phraseTemplates)
                .addAllWordTemplates(wordTemplates)
                .build();
        return Block.of(storePhraseTemplatesBlock);
    }

    public static Block getPhraseBlock() {
        // 5. Phrase (optional)
        List<PhraseBlock.PhraseHistory> history = List.of(
                ImmutablePhraseHistory.builder()
                        .phraseTemplateId(3)//"3 Security questions"
                        .phrase(List.of(
                                ImmutableWord.builder()
                                        .wordTemplateId(1)
                                        .name("username")
                                        .word("admin")
                                        .permissions(getWordPermissions(false, true, true, true))
                                        .icon(Icon.LOGIN)
                                        .build(),
                                ImmutableWord.builder()
                                        .wordTemplateId(2)
                                        .name("password")
                                        .word("qwerty")
                                        .permissions(getWordPermissions(true, false, true, false))
                                        .icon(Icon.KEY)
                                        .build(),
                                ImmutableWord.builder()
                                        .wordTemplateId(3)
                                        .name("question")
                                        .word("Question 1")
                                        .permissions(getWordPermissions(false, true, false, true))
                                        .icon(Icon.QUESTION)
                                        .build(),
                                ImmutableWord.builder()
                                        .wordTemplateId(4)
                                        .name("answer")
                                        .word("Answer 1")
                                        .permissions(getWordPermissions(true, false, true, false))
                                        .icon(Icon.MESSAGE)
                                        .build(),
                                ImmutableWord.builder()
                                        .wordTemplateId(3)
                                        .name("question")
                                        .word("Question 2")
                                        .permissions(getWordPermissions(false, true, false, true))
                                        .icon(Icon.QUESTION)
                                        .build(),
                                ImmutableWord.builder()
                                        .wordTemplateId(4)
                                        .name("answer")
                                        .word("Answer 2")
                                        .permissions(getWordPermissions(true, false, true, false))
                                        .icon(Icon.MESSAGE)
                                        .build(),
                                ImmutableWord.builder()
                                        .wordTemplateId(3)
                                        .name("question")
                                        .word("Question 3")
                                        .permissions(getWordPermissions(false, true, false, true))
                                        .icon(Icon.QUESTION)
                                        .build(),
                                ImmutableWord.builder()
                                        .wordTemplateId(4)
                                        .name("answer")
                                        .word("Answer 3")
                                        .permissions(getWordPermissions(true, false, true, false))
                                        .icon(Icon.MESSAGE)
                                        .build()
                        ))
                        .build(),
                ImmutablePhraseHistory.builder()
                        .phraseTemplateId(1)//Login/Pass
                        .phrase(List.of(
                                ImmutableWord.builder()
                                        .wordTemplateId(1)
                                        .name("username")
                                        .word("admin")
                                        .permissions(getWordPermissions(false, true, true, true))
                                        .icon(Icon.LOGIN)
                                        .build(),
                                ImmutableWord.builder()
                                        .wordTemplateId(2)
                                        .name("password")
                                        .word("qwerty")
                                        .permissions(getWordPermissions(true, false, true, false))
                                        .icon(Icon.KEY)
                                        .build()
                        ))
                        .build()
        );

        PhraseBlock storePhraseBlock = ImmutablePhraseBlock.builder()
                .blockId(3)
                .version(3)
                .entropy(PhraserUtils.generateEntropy())
                .phraseTemplateId(3)//"3 Security questions"
                .folderId(2)// \Computers
                .isTombstone(false)
                .phraseName("Gosuslugi")
                .history(history)
                .build();
        return Block.of(storePhraseBlock);
    }
}
