package com.tharigamart.dto;

import java.util.List;

/** AI shopping assistant response: a text reply plus recommended products. */
public class AiReply {

    private String reply;
    private List<ProductDTO> products;
    /** True when a real OpenAI model answered (OPENAI_API_KEY was set). */
    private boolean aiEnabled;

    public AiReply() {
    }

    public AiReply(String reply, List<ProductDTO> products, boolean aiEnabled) {
        this.reply = reply;
        this.products = products;
        this.aiEnabled = aiEnabled;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public List<ProductDTO> getProducts() {
        return products;
    }

    public void setProducts(List<ProductDTO> products) {
        this.products = products;
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
    }
}
