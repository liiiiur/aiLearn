package com.wangxia.liu.aitest.migrate.embedding;

import java.util.Collections;
import java.util.List;

public interface EmbeddingService {
    default List<Float> embed(String text) {
        List<List<Float>> batch = embedBatch(Collections.singletonList(text));
        if (batch.isEmpty()) {
            throw new IllegalStateException("Embedding result is empty.");
        }
        return batch.get(0);
    }

    List<List<Float>> embedBatch(List<String> texts);
}
