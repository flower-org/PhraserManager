package com.phraser.db;

public enum BlockType {
    FOLDERS_BLOCK((byte)1),
    SYMBOL_SETS_BLOCK((byte)2),
    PHRASE_TEMPLATES_BLOCK((byte)3),
    PHRASE_BLOCK((byte)4),
    KEY_BLOCK((byte)5);

    final byte code;

    BlockType(byte code) {
        this.code = code;
    }
}
