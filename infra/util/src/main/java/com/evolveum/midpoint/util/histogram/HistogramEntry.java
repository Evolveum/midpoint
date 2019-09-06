/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
