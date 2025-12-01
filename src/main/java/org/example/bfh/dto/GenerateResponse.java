package org.example.bfh.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenerateResponse {
    private String webhook;
    private String accessToken;
    private String questionUrl;

    public GenerateResponse() {}

    public String getWebhook() { return webhook; }
    public void setWebhook(String webhook) { this.webhook = webhook; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getQuestionUrl() { return questionUrl; }
    public void setQuestionUrl(String questionUrl) { this.questionUrl = questionUrl; }
}
