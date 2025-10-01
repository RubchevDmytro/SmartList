package com.example.mobile_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
@Component
public class PriceUpdaterService {
    private static final Logger logger = LoggerFactory.getLogger(PriceUpdaterService.class);

    @Scheduled(cron = "0 12 */7 * * ?") // каждые 7 дней
    @Profile("!test")
    public void updatePricesFromParser() {
        runPythonScript();
    }

//    @PostConstruct
//    public void runOnStartup() {
  //      logger.info("Running price update on application startup...");
    //    runPythonScript();
    //}

    private void runPythonScript() {
        logger.info("Starting price update from Python parser...");
        try {
            String pythonScriptPath = "/Users/dimarubchev/Desktop/mobile-backend/src/main/java/com/example/mobile_backend/service/haha.py";
            ProcessBuilder processBuilder = new ProcessBuilder(Arrays.asList("python3", pythonScriptPath));

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("Python output: {}", line);
                }
                while ((line = errorReader.readLine()) != null) {
                    logger.error("Python error: {}", line);
                }
            }

            if (exitCode == 0) {
                logger.info("Price update completed successfully (exit code: {})", exitCode);
            } else {
                logger.error("Price update failed with exit code: {}", exitCode);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error executing Python script: {}", e.getMessage(), e);
        }
    }
}

