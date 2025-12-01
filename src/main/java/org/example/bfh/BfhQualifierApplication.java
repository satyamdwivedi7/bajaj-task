package org.example.bfh;

import org.example.bfh.service.WorkflowService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BfhQualifierApplication {

    public static void main(String[] args) {
        SpringApplication.run(BfhQualifierApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(WorkflowService workflowService) {
        return args -> {
            workflowService.runWorkflowOnStartup();
        };
    }
}
