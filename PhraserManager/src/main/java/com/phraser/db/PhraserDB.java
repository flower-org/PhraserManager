package com.phraser.db;

import java.io.File;

public class PhraserDB {
  static final byte[] DEFAULT_IV =
    new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f };

  //AES-256 key
  static final byte[] DEFAULT_KEY =
    new byte[] { 0x60, 0x3d, (byte)0xeb, 0x10, 0x15, (byte)0xca, 0x71, (byte)0xbe, 0x2b, 0x73, (byte)0xae, (byte)0xf0, (byte)0x85, 0x7d, 0x77, (byte)0x81,
    0x1f, 0x35, 0x2c, 0x07, 0x3b, 0x61, 0x08, (byte)0xd7, 0x2d, (byte)0x98, 0x10, (byte)0xa3, 0x09, 0x14, (byte)0xdf, (byte)0xf4 };

  public static PhraserDB createNewDb(int bucketCount) {
    Block[] blocks = new Block[bucketCount];

    blocks[0] = Block.create(Block.createFirstKeyBlock(DEFAULT_KEY, DEFAULT_IV));

    return new PhraserDB(blocks);
  }

  final Block[] blocks;

  public PhraserDB(Block[] blocks) {
    this.blocks = blocks;
  }

  public Block[] blocks() {
    return blocks;
  }

  public int blockCount() {
    return blocks.length;
  }

  public void loadFromFile(File file) {
    //
  }

  public void saveToFile(File file) {
    //
  }
}
