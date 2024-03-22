package org.springframework.data.microbenchmark.petclinic.jdbc;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.microbenchmark.common.AbstractMicrobenchmark;

public class PetClinicBenchmark extends AbstractMicrobenchmark {


	private OwnerRepository ownerRepository;
	private PetRepository petRepository;

	@Setup
	public void setup() {

		PetClinicFixture fixture = new PetClinicFixture("h2-in-memory");
		ConfigurableApplicationContext context = fixture.context;

		ownerRepository = context.getBean(OwnerRepository.class);
		petRepository = context.getBean(PetRepository.class);

	}

	@Benchmark
	public void findByName() {

	}

	@Benchmark
	public void findByOwnerId() {

	}

}
