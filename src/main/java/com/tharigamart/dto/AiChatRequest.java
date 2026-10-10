package com.tharigamart.dto;

/** Request body for the AI shopping assistant: { "message": "..." }. */
public class AiChatRequest {

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
