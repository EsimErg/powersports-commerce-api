package kz.powersports.commerce.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
                "application", "powersports-commerce-api",
                "status", "UP",
                "timestamp", Instant.now().toString()
        );
    }
}