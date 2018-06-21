/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.util.histogram;

/**
 * @author mederly
 */
public class HistogramEntry<T> {

	private int itemsCount;
	private long representativeItemValue;
	private T representativeItem;

	public int getItemsCount() {
		return itemsCount;
	}

	public T getRepresentativeItem() {
		return representativeItem;
	}

	public long getRepresentativeItemValue() {
		return representativeItemValue;
	}

	public void record(T item, long value) {
		if (representativeItem == null || representativeItemValue < value) {
			representativeItem = item;
			representativeItemValue = value;
		}
		itemsCount++;
	}
}
