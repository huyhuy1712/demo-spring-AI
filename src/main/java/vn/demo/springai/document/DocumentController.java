package vn.demo.springai.document;

import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentIndexingService documentIndexingService;

    public DocumentController(DocumentIndexingService documentIndexingService) {
        this.documentIndexingService = documentIndexingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IndexedDocument upload(@RequestParam("file") @NotNull MultipartFile file) {
        return documentIndexingService.index(file);
    }

    @GetMapping
    public List<IndexedDocument> list() {
        return documentIndexingService.list();
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String documentId) {
        documentIndexingService.delete(documentId);
    }
}
