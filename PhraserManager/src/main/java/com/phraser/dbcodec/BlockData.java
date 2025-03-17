package com.phraser.dbcodec;

import com.phraser.db.BlockType;

public class BlockData {
    public final BlockType blockType;
    public final byte[] blockData;

    public BlockData(BlockType blockType, byte[] blockData) {
        this.blockType = blockType;
        this.blockData = blockData;
    }
}
