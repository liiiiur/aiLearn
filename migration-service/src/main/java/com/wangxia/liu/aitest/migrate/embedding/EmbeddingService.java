package com.wangxia.liu.aitest.migrate.embedding;

import java.util.Collections;
import java.util.List;

/**
 * 向量化服务接口。
 */
public interface EmbeddingService {
    /**
     * 将单条文本转为向量。
     *
     * @param text 文本内容
     * @return 向量结果
     */
    default List<Float> embed(String text) {
        List<List<Float>> batch = embedBatch(Collections.singletonList(text));
        if (batch.isEmpty()) {
            throw new IllegalStateException("向量化结果为空。");
        }
        return batch.get(0);
    }

    /**
     * 批量将文本转为向量。
     *
     * @param texts 文本列表
     * @return 向量列表
     */
    List<List<Float>> embedBatch(List<String> texts);
}
