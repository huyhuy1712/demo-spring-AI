package vn.demo.springai.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatModeTest {

    @Test
    void resolvesKnownModeWithoutCaseSensitivity() {
        assertEquals(ChatMode.EXPLAIN, ChatMode.fromId("EXPLAIN"));
    }

    @Test
    void rejectsUnknownMode() {
        assertThrows(IllegalArgumentException.class, () -> ChatMode.fromId("unknown"));
    }

    @Test
    void defaultsMissingModeToChat() {
        assertEquals("chat", new ChatRequest("Xin chào", null).mode());
    }
}
