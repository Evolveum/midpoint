/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.gui.impl.prism;

import java.text.Collator;
import java.util.Comparator;

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;

/**
 * @author katka
 *
 */
public class ItemWrapperComparator<IW extends ItemWrapper> implements Comparator<IW>{

	private Collator collator = null;
	private boolean sorted;
	
	public ItemWrapperComparator(Collator collator, boolean sorted) {
		this.collator = collator;
		this.sorted = sorted;
	}
	
	@Override
	public int compare(IW id1, IW id2) {
		if (sorted) {
			return compareByDisplayNames(id1, id2, collator);
		}
		int displayOrder1 = (id1 == null || id1.getDisplayOrder() == null) ? Integer.MAX_VALUE : id1.getDisplayOrder();
		int displayOrder2 = (id2 == null || id2.getDisplayOrder() == null) ? Integer.MAX_VALUE : id2.getDisplayOrder();
		if (displayOrder1 == displayOrder2) {
			return compareByDisplayNames(id1, id1, collator);
		} else {
			return Integer.compare(displayOrder1, displayOrder2);
		}
	}

	private int compareByDisplayNames(IW pw1, IW pw2, Collator collator) {
		return collator.compare(pw1.getDisplayName(), pw2.getDisplayName());
	}
}
