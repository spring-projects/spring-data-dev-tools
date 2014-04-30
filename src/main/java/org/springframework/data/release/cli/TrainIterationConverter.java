/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.release.cli;

import java.util.List;

import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;
import org.springframework.stereotype.Component;

/**
 * @author Oliver Gierke
 */
@Component
public class TrainIterationConverter implements Converter<TrainIteration> {

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.shell.core.Converter#supports(java.lang.Class, java.lang.String)
	 */
	@Override
	public boolean supports(Class<?> type, String optionContext) {
		return TrainIteration.class.equals(type);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.shell.core.Converter#convertFromText(java.lang.String, java.lang.Class, java.lang.String)
	 */
	@Override
	public TrainIteration convertFromText(String value, Class<?> targetType, String optionContext) {

		String[] parts = value.split(" ");
		Train train = ReleaseTrains.getTrainByName(parts[0].trim());
		Iteration iteration = train.getIteration(parts[1].trim());

		return new TrainIteration(train, iteration);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.shell.core.Converter#getAllPossibleValues(java.util.List, java.lang.Class, java.lang.String, java.lang.String, org.springframework.shell.core.MethodTarget)
	 */
	@Override
	public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData,
			String optionContext, MethodTarget target) {

		for (Train train : ReleaseTrains.TRAINS) {

			for (Iteration iteration : train.getIterations()) {
				completions.add(new Completion(new TrainIteration(train, iteration).toString()));
			}
		}

		return false;
	}
}
