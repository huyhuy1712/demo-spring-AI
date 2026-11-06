package vn.demo.springai.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentIndexingServiceTest {

    @Test
    void rejectsNonPdfFiles() {
        DocumentIndexingService service = newService(mockVectorStoreProvider());
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "not a pdf".getBytes());

        DocumentProcessingException exception = assertThrows(
                DocumentProcessingException.class, () -> service.index(file));

        assertEquals("Chỉ chấp nhận tệp có đuôi .pdf.", exception.getMessage());
    }

    @Test
    void rejectsFilesWithoutPdfHeader() {
        DocumentIndexingService service = newService(mockVectorStoreProvider());
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.pdf", "application/pdf", "not a pdf".getBytes());

        DocumentProcessingException exception = assertThrows(
                DocumentProcessingException.class, () -> service.index(file));

        assertEquals("Tệp tải lên không có định dạng PDF hợp lệ.", exception.getMessage());
    }

    @Test
    void extractsSplitsAndIndexesPdfPages() throws IOException {
        SimpleVectorStore vectorStore = mock(SimpleVectorStore.class);
        DocumentIndexingService service = newService(providerOf(vectorStore));
        MockMultipartFile file = new MockMultipartFile(
                "file", "lecture.pdf", "application/pdf", createPdf());

        IndexedDocument indexed = service.index(file);

        assertEquals("lecture.pdf", indexed.fileName());
        assertEquals(1, indexed.pages());
        assertTrue(indexed.chunks() > 0);
        assertEquals(1, service.list().size());
        verify(vectorStore).add(anyList());
    }

    private static DocumentIndexingService newService(ObjectProvider<SimpleVectorStore> provider) {
        return new DocumentIndexingService(provider);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<SimpleVectorStore> mockVectorStoreProvider() {
        return mock(ObjectProvider.class);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<SimpleVectorStore> providerOf(SimpleVectorStore vectorStore) {
        ObjectProvider<SimpleVectorStore> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(vectorStore);
        return provider;
    }

    private static byte[] createPdf() throws IOException {
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            pdf.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText("Spring AI indexes PDF text and retrieves relevant chunks.");
                content.endText();
            }
            pdf.save(output);
            return output.toByteArray();
        }
    }
}
