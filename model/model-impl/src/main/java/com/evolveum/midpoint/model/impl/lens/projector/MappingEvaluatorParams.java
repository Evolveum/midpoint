/**
 * Copyright (c) 2014-2017 Evolveum
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
package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.Collection;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.repo.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class MappingEvaluatorParams<V extends PrismValue, D extends ItemDefinition, T extends ObjectType, F extends FocusType> {

	private Collection<MappingType> mappingTypes;
	private String mappingDesc;
	private XMLGregorianCalendar now;
	private MappingInitializer<V,D> initializer;
	private MappingLoader<T> targetLoader;
	private MappingOutputProcessor<V> processor;
	private PrismObject<T> aPrioriTargetObject;
	private ObjectDelta<T> aPrioriTargetDelta;
	private LensElementContext<T> targetContext;
	private ObjectDeltaObject<?> sourceContext;
	private ItemPath defaultTargetItemPath;
	// Only needed if defaultTargetItemPath == null
	private D targetItemDefinition;
	private MappingTimeEval evaluateCurrent;
	private boolean evaluateWeak = true;
	private LensContext<F> context;
	private boolean hasFullTargetObject;
	// If set to true then the target cannot be overridden in mapping
	private boolean fixTarget = false;

	public Collection<MappingType> getMappingTypes() {
		return mappingTypes;
	}

	public void setMappingTypes(Collection<MappingType> mappingTypes) {
		this.mappingTypes = mappingTypes;
	}

	public String getMappingDesc() {
		return mappingDesc;
	}

	public void setMappingDesc(String mappingDesc) {
		this.mappingDesc = mappingDesc;
	}

	public XMLGregorianCalendar getNow() {
		return now;
	}

	public void setNow(XMLGregorianCalendar now) {
		this.now = now;
	}

	public MappingInitializer<V,D> getInitializer() {
		return initializer;
	}

	public void setInitializer(MappingInitializer<V,D> initializer) {
		this.initializer = initializer;
	}

	public MappingLoader<T> getTargetLoader() {
		return targetLoader;
	}

	public void setTargetLoader(MappingLoader<T> targetLoader) {
		this.targetLoader = targetLoader;
	}

	public MappingOutputProcessor<V> getProcessor() {
		return processor;
	}

	public void setProcessor(MappingOutputProcessor<V> processor) {
		this.processor = processor;
	}

	public PrismObject<T> getAPrioriTargetObject() {
		return aPrioriTargetObject;
	}

	public void setAPrioriTargetObject(PrismObject<T> aPrioriTargetObject) {
		this.aPrioriTargetObject = aPrioriTargetObject;
	}

	public ObjectDelta<T> getAPrioriTargetDelta() {
		return aPrioriTargetDelta;
	}

	public void setAPrioriTargetDelta(ObjectDelta<T> aPrioriTargetDelta) {
		this.aPrioriTargetDelta = aPrioriTargetDelta;
	}

	public LensElementContext<T> getTargetContext() {
		return targetContext;
	}

	public void setTargetContext(LensElementContext<T> targetContext) {
		this.targetContext = targetContext;
	}

	public ObjectDeltaObject<?> getSourceContext() {
		return sourceContext;
	}

	public void setSourceContext(ObjectDeltaObject<?> sourceContext) {
		this.sourceContext = sourceContext;
	}

	public MappingTimeEval getEvaluateCurrent() {
		return evaluateCurrent;
	}

	public void setEvaluateCurrent(MappingTimeEval evaluateCurrent) {
		this.evaluateCurrent = evaluateCurrent;
	}

	public boolean isEvaluateWeak() {
		return evaluateWeak;
	}

	public void setEvaluateWeak(boolean evaluateWeak) {
		this.evaluateWeak = evaluateWeak;
	}

	public LensContext<F> getContext() {
		return context;
	}

	public void setContext(LensContext<F> context) {
		this.context = context;
	}

	public boolean hasFullTargetObject() {
		return hasFullTargetObject;
	}

	public void setHasFullTargetObject(boolean hasFullTargetObject) {
		this.hasFullTargetObject = hasFullTargetObject;
	}

	public ItemPath getDefaultTargetItemPath() {
		return defaultTargetItemPath;
	}

	public void setDefaultTargetItemPath(ItemPath defaultTargetItemPath) {
		this.defaultTargetItemPath = defaultTargetItemPath;
	}

	public boolean isFixTarget() {
		return fixTarget;
	}

	public void setFixTarget(boolean fixTarget) {
		this.fixTarget = fixTarget;
	}

	public D getTargetItemDefinition() {
		return targetItemDefinition;
	}

	public void setTargetItemDefinition(D targetItemDefinition) {
		this.targetItemDefinition = targetItemDefinition;
	}

}
