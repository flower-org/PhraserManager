package com.phraser;

import com.google.flatbuffers.FlatBufferBuilder;

/**
 * Those methods are added for what would be `root_type` structs.
 * Since, as it often happens in large corporations, FlatBuffer design wasn't immune from pseudo-philosophical influences,
 * somebody did introduced flawed concepts into its design - namely, that there can only be one `root_type`.
 * This implementation detail as such is sheer idiotism made flesh, but violating it causes abnormalities in code generation.
 * The workaround is to have no root types in the Schema and handle logic related to root_types manually.
 * It might not be much in java generation, as we can see below, but it matters to much greater extent in the code generated for C++.
 * Needless to say, we'd like to keep a single FlatBuffer schema for all languages.
 */
public interface RootTypeFinishingMethods {
  static void finishSymbolSetsBlockBuffer(FlatBufferBuilder builder, int offset) { builder.finish(offset); }
  static void finishSizePrefixedSymbolSetsBlockBuffer(FlatBufferBuilder builder, int offset) { builder.finishSizePrefixed(offset); }

  static void finishPhraseTemplatesBlockBuffer(FlatBufferBuilder builder, int offset) { builder.finish(offset); }
  static void finishSizePrefixedPhraseTemplatesBlockBuffer(FlatBufferBuilder builder, int offset) { builder.finishSizePrefixed(offset); }

  static void finishPhraseBlockBuffer(FlatBufferBuilder builder, int offset) { builder.finish(offset); }
  static void finishSizePrefixedPhraseBlockBuffer(FlatBufferBuilder builder, int offset) { builder.finishSizePrefixed(offset); }

  static void finishKeyBlockBuffer(FlatBufferBuilder builder, int offset) { builder.finish(offset); }
  static void finishSizePrefixedKeyBlockBuffer(FlatBufferBuilder builder, int offset) { builder.finishSizePrefixed(offset); }

  static void finishFoldersBlockBuffer(FlatBufferBuilder builder, int offset) { builder.finish(offset); }
  static void finishSizePrefixedFoldersBlockBuffer(FlatBufferBuilder builder, int offset) { builder.finishSizePrefixed(offset); }
}
