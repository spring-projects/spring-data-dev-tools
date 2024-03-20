package org.springframework.data.microbenchmark.petclinic.jdbc;

import org.springframework.data.annotation.Id;

public record PetType(@Id Integer id, String name) {
}

