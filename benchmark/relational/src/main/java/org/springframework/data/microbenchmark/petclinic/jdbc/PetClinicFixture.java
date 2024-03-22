package org.springframework.data.microbenchmark.petclinic.jdbc;

import org.h2.jdbcx.JdbcDataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcTransactionManagerAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.microbenchmark.FixtureUtils;

import javax.sql.DataSource;

public class PetClinicFixture {

	final ConfigurableApplicationContext context;

	PetClinicFixture(String database) {

		this.context = FixtureUtils.createContext(JdbcApplication.class, "jdbc", database);


		// disableEntityCallbacks(context);

	}

	@SpringBootApplication(
			exclude = {
					R2dbcAutoConfiguration.class,
					R2dbcDataAutoConfiguration.class,
					R2dbcRepositoriesAutoConfiguration.class,
					R2dbcTransactionManagerAutoConfiguration.class,
					HibernateJpaAutoConfiguration.class
			}
	)
	static class JdbcApplication {

		@Bean
		@Profile({"h2","h2-in-memory"})
		@ConfigurationProperties(prefix = "spring.datasource")
		DataSource dataSourceH2() {
			return new JdbcDataSource();
		}

		@Bean
		@Profile({"postgres"})
		@ConfigurationProperties(prefix = "spring.datasource")
		DataSource dataSourcePostgres() {
			PGSimpleDataSource dataSource = new PGSimpleDataSource();
			return dataSource;
		}

	}
}
