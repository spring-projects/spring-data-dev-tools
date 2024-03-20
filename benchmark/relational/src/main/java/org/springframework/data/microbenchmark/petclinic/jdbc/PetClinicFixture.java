package org.springframework.data.microbenchmark.petclinic.jdbc;

import lombok.Getter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.microbenchmark.FixtureUtils;

public class PetClinicFixture {

	final ConfigurableApplicationContext context;

	PetClinicFixture(String database) {

		this.context = FixtureUtils.createContext(Application.class, "jdbc", database);


		// disableEntityCallbacks(context);

	}

	@SpringBootApplication
	@EnableJdbcRepositories
	static class Application {
	}
}
