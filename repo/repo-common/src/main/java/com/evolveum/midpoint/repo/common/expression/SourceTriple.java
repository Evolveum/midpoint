/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.repo.common.expression;

import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class SourceTriple<V extends PrismValue,D extends ItemDefinition> extends PrismValueDeltaSetTriple<V> {

	private Source<V,D> source;

	public SourceTriple(Source<V,D> source) {
		super();
		this.source = source;
	}

	public SourceTriple(Source<V,D> source, @NotNull Collection<V> zeroSet, @NotNull Collection<V> plusSet, @NotNull Collection<V> minusSet) {
		super(zeroSet, plusSet, minusSet);
		this.source = source;
	}

	public Source<V,D> getSource() {
		return source;
	}

	public void setSource(Source<V,D> source) {
		this.source = source;
	}

	public QName getName() {
		return source.getName();
	}

	public ItemPath getResidualPath() {
		return source.getResidualPath();
	}
	
}
