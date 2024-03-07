/*
 * Copyright 2018-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.microbenchmark.common;

import jmh.mbr.junit4.Microbenchmark;

import org.junit.runner.RunWith;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Base class for microbenchmarks providing default JMH settings and allowing execution through JUnit.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @see Microbenchmark
 */
@Warmup(iterations = 10, time = 2)
@Measurement(iterations = 10, time = 2)
@Fork(value = 1, jvmArgs = { "-server", "-XX:+HeapDumpOnOutOfMemoryError", "-Xms1024m", "-Xmx1024m",
		"-XX:MaxDirectMemorySize=1024m", "-noverify" })
@State(Scope.Thread)
@RunWith(Microbenchmark.class)
public abstract class AbstractMicrobenchmark {

}
