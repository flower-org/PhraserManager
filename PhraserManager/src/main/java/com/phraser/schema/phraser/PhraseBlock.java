// automatically generated by the FlatBuffers compiler, do not modify

package com.phraser.schema.phraser;

import com.google.flatbuffers.BaseVector;
import com.google.flatbuffers.BooleanVector;
import com.google.flatbuffers.ByteVector;
import com.google.flatbuffers.Constants;
import com.google.flatbuffers.DoubleVector;
import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.FloatVector;
import com.google.flatbuffers.IntVector;
import com.google.flatbuffers.LongVector;
import com.google.flatbuffers.ShortVector;
import com.google.flatbuffers.StringVector;
import com.google.flatbuffers.Struct;
import com.google.flatbuffers.Table;
import com.google.flatbuffers.UnionVector;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressWarnings("unused")
public final class PhraseBlock extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_24_3_25(); }
  public static PhraseBlock getRootAsPhraseBlock(ByteBuffer _bb) { return getRootAsPhraseBlock(_bb, new PhraseBlock()); }
  public static PhraseBlock getRootAsPhraseBlock(ByteBuffer _bb, PhraseBlock obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public PhraseBlock __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public StoreBlock block() { return block(new StoreBlock()); }
  public StoreBlock block(StoreBlock obj) { int o = __offset(4); return o != 0 ? obj.__assign(o + bb_pos, bb) : null; }
  public int phraseTemplateId() { int o = __offset(6); return o != 0 ? bb.getShort(o + bb_pos) & 0xFFFF : 0; }
  public boolean mutatePhraseTemplateId(int phrase_template_id) { int o = __offset(6); if (o != 0) { bb.putShort(o + bb_pos, (short) phrase_template_id); return true; } else { return false; } }
  public int folderId() { int o = __offset(8); return o != 0 ? bb.getShort(o + bb_pos) & 0xFFFF : 0; }
  public boolean mutateFolderId(int folder_id) { int o = __offset(8); if (o != 0) { bb.putShort(o + bb_pos, (short) folder_id); return true; } else { return false; } }
  public boolean isTombstone() { int o = __offset(10); return o != 0 ? 0!=bb.get(o + bb_pos) : false; }
  public boolean mutateIsTombstone(boolean is_tombstone) { int o = __offset(10); if (o != 0) { bb.put(o + bb_pos, (byte)(is_tombstone ? 1 : 0)); return true; } else { return false; } }
  public String phraseName() { int o = __offset(12); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer phraseNameAsByteBuffer() { return __vector_as_bytebuffer(12, 1); }
  public ByteBuffer phraseNameInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 12, 1); }
  public PhraseHistory history(int j) { return history(new PhraseHistory(), j); }
  public PhraseHistory history(PhraseHistory obj, int j) { int o = __offset(14); return o != 0 ? obj.__assign(__indirect(__vector(o) + j * 4), bb) : null; }
  public int historyLength() { int o = __offset(14); return o != 0 ? __vector_len(o) : 0; }
  public PhraseHistory.Vector historyVector() { return historyVector(new PhraseHistory.Vector()); }
  public PhraseHistory.Vector historyVector(PhraseHistory.Vector obj) { int o = __offset(14); return o != 0 ? obj.__assign(__vector(o), 4, bb) : null; }

  public static void startPhraseBlock(FlatBufferBuilder builder) { builder.startTable(6); }
  public static void addBlock(FlatBufferBuilder builder, int blockOffset) { builder.addStruct(0, blockOffset, 0); }
  public static void addPhraseTemplateId(FlatBufferBuilder builder, int phraseTemplateId) { builder.addShort(1, (short) phraseTemplateId, (short) 0); }
  public static void addFolderId(FlatBufferBuilder builder, int folderId) { builder.addShort(2, (short) folderId, (short) 0); }
  public static void addIsTombstone(FlatBufferBuilder builder, boolean isTombstone) { builder.addBoolean(3, isTombstone, false); }
  public static void addPhraseName(FlatBufferBuilder builder, int phraseNameOffset) { builder.addOffset(4, phraseNameOffset, 0); }
  public static void addHistory(FlatBufferBuilder builder, int historyOffset) { builder.addOffset(5, historyOffset, 0); }
  public static int createHistoryVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]); return builder.endVector(); }
  public static void startHistoryVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static int endPhraseBlock(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public PhraseBlock get(int j) { return get(new PhraseBlock(), j); }
    public PhraseBlock get(PhraseBlock obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}

