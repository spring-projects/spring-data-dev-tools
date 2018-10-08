/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.microbenchmark.commons.convert;

import java.util.Collections;
import java.util.Map;

import org.openjdk.jmh.annotations.Benchmark;
import org.springframework.data.convert.DefaultTypeMapper;
import org.springframework.data.convert.TypeAliasAccessor;
import org.springframework.data.mapping.Alias;
import org.springframework.data.microbenchmark.common.AbstractMicrobenchmark;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * Benchmark for {@link DefaultTypeMapper}.
 * 
 * @author Mark Paluch
 */
public class DefaultTypeMapperBenchmark extends AbstractMicrobenchmark {

	private static final DefaultTypeMapper<Map<String, Object>> TYPE_MAPPER = new DefaultTypeMapper<>(
			StringTypeAliasAccessor.INSTANCE);

	private static final Map<String, Object> TYPED = Collections.singletonMap("_class", MySubType.class.getName());
	private static final Map<String, Object> UNTYPED = Collections.emptyMap();
	private static final TypeInformation<MyType> TYPE_INFORMATION = ClassTypeInformation.from(MyType.class);

	@Benchmark
	public Object readTyped() {
		return TYPE_MAPPER.readType(TYPED);
	}

	@Benchmark
	public Object readTypedWithBaseType() {
		return TYPE_MAPPER.readType(TYPED, TYPE_INFORMATION);
	}

	@Benchmark
	public Object readUntyped() {
		return TYPE_MAPPER.readType(UNTYPED);
	}

	@Benchmark
	public Object readUntypedWithBaseType() {
		return TYPE_MAPPER.readType(UNTYPED, TYPE_INFORMATION);
	}

	static class MyType {}

	static class MySubType extends MyType {}

	enum StringTypeAliasAccessor implements TypeAliasAccessor<Map<String, Object>> {

		INSTANCE;

		@Override
		public Alias readAliasFrom(Map<String, Object> source) {
			return Alias.ofNullable(source.get("_class"));
		}

		@Override
		public void writeTypeTo(Map<String, Object> sink, Object alias) {
			sink.put("_class", alias);
		}
	}
}
