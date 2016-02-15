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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import org.springframework.shell.table.TableModel;
import org.springframework.util.Assert;

/**
 * List table model for a {@link org.springframework.shell.table.Table}. The model is backed by a list and allows to set
 * header labels. This class uses a {@link BiFunction} to retrieve the value for the requested column.
 *
 * @author Mark Paluch
 */
public class ListTableModel<T> extends TableModel {

	private final List<T> elements;
	private final List<String> headers = new ArrayList<>();
	private final BiFunction<Integer, T, Object> valueFunction;

	/**
	 * Creates a new {@link ListTableModel} with a value function. The value function accepts the row index and the
	 * element and returns the resulting value.
	 *
	 * @param elements
	 * @param valueFunction
	 */
	public ListTableModel(List<T> elements, BiFunction<Integer, T, Object> valueFunction) {

		Assert.notNull(elements, "Elements must not be null");
		Assert.notNull(valueFunction, "Value function must not be null");

		this.elements = elements;
		this.valueFunction = valueFunction;
	}

	/**
	 * Add a header label to the model.
	 *
	 * @param header
	 * @return the model
	 */
	public ListTableModel<T> withHeader(String... header) {

		Assert.notNull(header, "Header must not be null");
		headers.addAll(Arrays.asList(header));
		return this;
	}

	@Override
	public int getRowCount() {
		return elements.size() + 1;
	}

	@Override
	public int getColumnCount() {
		return headers.size();
	}

	@Override
	public Object getValue(int row, int column) {
		if (row == 0) {
			return headers.get(column);
		}
		return valueFunction.apply(column, elements.get(row - 1));
	}
}
