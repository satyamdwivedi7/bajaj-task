package org.example.bfh.service;

import org.example.bfh.dto.FinalQueryRequest;
import org.example.bfh.dto.GenerateRequest;
import org.example.bfh.dto.GenerateResponse;
import org.example.bfh.util.SqlSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WorkflowService {
    private final Logger log = LoggerFactory.getLogger(WorkflowService.class);
    private final RestTemplate restTemplate;

    @Value("${bfh.generate-url}")
    private String generateUrl;

    @Value("${bfh.submit-url}")
    private String submitUrl;

    @Value("${bfh.name}")
    private String name;

    @Value("${bfh.regNo}")
    private String regNo;

    @Value("${bfh.email}")
    private String email;

    public WorkflowService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void runWorkflowOnStartup() {
        try {
            GenerateRequest req = new GenerateRequest(name, regNo, email);
            log.info("Sending generateWebhook request to {}", generateUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<GenerateRequest> entity = new HttpEntity<>(req, headers);

            ResponseEntity<String> resp = restTemplate.exchange(generateUrl, HttpMethod.POST, entity, String.class);
            if (resp.getStatusCode() != HttpStatus.OK && resp.getStatusCode() != HttpStatus.CREATED) {
                log.error("generateWebhook returned non-200: status={}, body={}", resp.getStatusCode(), resp.getBody());
                return;
            }

            String body = resp.getBody();
            log.info("generateWebhook response body: {}", body);

            GenerateResponse generateResponse = null;
            try {
                generateResponse = restTemplate.getForObject("data:application/json," + body, GenerateResponse.class);
            } catch (Exception e) {
                log.warn("Could not parse response automatically, trying direct deserialization");
            }

            if (generateResponse == null) {
                generateResponse = new GenerateResponse();
                if (body != null && body.contains("webhook")) {
                    String webhookPart = body.substring(body.indexOf("\"webhook\""));
                    if (webhookPart.contains(":")) {
                        String webhookValue = webhookPart.split(":")[1].split("\"")[1];
                        generateResponse.setWebhook(webhookValue);
                    }
                }
                if (body != null && body.contains("accessToken")) {
                    String tokenPart = body.substring(body.indexOf("\"accessToken\""));
                    if (tokenPart.contains(":")) {
                        String tokenValue = tokenPart.split(":")[1].split("\"")[1];
                        generateResponse.setAccessToken(tokenValue);
                    }
                }
            }

            String webhookUrl = generateResponse.getWebhook();
            String accessToken = generateResponse.getAccessToken();

            log.info("Parsed webhookUrl={}, accessTokenPresent={}", webhookUrl, accessToken != null && !accessToken.isBlank());

            int lastTwo = -1;
            try {
                String s = regNo.replaceAll("\\D", "");
                if (s.length() >= 2) {
                    lastTwo = Integer.parseInt(s.substring(s.length() - 2));
                } else {
                    String tail = regNo.length() >= 2 ? regNo.substring(regNo.length()-2) : regNo;
                    lastTwo = tail.chars().sum();
                }
            } catch (Exception e) {
                log.warn("Could not parse regNo numeric suffix: {}", e.getMessage());
            }
            boolean isOdd = (lastTwo % 2) != 0;
            log.info("Determined regNo last two digits = {}, odd={}", lastTwo, isOdd);

            String questionText = null;
            if (generateResponse.getQuestionUrl() != null) {
                String questionUrl = generateResponse.getQuestionUrl();
                try {
                    questionText = restTemplate.getForObject(questionUrl, String.class);
                } catch (Exception ex) {
                    log.warn("Failed to download question from returned questionUrl: {}", ex.getMessage());
                }
            }

            if (questionText == null) {
                log.info("No question text available from response. You must manually open the drive links from the PDF and implement SQL.");
                if (isOdd) {
                    log.info("Question assigned is Question 1 (odd regNo) — use Drive link from the PDF.");
                } else {
                    log.info("Question assigned is Question 2 (even regNo) — use Drive link from the PDF.");
                }
            }

            String finalSql = SqlSolver.solveFromQuestionText(questionText, regNo);
            log.info("Computed final SQL (placeholder):\n{}", finalSql);

            submitFinalQuery(webhookUrl, accessToken, finalSql);

        } catch (Exception ex) {
            log.error("Workflow failed: {}", ex.getMessage(), ex);
        }
    }

    private void submitFinalQuery(String webhookUrl, String accessToken, String finalSql) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.error("No webhookUrl available; cannot submit final query.");
            return;
        }
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("No accessToken provided by generateWebhook. The PDF expects an 'accessToken' to be used as JWT. Proceeding without Authorization header.");
        }

        FinalQueryRequest queryReq = new FinalQueryRequest(finalSql);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (accessToken != null && !accessToken.isBlank()) {
            headers.set("Authorization", "Bearer " + accessToken);
        }

        HttpEntity<FinalQueryRequest> entity = new HttpEntity<>(queryReq, headers);

        try {
            log.info("Submitting finalQuery to {} ...", webhookUrl);
            ResponseEntity<String> resp = restTemplate.exchange(webhookUrl, HttpMethod.POST, entity, String.class);
            log.info("Submission response status: {}, body: {}", resp.getStatusCode(), resp.getBody());
        } catch (Exception e) {
            log.error("Failed to submit final query: {}", e.getMessage(), e);
        }
    }
}
