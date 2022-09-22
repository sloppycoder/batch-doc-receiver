package org.vino9.lib.batchdocreceiver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {"org.vino9.lib.batchdocreceiver.data"})
public class BatchDocReceiverApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatchDocReceiverApplication.class, args);
	}

}
