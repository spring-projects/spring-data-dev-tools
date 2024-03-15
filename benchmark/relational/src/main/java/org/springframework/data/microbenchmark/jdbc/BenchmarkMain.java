package org.springframework.data.microbenchmark.jdbc;

import org.openjdk.jmh.infra.Blackhole;
import org.springframework.boot.autoconfigure.SpringBootApplication;

public class BenchmarkMain {
	public static void main(String[] args) throws Exception {
		JdbcBenchmark jdbcBenchmark = new JdbcBenchmark();
		jdbcBenchmark.profile = "postgres";
		jdbcBenchmark.setUp();
		jdbcBenchmark.convertWithSpringData(new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous."));
	}
}
