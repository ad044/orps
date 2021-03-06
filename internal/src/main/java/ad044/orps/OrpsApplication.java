package ad044.orps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OrpsApplication {
	public static void main(String[] args) {
		SpringApplication.run(OrpsApplication.class, args);
	}
}
