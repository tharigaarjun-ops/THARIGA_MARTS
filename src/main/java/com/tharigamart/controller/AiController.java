package com.tharigamart.controller;

import com.tharigamart.dto.AiChatRequest;
import com.tharigamart.dto.AiReply;
import com.tharigamart.dto.ApiResponse;
import com.tharigamart.exception.BadRequestException;
import com.tharigamart.service.AiAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI shopping assistant (any signed-in role). The OpenAI API key is read
 * server-side from the OPENAI_API_KEY environment variable and is never
 * exposed to the browser.
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiAssistantService aiAssistantService;

    public AiController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    /** Body: { "message": "Do you have gel pens?" } -> { reply, products, aiEnabled }. */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiReply>> chat(@RequestBody AiChatRequest request) {
        String message = request == null ? null : request.getMessage();
        if (message == null || message.isBlank()) {
            throw new BadRequestException("Please enter a message for the assistant");
        }
        AiReply reply = aiAssistantService.chat(message);
        return ResponseEntity.ok(ApiResponse.ok("Assistant reply ready", reply));
    }
}
