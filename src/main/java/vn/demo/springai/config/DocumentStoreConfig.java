package vn.demo.springai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class DocumentStoreConfig {

    private static final String EMBEDDING_MODEL_BASE =
            "https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main";

    @Bean
    @Lazy
    EmbeddingModel localEmbeddingModel() {
        TransformersEmbeddingModel model = new TransformersEmbeddingModel();
        model.setResourceCacheDirectory(".cache/spring-ai");
        model.setTokenizerResource(EMBEDDING_MODEL_BASE + "/tokenizer.json");
        model.setModelResource(EMBEDDING_MODEL_BASE + "/onnx/model_quint8_avx2.onnx");
        return model;
    }

    @Bean
    @Lazy
    SimpleVectorStore simpleVectorStore(@Qualifier("localEmbeddingModel") EmbeddingModel localEmbeddingModel) {
        return SimpleVectorStore.builder(localEmbeddingModel).build();
    }
}
