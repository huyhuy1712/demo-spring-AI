# Spring AI Chat Demo

Ứng dụng chat nhỏ dùng Spring AI `ChatClient`, với giao diện tại <http://localhost:8080>. Ứng dụng kết nối Groq qua API tương thích OpenAI và model `openai/gpt-oss-120b`.

Có thể tải PDF lên ngay trong khung chat. Backend đọc PDF, tách từng trang thành các đoạn văn bản, tạo embedding đa ngôn ngữ cục bộ bằng Spring AI Transformers, lưu vào `SimpleVectorStore` trong RAM rồi truy xuất các đoạn liên quan để đưa vào prompt (RAG). Tài liệu và vector chỉ tồn tại đến khi tắt ứng dụng; sau khi nhấn F5 cần nạp lại PDF. PDF scan ảnh không có text cần OCR, chưa được hỗ trợ.

## Yêu cầu

- JDK 21
- Extension **Extension Pack for Java** trong VS Code
- Groq API key

## Cấu hình API key và cách chạy

1. Tạo hoặc thu hồi key cũ và tạo key mới tại <https://console.groq.com/keys>.
2. Sao chép `.env.example` thành `.env` ở thư mục gốc project.
3. Mở `.env`, thay `AI_API_KEY` bằng Groq API key của bạn.
4. Mở thư mục gốc project trong VS Code, chọn cấu hình **Spring AI Chat Demo** trong **Run and Debug**, nhấn **F5**.
5. Mở <http://localhost:8080>.

`.vscode/launch.json` chạy thẳng lớp Spring Boot; Spring đọc cấu hình từ `.env`. Không cần gõ lệnh Maven. Lần đầu mở project, VS Code có thể cần tải dependency Maven. Không commit hoặc gửi file `.env` cho người khác.

Lần đầu tải PDF, ứng dụng tải model embedding ONNX đa ngôn ngữ (khoảng 130 MB) vào `.cache/spring-ai`; thao tác đầu có thể chậm và cần Internet. Các lần sau sử dụng model đã cache. File PDF và nội dung của chúng không được lưu vào ổ đĩa hoặc gửi tới Groq để tạo embedding; các đoạn liên quan được gửi tới Groq cùng câu hỏi khi chat.

## Luồng nạp PDF

1. Chọn nút **PDF** trong khung chat; demo nhận PDF tối đa 10 MB và tối đa 10 tài liệu trong bộ nhớ.
2. `PagePdfDocumentReader` đọc nội dung theo trang, `TokenTextSplitter` chia nhỏ văn bản và gắn metadata tên file/trang.
3. Spring AI Transformers tạo embedding cục bộ; `SimpleVectorStore` lưu vector tạm trong RAM.
4. Khi chat, backend truy xuất tối đa 4 đoạn liên quan và gửi chúng cùng câu hỏi tới Groq. Câu trả lời hiển thị nguồn PDF/trang nếu có.

Các endpoint tài liệu: `POST /api/documents` (multipart field `file`), `GET /api/documents`, `DELETE /api/documents/{id}`. Tắt/restart ứng dụng sẽ xóa tài liệu đã nạp khỏi vector store trong RAM. PDF scan ảnh cần OCR, hiện chưa hỗ trợ.

## Thử API

```powershell
$body = @{ message = "Giải thích Spring AI ChatClient là gì"; mode = "explain" } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8080/api/chat -Method Post -ContentType "application/json" -Body $body
```

`mode` có thể là `chat`, `explain`, `summarize`, `translate` hoặc `ideas`.