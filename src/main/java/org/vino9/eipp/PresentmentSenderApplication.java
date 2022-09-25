package org.vino9.eipp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {"org.vino9.eipp.data"})
public class PresentmentSenderApplication {

	public static void main(String[] args) {
		SpringApplication.run(PresentmentSenderApplication.class, args);
	}

}
