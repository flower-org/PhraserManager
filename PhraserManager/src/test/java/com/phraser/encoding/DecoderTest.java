package com.phraser.encoding;

import com.phraser.HexTool;
import com.phraser.db.KeyBlock;
import com.phraser.dbcodec.FlatBufBlockDecoder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DecoderTest {
    @Test
    public void testDecodeKeyBlock() {
        String aesKeyHex = "5EB0D541F969E8B4FF8CB2A1C2D80DB3633EF02F739364B9CD3ACEE8912D2773";
        String aesIvHex = "E5333ABC68FA970BF4F9102307E4712D";
        String dbNameHex = "546F6B656E47656E657261746564";
        long entropy = 2014619548L;
        String flatBufHex = "0400000094FFFFFF8000000040000000280000001000000001000000010000009CA714780E000000546F6B656E47656E657261746564000010000000E5333ABC68FA970BF4F9102307E4712D200000005EB0D541F969E8B4FF8CB2A1C2D80DB3633EF02F739364B9CD3ACEE8912D27730E0020001400080010000C0004000000";

        byte[] flatBufData = HexTool.hexStringToByteArray(flatBufHex);
        byte[] aesKey = HexTool.hexStringToByteArray(aesKeyHex);
        byte[] aesIv = HexTool.hexStringToByteArray(aesIvHex);
        String dbName = new String(HexTool.hexStringToByteArray(dbNameHex));

        KeyBlock keyBlock = FlatBufBlockDecoder.fromFlatBufKeyBlock(flatBufData);

        assertEquals(1, keyBlock.blockId());
        assertEquals(1, keyBlock.version());
        assertEquals(entropy, keyBlock.entropy());

        assertEquals(128, keyBlock.blockCount());
        assertArrayEquals(aesKey, keyBlock.key());
        assertEquals(dbName, keyBlock.dbName());
        assertArrayEquals(aesIv, keyBlock.iv());
    }

    @Test
    public void testDecodeKeyBlock2() {
        String aesKeyHex = "BD5FE0E5CC692E43423727ABDADB55256E3137EA59B21252F2BA3041AC9E0E1A";
        String aesIvHex = "24B566414C15D0289D1BD32AAD83576D";
        String dbNameHex = "546F6B656E47656E657261746564";
        long entropy = 3515184706L;
        String flatBufHex = "0400000094FFFFFF800000004000000028000000100000000100000001000000427685D10E000000546F6B656E47656E65726174656400001000000024B566414C15D0289D1BD32AAD83576D20000000BD5FE0E5CC692E43423727ABDADB55256E3137EA59B21252F2BA3041AC9E0E1A0E0020001400080010000C0004000000";

        byte[] flatBufData = HexTool.hexStringToByteArray(flatBufHex);
        byte[] aesKey = HexTool.hexStringToByteArray(aesKeyHex);
        byte[] aesIv = HexTool.hexStringToByteArray(aesIvHex);
        String dbName = new String(HexTool.hexStringToByteArray(dbNameHex));

        KeyBlock keyBlock = FlatBufBlockDecoder.fromFlatBufKeyBlock(flatBufData);

        assertEquals(1, keyBlock.blockId());
        assertEquals(1, keyBlock.version());
        assertEquals(entropy, keyBlock.entropy());

        assertEquals(128, keyBlock.blockCount());
        assertArrayEquals(aesKey, keyBlock.key());
        assertEquals(dbName, keyBlock.dbName());
        assertArrayEquals(aesIv, keyBlock.iv());
    }
}
