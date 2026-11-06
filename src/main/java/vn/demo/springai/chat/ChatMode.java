package vn.demo.springai.chat;

import java.util.Arrays;

public enum ChatMode {
    CHAT("chat", "Trò chuyện", "Trả lời câu hỏi hữu ích, ngắn gọn và dễ hiểu bằng tiếng Việt."),
    EXPLAIN("explain", "Giải thích", "Giải thích nội dung sau bằng tiếng Việt cho người mới bắt đầu, kèm ví dụ ngắn nếu phù hợp."),
    SUMMARIZE("summarize", "Tóm tắt", "Tóm tắt nội dung sau bằng tiếng Việt thành các ý chính, không thêm thông tin không có trong bản gốc."),
    TRANSLATE("translate", "Dịch tiếng Anh", "Dịch nội dung sau sang tiếng Anh tự nhiên. Chỉ đưa ra bản dịch, không giải thích thêm."),
    IDEAS("ideas", "Gợi ý ý tưởng", "Đưa ra 5 ý tưởng thực tế, khác nhau và ngắn gọn dựa trên yêu cầu sau.");

    private final String id;
    private final String label;
    private final String instruction;

    ChatMode(String id, String label, String instruction) {
        this.id = id;
        this.label = label;
        this.instruction = instruction;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public String instruction() {
        return instruction;
    }

    public static ChatMode fromId(String id) {
        return Arrays.stream(values())
                .filter(mode -> mode.id.equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Chức năng AI không hợp lệ."));
    }
}
