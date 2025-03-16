package com.phraser.dbcodec;

import com.phraser.forms.DefaultDBCreator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FlatBufDecoderTest {
    @Test
    public void testFromFlatBufKeyBlock() {
        com.phraser.db.KeyBlock sampleKeyBlock = DefaultDBCreator.getKeyBlock("DB name").keyBlock();

        // Create a sample KeyBlock using FlatBuffers
        byte[] serializedData = FlatBufBlockEncoder.toFlatBufKeyBlock(sampleKeyBlock);

        // Decode the KeyBlock
        com.phraser.db.KeyBlock decodedKeyBlock = FlatBufBlockDecoder.fromFlatBufKeyBlock(serializedData);

        // Assertions to verify the decoded values
        assertNotNull(decodedKeyBlock);
        assertEquals(sampleKeyBlock, decodedKeyBlock);
    }

    @Test
    public void testFromFlatBufSymbolSetsBlock() {
        com.phraser.db.SymbolSetsBlock symbolSetsBlock = DefaultDBCreator.getSymbolSetsBlock().symbolSetsBlock();

        // Create a sample KeyBlock using FlatBuffers
        byte[] serializedData = FlatBufBlockEncoder.toFlatBufSymbolSetsBlock(symbolSetsBlock);

        // Decode the SymbolSetsBlock
        com.phraser.db.SymbolSetsBlock decodedSymbolSetsBlock =
                FlatBufBlockDecoder.fromFlatBufSymbolSetsBlock(serializedData);

        // Assertions to verify the decoded values
        assertNotNull(decodedSymbolSetsBlock);
        assertEquals(symbolSetsBlock, decodedSymbolSetsBlock);
    }

    @Test
    public void testFromFlatBufFoldersBlock() {
        com.phraser.db.FoldersBlock foldersBlock = DefaultDBCreator.getFoldersBlock().foldersBlock();

        // Create a sample KeyBlock using FlatBuffers
        byte[] serializedData = FlatBufBlockEncoder.toFlatBufFoldersBlock(foldersBlock);

        // Decode the FoldersBlock
        com.phraser.db.FoldersBlock decodedSymbolSetsBlock =
                FlatBufBlockDecoder.fromFlatBufFoldersBlock(serializedData);

        // Assertions to verify the decoded values
        assertNotNull(decodedSymbolSetsBlock);
        assertEquals(foldersBlock, decodedSymbolSetsBlock);
    }

    @Test
    public void testFromFlatBufPhraseTemplatesBlock() {
        com.phraser.db.PhraseTemplatesBlock phraseTemplatesBlock = DefaultDBCreator.getPhraseTemplatesBlock().phraseTemplatesBlock();

        // Create a sample KeyBlock using FlatBuffers
        byte[] serializedData = FlatBufBlockEncoder.toFlatBufPhraseTemplatesBlock(phraseTemplatesBlock);

        // Decode the FoldersBlock
        com.phraser.db.PhraseTemplatesBlock decodedPhraseTemplatesBlock =
                FlatBufBlockDecoder.fromFlatBufPhraseTemplatesBlock(serializedData);

        // Assertions to verify the decoded values
        assertNotNull(decodedPhraseTemplatesBlock);
        assertEquals(phraseTemplatesBlock, decodedPhraseTemplatesBlock);
    }

    @Test
    public void testFromFlatBufPhraseBlock() {
        com.phraser.db.PhraseBlock phraseBlock = DefaultDBCreator.getPhraseBlock().phraseBlock();

        // Create a sample KeyBlock using FlatBuffers
        byte[] serializedData = FlatBufBlockEncoder.toFlatBufPhraseBlock(phraseBlock);

        // Decode the FoldersBlock
        com.phraser.db.PhraseBlock decodedPhraseBlock =
                FlatBufBlockDecoder.fromFlatBufPhraseBlock(serializedData);

        // Assertions to verify the decoded values
        assertNotNull(decodedPhraseBlock);
        assertEquals(phraseBlock, decodedPhraseBlock);
    }
}
