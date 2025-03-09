package com.phraser.db;

public enum BlockType {
    FOLDERS_BLOCK((byte)1),
    SYMBOL_SETS_BLOCK((byte)2),
    PHRASE_TEMPLATES_BLOCK((byte)3),
    PHRASE_BLOCK((byte)4),
    KEY_BLOCK((byte)5);

    public final byte code;

    BlockType(byte code) {
        this.code = code;
    }

    public static BlockType fromCode(byte code) {
        switch (code) {
            case 1: return BlockType.FOLDERS_BLOCK;
            case 2: return BlockType.SYMBOL_SETS_BLOCK;
            case 3: return BlockType.PHRASE_TEMPLATES_BLOCK;
            case 4: return BlockType.PHRASE_BLOCK;
            case 5: return BlockType.KEY_BLOCK;
            default: throw new RuntimeException("Unknown BlockType code " + code);
        }
    }
}
