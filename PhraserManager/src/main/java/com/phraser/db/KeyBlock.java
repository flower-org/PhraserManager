package com.phraser.db;

import com.google.flatbuffers.FlatBufferBuilder;
import com.phraser.utils.PhraserUtils;
import org.immutables.value.Value;

import static com.phraser.db.Block.BLOCK_DATA_SIZE;

@Value.Immutable
public interface KeyBlock extends StoreBlock {
    /** 32 bytes key - AES-256 */
    byte[] key();
    byte[] iv();
    String dbName();
    int bucketCount();

    static com.phraser.schema.phraser.KeyBlock createKeyBlock(byte[] key_256, byte[] iv_128) {
        assert(key_256.length == 32);
        assert(iv_128.length == 16);

        FlatBufferBuilder flatBufferBuilder = new FlatBufferBuilder(BLOCK_DATA_SIZE);
        int baseBlockOffset = com.phraser.schema.phraser.StoreBlock.createStoreBlock(flatBufferBuilder,
                BlockType.KEY_BLOCK.code, 1, 1, PhraserUtils.generateEntropy());
        int keyOffset = flatBufferBuilder.createByteVector(key_256);
        int ivOffset = flatBufferBuilder.createByteVector(iv_128);

        com.phraser.schema.phraser.KeyBlock.startKeyBlock(flatBufferBuilder);
        com.phraser.schema.phraser.KeyBlock.addBlock(flatBufferBuilder, baseBlockOffset);
        com.phraser.schema.phraser.KeyBlock.addKey(flatBufferBuilder, keyOffset);
        com.phraser.schema.phraser.KeyBlock.addIv(flatBufferBuilder, ivOffset);
        int keyBlockOffset = com.phraser.schema.phraser.KeyBlock.endKeyBlock(flatBufferBuilder);

        flatBufferBuilder.finish(keyBlockOffset);
        return com.phraser.schema.phraser.KeyBlock.getRootAsKeyBlock(flatBufferBuilder.dataBuffer());
    }

    // --------------------------------------------------

    static KeyBlock createFirstKeyBlock(byte[] key_256, int version) {
        return ImmutableKeyBlock.builder()
                .key(key_256)
                .version(version)
                .build();
    }
}
