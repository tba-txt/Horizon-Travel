package br.com.horizon.horizon_api.interfaces.rest;

import br.com.horizon.horizon_api.infrastructure.config.DataSeeder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dev")
@Profile("dev")
@RequiredArgsConstructor
public class DevController {

    private final DataSeeder dataSeeder;

    @PostMapping("/reseed")
    public ResponseEntity<String> forceReseed() {
        try {
            dataSeeder.run();
            return ResponseEntity.ok("Database dev re-seeded successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error reseeding: " + e.getMessage());
        }
    }
}
