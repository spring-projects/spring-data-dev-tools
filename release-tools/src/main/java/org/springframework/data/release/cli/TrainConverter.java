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

import org.springframework.data.release.model.ReleaseTrains;
import org.springframework.data.release.model.Train;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Gierke
 */
@Component
public class TrainConverter implements Converter<Train> {

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.shell.core.Converter#supports(java.lang.Class, java.lang.String)
	 */
	@Override
	public boolean supports(Class<?> type, String optionContext) {
		return Train.class.isAssignableFrom(type);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.shell.core.Converter#convertFromText(java.lang.String, java.lang.Class, java.lang.String)
	 */
	@Override
	public Train convertFromText(String value, Class<?> targetType, String optionContext) {
		return StringUtils.hasText(value) ? ReleaseTrains.getTrainByName(value) : null;
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
