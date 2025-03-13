package com.phraser.forms;

import com.phraser.db.Block;
import com.phraser.db.FoldersBlock;
import com.phraser.db.Icon;
import com.phraser.db.ImmutableFoldersBlock;
import com.phraser.db.ImmutableKeyBlock;
import com.phraser.db.ImmutablePhraseTemplatesBlock;
import com.phraser.db.ImmutableSymbolSetsBlock;
import com.phraser.db.KeyBlock;
import com.phraser.db.PhraseTemplatesBlock;
import com.phraser.db.SymbolSetsBlock;
import com.phraser.utils.PhraserUtils;

import javax.crypto.SecretKey;
import java.util.List;

import static com.phraser.db.PhraseTemplatesBlock.getWordPermissions;

public class DefaultDBCreator {
    public static final char[] DIGITS = "0123456789".toCharArray();
    public static final char[] LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    public static final char[] UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    public static final char[] LOWERCASE = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    public static final char[] SPECIAL = "%#!*^@$&".toCharArray();
    public static final char[] MIN_SPECIAL = "#!?".toCharArray();
    public static final char[] EXT_SPECIAL = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".toCharArray();
    public static final char[] SPACE = " ".toCharArray();

    public static final List<char[]> DEFAULT_SYMBOL_SETS = List.of(DIGITS, LETTERS, MIN_SPECIAL);

    static List<Block> initDefaultBlockConfig(String dbName) {
        // 1. KeyBlock
        int bucketCount = 256;
        SecretKey aesKey = PhraserUtils.getAes256Key();
        byte[] key = aesKey.getEncoded();
        byte[] iv = PhraserUtils.generateAesIv();
        KeyBlock storeKeyBlock = ImmutableKeyBlock.builder()
                .blockId(1)
                .version(1)
                .bucketCount(bucketCount)
                .entropy(PhraserUtils.generateEntropy())
                .key(key)
                .iv(iv)
                .dbName(dbName)
                .build();
        Block keyBlock = Block.create(storeKeyBlock);

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
        Block symbolSetsBlock = Block.create(storeSymbolSetsBlock);

        // 3. FoldersBlock
        List<FoldersBlock.Folder> folders = List.of(
                FoldersBlock.Folder.of(1, 0, "Websites"),
                FoldersBlock.Folder.of(2, 0, "Computers"),
                FoldersBlock.Folder.of(3, 1, "Banking"),
                FoldersBlock.Folder.of(4, 1, "Social"),
                FoldersBlock.Folder.of(5, 2, "Laptops"),
                FoldersBlock.Folder.of(6, 2, "Servers")
        );
        FoldersBlock storeFoldersBlock = ImmutableFoldersBlock.builder()
                .blockId(3)
                .version(3)
                .entropy(PhraserUtils.generateEntropy())
                .addAllFolders(folders)
                .build();
        Block foldersBlock = Block.create(storeFoldersBlock);

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
                )
        );

        List<PhraseTemplatesBlock.PhraseTemplate> phraseTemplates = List.of(
                PhraseTemplatesBlock.PhraseTemplate.of(1,
                        "Login/Pass",
                        List.of(1, 2)),
                PhraseTemplatesBlock.PhraseTemplate.of(2,
                        "OS/encrypted drive",
                        List.of(1, 2, 5)),
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
        Block phraseTemplatesBlock = Block.create(storePhraseTemplatesBlock);

        return List.of(keyBlock, symbolSetsBlock, foldersBlock, phraseTemplatesBlock);
    }
}
