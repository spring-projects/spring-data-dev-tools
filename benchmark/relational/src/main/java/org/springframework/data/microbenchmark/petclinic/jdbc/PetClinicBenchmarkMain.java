package org.springframework.data.microbenchmark.petclinic.jdbc;

public class PetClinicBenchmarkMain {
	public static void main(String[] args) {

		PetClinicBenchmark benchmark = new PetClinicBenchmark();
		benchmark.setup();
		System.out.println("starting loop");
		while (true) {
			benchmark.findByName();
		}
	}
}
