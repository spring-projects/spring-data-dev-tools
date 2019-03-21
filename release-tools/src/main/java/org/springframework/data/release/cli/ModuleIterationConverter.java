/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.release.cli;

import java.util.List;

import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.Module;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Project;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.Train.Iterations;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.model.Version;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Mark Paluch
 */
@Component
public class ModuleIterationConverter implements Converter<ModuleIteration> {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.shell.core.Converter#supports(java.lang.Class, java.lang.String)
	 */
	@Override
	public boolean supports(Class<?> type, String optionContext) {
		return ModuleIteration.class.isAssignableFrom(type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.shell.core.Converter#convertFromText(java.lang.String, java.lang.Class, java.lang.String)
	 */
	@Override
	public ModuleIteration convertFromText(String value, Class<?> targetType, String optionContext) {

		// Elasticsearch 1.0 Moore SR1

		if (StringUtils.hasText(value)) {

			String[] parts = value.split(" ");

			if (parts.length < 4) {
				throw new IllegalArgumentException(String.format("Cannot resolve ModuleIteration from '%s'", value));
			}

			Project project = Projects.requiredByName(parts[0].trim());
			Version version = Version.parse(parts[1].trim());
			Iteration iteration = Iterations.DEFAULT.getIterationByName(parts[3].trim());

			Module module = Module.create(project, version);
			Train train = new Train(parts[2].trim(), module).withDetached(true);

			return new ModuleIteration(module, new TrainIteration(train, iteration));
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.shell.core.Converter#getAllPossibleValues(java.util.List, java.lang.Class, java.lang.String, java.lang.String, org.springframework.shell.core.MethodTarget)
	 */
	@Override
	public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData,
			String optionContext, MethodTarget target) {
		return false;
	}
}
