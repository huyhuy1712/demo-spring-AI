package vn.demo.springai.document;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DocumentIndexingService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final int MAX_DOCUMENTS = 10;

    private final ObjectProvider<SimpleVectorStore> vectorStoreProvider;
    private final TokenTextSplitter splitter = TokenTextSplitter.builder().build();
    private final Map<String, IndexedDocument> indexedDocuments = new ConcurrentHashMap<>();
    private final Map<String, List<String>> chunkIdsByDocument = new ConcurrentHashMap<>();

    public DocumentIndexingService(ObjectProvider<SimpleVectorStore> vectorStoreProvider) {
        this.vectorStoreProvider = vectorStoreProvider;
    }

    public synchronized IndexedDocument index(MultipartFile upload) {
        String fileName = safeFileName(upload.getOriginalFilename());
        if (!StringUtils.hasText(fileName) || !fileName.toLowerCase().endsWith(".pdf")) {
            throw new DocumentProcessingException("Chỉ chấp nhận tệp có đuôi .pdf.");
        }
        if (upload.isEmpty() || upload.getSize() > MAX_FILE_SIZE) {
            throw new DocumentProcessingException("PDF phải có nội dung và dung lượng tối đa 10 MB.");
        }
        if (indexedDocuments.size() >= MAX_DOCUMENTS) {
            throw new DocumentProcessingException("Demo chỉ cho phép lập chỉ mục tối đa 10 PDF cùng lúc.");
        }

        byte[] bytes = readBytes(upload);
        if (!hasPdfHeader(bytes)) {
            throw new DocumentProcessingException("Tệp tải lên không có định dạng PDF hợp lệ.");
        }

        String documentId = UUID.randomUUID().toString();
        List<Document> pages;
        try {
            PagePdfDocumentReader reader = new PagePdfDocumentReader(
                    new ByteArrayResource(bytes),
                    PdfDocumentReaderConfig.builder().withPagesPerDocument(1).build());
            pages = reader.read();
        }
        catch (RuntimeException exception) {
            throw new DocumentProcessingException("Không đọc được PDF. Hãy kiểm tra tệp có bị lỗi hoặc được bảo vệ không.", exception);
        }

        List<Document> sourcePages = new ArrayList<>();
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            Document page = pages.get(pageIndex);
            if (StringUtils.hasText(page.getText())) {
                sourcePages.add(withSourceMetadata(page, documentId, fileName, pageIndex + 1));
            }
        }
        if (sourcePages.isEmpty()) {
            throw new DocumentProcessingException("PDF không có văn bản để lập chỉ mục. PDF dạng ảnh cần OCR.");
        }

        List<Document> chunks = splitter.apply(sourcePages);
        if (chunks.isEmpty()) {
            throw new DocumentProcessingException("Không tách được nội dung PDF thành các đoạn văn bản.");
        }

        List<String> chunkIds = chunks.stream().map(Document::getId).toList();
        SimpleVectorStore vectorStore = null;
        try {
            vectorStore = vectorStoreProvider.getObject();
            vectorStore.add(chunks);
        }
        catch (RuntimeException exception) {
            if (vectorStore != null) {
                try {
                    vectorStore.delete(chunkIds);
                }
                catch (RuntimeException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            throw new DocumentProcessingException("Không thể tạo embedding cho PDF. Kiểm tra cấu hình hoặc thử lại.", exception);
        }

        IndexedDocument indexedDocument = new IndexedDocument(documentId, fileName, sourcePages.size(), chunks.size());
        indexedDocuments.put(documentId, indexedDocument);
        chunkIdsByDocument.put(documentId, chunkIds);
        return indexedDocument;
    }

    public List<IndexedDocument> list() {
        return indexedDocuments.values().stream()
                .sorted((first, second) -> first.fileName().compareToIgnoreCase(second.fileName()))
                .toList();
    }

    public synchronized void delete(String documentId) {
        List<String> chunkIds = chunkIdsByDocument.remove(documentId);
        if (chunkIds == null) {
            throw new DocumentProcessingException("Không tìm thấy tài liệu cần xóa.");
        }
        vectorStoreProvider.getObject().delete(chunkIds);
        indexedDocuments.remove(documentId);
    }

    public List<Document> search(String query) {
        if (indexedDocuments.isEmpty()) {
            return List.of();
        }
        return vectorStoreProvider.getObject().similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(4)
                .similarityThreshold(0.25)
                .build());
    }

    private static Document withSourceMetadata(Document page, String documentId, String fileName, int pageNumber) {
        Map<String, Object> metadata = new HashMap<>(page.getMetadata());
        metadata.values().removeIf(Objects::isNull);
        metadata.put("documentId", documentId);
        metadata.put("fileName", fileName);
        metadata.put("page_number", pageNumber);
        return new Document(page.getText(), metadata);
    }

    private static byte[] readBytes(MultipartFile upload) {
        try {
            return upload.getBytes();
        }
        catch (IOException exception) {
            throw new DocumentProcessingException("Không thể đọc tệp được tải lên.", exception);
        }
    }

    private static String safeFileName(String originalFileName) {
        if (originalFileName == null) {
            return "";
        }
        String normalized = originalFileName.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
    }

    private static boolean hasPdfHeader(byte[] bytes) {
        int headerLength = Math.min(bytes.length, 1024);
        String header = new String(bytes, 0, headerLength, StandardCharsets.ISO_8859_1);
        return header.contains("%PDF-");
    }
}
