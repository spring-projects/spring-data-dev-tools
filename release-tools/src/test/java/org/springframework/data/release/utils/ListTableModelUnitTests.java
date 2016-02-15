/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.data.release.utils;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class ListTableModelUnitTests {

	@Test
	public void testNoRows() throws Exception {

		ListTableModel<Object> model = new ListTableModel<>(Collections.emptyList(), (integer, o) -> o);
		model.withHeader("header");

		assertThat(model.getRowCount(), is(1));
		assertThat(model.getColumnCount(), is(1));
		assertThat(model.getValue(0, 0), is(equalTo("header")));
	}

	@Test
	public void testWithRows() throws Exception {

		List<String> simpleList = Arrays.asList("entry1", "entry2");
		ListTableModel<String> model = new ListTableModel<>(simpleList, (integer, o) -> o);
		model.withHeader("header");

		assertThat(model.getRowCount(), is(3));
		assertThat(model.getColumnCount(), is(1));
		assertThat(model.getValue(0, 0), is(equalTo("header")));
		assertThat(model.getValue(1, 0), is(equalTo("entry1")));
		assertThat(model.getValue(2, 0), is(equalTo("entry2")));
	}
}
