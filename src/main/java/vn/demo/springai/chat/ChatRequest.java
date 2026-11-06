package vn.demo.springai.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "Vui lòng nhập nội dung.")
        @Size(max = 4000, message = "Nội dung tối đa 4000 ký tự.")
        String message,
        String mode
) {
    public ChatRequest {
        if (mode == null || mode.isBlank()) {
            mode = "chat";
        }
    }
}
