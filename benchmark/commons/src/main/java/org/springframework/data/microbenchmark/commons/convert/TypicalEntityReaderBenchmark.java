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

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openjdk.jmh.annotations.Benchmark;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.AccessType.Type;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.CustomConversions.StoreConversions;
import org.springframework.data.convert.EntityInstantiator;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.PropertyValueProvider;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.microbenchmark.common.AbstractMicrobenchmark;
import org.springframework.data.util.TypeInformation;

/**
 * Benchmark for a typical converter that reads entities.
 *
 * @author Mark Paluch
 */
public class TypicalEntityReaderBenchmark extends AbstractMicrobenchmark {

	private final MyMappingContext context = new MyMappingContext();
	private final EntityInstantiators instantiators = new EntityInstantiators();
	private final ConversionService conversionService = DefaultConversionService.getSharedInstance();
	private final CustomConversions customConversions = new CustomConversions(StoreConversions.NONE,
			Collections.emptyList());
	private final ParameterValueProvider<MyPersistentProperty> NONE = new ParameterValueProvider<TypicalEntityReaderBenchmark.MyPersistentProperty>() {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.model.ParameterValueProvider#getParameterValue(org.springframework.data.mapping.PreferredConstructor.Parameter)
		 */
		@Override
		public <T> T getParameterValue(Parameter<T, MyPersistentProperty> parameter) {
			return null;
		}
	};

	private final Map<String, Object> simpleEntityData = new HashMap<>();

	/**
	 * Prepare and pre-initialize to remove initialization overhead from measurement.
	 */
	public TypicalEntityReaderBenchmark() {

		instantiators.getInstantiatorFor(context.getRequiredPersistentEntity(SimpleEntity.class));
		instantiators.getInstantiatorFor(context.getRequiredPersistentEntity(SimpleAccessibleEntityPropertyAccess.class));
		instantiators.getInstantiatorFor(context.getRequiredPersistentEntity(SimpleAccessibleEntityFieldAccess.class));
		instantiators.getInstantiatorFor(context.getRequiredPersistentEntity(SimpleEntityWithConstructor.class));

		instantiators.getInstantiatorFor(context.getRequiredPersistentEntity(MyDataClass.class));
		instantiators.getInstantiatorFor(context.getRequiredPersistentEntity(MyDataClassWithDefaulting.class));

		simpleEntityData.put("firstname", "Walter");
		simpleEntityData.put("lastname", "White");
	}

	@Benchmark
	public Object simpleEntityReflectiveFieldAccess() {
		return read(simpleEntityData, SimpleEntity.class, false);
	}

	@Benchmark
	public Object simpleEntityReflectivePropertyAccess() {
		return read(simpleEntityData, SimpleEntityPropertyAccess.class, false);
	}

	@Benchmark
	public Object simpleEntityReflectivePropertyAccessWithCustomConversionRegistry() {
		return read(simpleEntityData, SimpleEntity.class, true);
	}

	@Benchmark
	public Object simpleEntityGeneratedPropertyAccess() {
		return read(simpleEntityData, SimpleAccessibleEntityPropertyAccess.class, false);
	}

	@Benchmark
	public Object simpleEntityGeneratedFieldAccess() {
		return read(simpleEntityData, SimpleAccessibleEntityFieldAccess.class, false);
	}

	@Benchmark
	public Object simpleEntityGeneratedConstructorArgsCreation() {
		return read(simpleEntityData, SimpleEntityWithConstructor.class, false);
	}

	@Benchmark
	public Object simpleEntityReflectiveConstructorArgsCreation() {
		return read(simpleEntityData, SimpleEntityWithReflectiveConstructor.class, false);
	}

	@Benchmark
	public Object simpleEntityReflectiveConstructorAndProperty() {
		return read(simpleEntityData, SimpleEntityWithConstructorAndProperty.class, false);
	}

	@Benchmark
	public Object simpleEntityReflectiveConstructorAndField() {
		return read(simpleEntityData, SimpleEntityWithConstructorAndField.class, false);
	}

	@Benchmark
	public Object simpleEntityGeneratedConstructorAndProperty() {
		return read(simpleEntityData, SimpleEntityWithGeneratedConstructorAndProperty.class, false);
	}

	@Benchmark
	public Object simpleEntityGeneratedConstructorAndField() {
		return read(simpleEntityData, SimpleEntityWithGeneratedConstructorAndField.class, false);
	}

	@Benchmark
	public Object kotlinDataClass() {
		return read(simpleEntityData, MyDataClass.class, false);
	}

	@Benchmark
	public Object kotlinDataClassWithDefaulting() {
		return read(simpleEntityData, MyDataClassWithDefaulting.class, false);
	}

	@Benchmark
	public boolean hasReadTarget() {
		return customConversions.hasCustomReadTarget(String.class, Object.class);
	}

	static class SimpleEntity {
		String firstname, lastname;
	}

	@Data
	@AccessType(Type.PROPERTY)
	private static class SimpleEntityPropertyAccess {
		String firstname, lastname;
	}

	@Data
	@AccessType(Type.PROPERTY)
	public static class SimpleAccessibleEntityPropertyAccess {
		String firstname, lastname;
	}

	@Data
	public static class SimpleAccessibleEntityFieldAccess {
		public String firstname, lastname;
	}

	@RequiredArgsConstructor
	public static class SimpleEntityWithConstructor {
		final String firstname, lastname;
	}

	@RequiredArgsConstructor
	private static class SimpleEntityWithReflectiveConstructor {
		final String firstname, lastname;
	}

	@Data
	@RequiredArgsConstructor
	private static class SimpleEntityWithConstructorAndField {

		final String firstname;
		String lastname;
	}

	@Data
	@AccessType(Type.PROPERTY)
	@RequiredArgsConstructor
	private static class SimpleEntityWithConstructorAndProperty {

		final String firstname;
		String lastname;
	}

	@Data
	@AccessType(Type.PROPERTY)
	@RequiredArgsConstructor
	public static class SimpleEntityWithGeneratedConstructorAndProperty {

		final String firstname;
		String lastname;
	}

	@Data
	@RequiredArgsConstructor
	public static class SimpleEntityWithGeneratedConstructorAndField {

		final String firstname;
		String lastname;
	}

	/**
	 * Typical code used to read entities in {@link org.springframework.data.convert.EntityReader}.
	 *
	 * @param data
	 * @param classToRead
	 * @param queryCustomConversions {@literal true} to call {@link CustomConversions#hasCustomReadTarget(Class, Class)}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Object read(Map<String, Object> data, Class<?> classToRead, boolean queryCustomConversions) {

		if (queryCustomConversions) {
			customConversions.hasCustomReadTarget(Map.class, classToRead);
		}

		MyPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(classToRead);
		PreferredConstructor<?, MyPersistentProperty> constructor = persistentEntity.getPersistenceConstructor();

		ParameterValueProvider<MyPersistentProperty> provider = constructor.isNoArgConstructor() //
				? NONE //
				: new ParameterValueProvider<MyPersistentProperty>() {

					@Override
					public <T> T getParameterValue(Parameter<T, MyPersistentProperty> parameter) {
						return (T) getValue(data, parameter.getName(), parameter.getType().getType(), queryCustomConversions);
					}
				};

		EntityInstantiator instantiator = instantiators.getInstantiatorFor(persistentEntity);
		Object instance = instantiator.createInstance(persistentEntity, provider);

		if (!persistentEntity.requiresPropertyPopulation()) {
			return instance;
		}

		PropertyValueProvider<MyPersistentProperty> valueProvider = new PropertyValueProvider<MyPersistentProperty>() {

			@Override
			public <T> T getPropertyValue(MyPersistentProperty property) {
				return (T) getValue(data, property.getName(), property.getType(), queryCustomConversions);
			}
		};

		PersistentPropertyAccessor<?> accessor = new ConvertingPropertyAccessor<>(
				persistentEntity.getPropertyAccessor(instance), conversionService);

		readProperties(data, persistentEntity, valueProvider, accessor);

		return accessor.getBean();
	}

	private void readProperties(Map<String, Object> data, MyPersistentEntity<?> persistentEntity,
			PropertyValueProvider<MyPersistentProperty> valueProvider, PersistentPropertyAccessor<?> accessor) {

		for (MyPersistentProperty prop : persistentEntity) {

			if (prop.isAssociation() && !persistentEntity.isConstructorArgument(prop)) {
				continue;
			}

			// We skip the id property since it was already set

			if (persistentEntity.isIdProperty(prop)) {
				continue;
			}

			if (persistentEntity.isConstructorArgument(prop) || !data.containsKey(persistentEntity.getName())) {
				continue;
			}

			accessor.setProperty(prop, valueProvider.getPropertyValue(prop));
		}
	}

	private Object getValue(Map<String, Object> data, String name, Class<?> type, boolean queryCustomConversions) {

		Object value = data.get(name);

		if (queryCustomConversions && value != null) {
			customConversions.hasCustomReadTarget(value.getClass(), type);
		}

		return value;
	}

	/**
	 * Minimal {@link MappingContext}.
	 */
	static class MyMappingContext extends AbstractMappingContext<MyPersistentEntity<?>, MyPersistentProperty> {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentEntity(org.springframework.data.util.TypeInformation)
		 */
		@Override
		protected <T> MyPersistentEntity<?> createPersistentEntity(TypeInformation<T> typeInformation) {
			return new MyPersistentEntity<T>(typeInformation);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentProperty(org.springframework.data.mapping.model.Property, org.springframework.data.mapping.model.MutablePersistentEntity, org.springframework.data.mapping.model.SimpleTypeHolder)
		 */
		@Override
		protected MyPersistentProperty createPersistentProperty(Property property, MyPersistentEntity<?> owner,
				SimpleTypeHolder simpleTypeHolder) {
			return new MyPersistentProperty(property, owner, simpleTypeHolder);
		}
	}

	/**
	 * Minimal {@link PersistentProperty}.
	 */
	static class MyPersistentProperty extends AnnotationBasedPersistentProperty<MyPersistentProperty> {

		MyPersistentProperty(Property property, PersistentEntity<?, MyPersistentProperty> owner,
				SimpleTypeHolder simpleTypeHolder) {
			super(property, owner, simpleTypeHolder);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.model.AbstractPersistentProperty#createAssociation()
		 */
		@Override
		protected Association<MyPersistentProperty> createAssociation() {
			return null;
		}
	}

	/**
	 * Minimal {@link PersistentEntity}.
	 *
	 * @param <T>
	 */
	static class MyPersistentEntity<T> extends BasicPersistentEntity<T, MyPersistentProperty> {

		MyPersistentEntity(TypeInformation<T> information) {
			super(information);
		}
	}
}
