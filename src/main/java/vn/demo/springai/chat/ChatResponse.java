package vn.demo.springai.chat;

import java.util.List;

public record ChatResponse(String reply, String mode, String modeLabel, List<String> sources) {
}
