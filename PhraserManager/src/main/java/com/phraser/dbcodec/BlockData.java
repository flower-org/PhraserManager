package com.phraser.dbcodec;

import com.phraser.db.BlockType;

public class BlockData {
    final BlockType blockType;
    final byte[] blockData;

    public BlockData(BlockType blockType, byte[] blockData) {
        this.blockType = blockType;
        this.blockData = blockData;
    }
}
