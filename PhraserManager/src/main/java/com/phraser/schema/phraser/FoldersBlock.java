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
public final class FoldersBlock extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_24_3_25(); }
  public static FoldersBlock getRootAsFoldersBlock(ByteBuffer _bb) { return getRootAsFoldersBlock(_bb, new FoldersBlock()); }
  public static FoldersBlock getRootAsFoldersBlock(ByteBuffer _bb, FoldersBlock obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public FoldersBlock __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public StoreBlock block() { return block(new StoreBlock()); }
  public StoreBlock block(StoreBlock obj) { int o = __offset(4); return o != 0 ? obj.__assign(o + bb_pos, bb) : null; }
  public Folder folders(int j) { return folders(new Folder(), j); }
  public Folder folders(Folder obj, int j) { int o = __offset(6); return o != 0 ? obj.__assign(__indirect(__vector(o) + j * 4), bb) : null; }
  public int foldersLength() { int o = __offset(6); return o != 0 ? __vector_len(o) : 0; }
  public Folder.Vector foldersVector() { return foldersVector(new Folder.Vector()); }
  public Folder.Vector foldersVector(Folder.Vector obj) { int o = __offset(6); return o != 0 ? obj.__assign(__vector(o), 4, bb) : null; }

  public static void startFoldersBlock(FlatBufferBuilder builder) { builder.startTable(2); }
  public static void addBlock(FlatBufferBuilder builder, int blockOffset) { builder.addStruct(0, blockOffset, 0); }
  public static void addFolders(FlatBufferBuilder builder, int foldersOffset) { builder.addOffset(1, foldersOffset, 0); }
  public static int createFoldersVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]); return builder.endVector(); }
  public static void startFoldersVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static int endFoldersBlock(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public FoldersBlock get(int j) { return get(new FoldersBlock(), j); }
    public FoldersBlock get(FoldersBlock obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}

