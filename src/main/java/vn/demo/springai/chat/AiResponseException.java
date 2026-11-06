package vn.demo.springai.chat;

public class AiResponseException extends RuntimeException {

    public AiResponseException(String message) {
        super(message);
    }
}
