package br.com.horizon.horizon_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class HorizonApiApplication {

	public static void main(String[] args) {
		loadEnvFile();
		SpringApplication.run(HorizonApiApplication.class, args);
	}

	private static void loadEnvFile() {
		File envFile = new File(".env");
		if (envFile.exists()) {
			try {
				List<String> lines = Files.readAllLines(Paths.get(".env"));
				for (String line : lines) {
					line = line.trim();
					if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
						continue;
					}
					int eqIdx = line.indexOf('=');
					String key = line.substring(0, eqIdx).trim();
					String value = line.substring(eqIdx + 1).trim();
					if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
						value = value.substring(1, value.length() - 1);
					}
					if (System.getProperty(key) == null && System.getenv(key) == null) {
						System.setProperty(key, value);
					}
				}
			} catch (Exception ignored) {
			}
		}
	}

}

