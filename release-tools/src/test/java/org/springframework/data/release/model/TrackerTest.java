/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.data.release.model;

import static org.assertj.core.api.Assertions.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 */
class TrackerTest {

	@Test
	void testVersionBranch() throws Exception {

		Pattern pattern = Pattern.compile(Tracker.GITHUB.getTicketPattern());
		Matcher matcher = pattern.matcher("1.2.x");
		matcher.find();

		assertThat(matcher.group(1)).isEqualTo("1");
	}
}
