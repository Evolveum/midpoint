/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async;

import com.evolveum.midpoint.provisioning.ucf.api.ConfigurationProperty;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateErrorHandlingActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateSourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateSourcesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public class AsyncUpdateConnectorConfiguration {

	private AsyncUpdateSourcesType sources;
	private ExpressionType transformExpression;
	private AsyncUpdateErrorHandlingActionType errorHandlingAction;

	@ConfigurationProperty
	public AsyncUpdateSourcesType getSources() {
		return sources;
	}

	public void setSources(AsyncUpdateSourcesType sources) {
		this.sources = sources;
	}

	@ConfigurationProperty
	public ExpressionType getTransformExpression() {
		return transformExpression;
	}

	public void setTransformExpression(ExpressionType transformExpression) {
		this.transformExpression = transformExpression;
	}

	@ConfigurationProperty
	public AsyncUpdateErrorHandlingActionType getErrorHandlingAction() {
		return errorHandlingAction;
	}

	public void setErrorHandlingAction(AsyncUpdateErrorHandlingActionType errorHandlingAction) {
		this.errorHandlingAction = errorHandlingAction;
	}

	public void validate() {
		if (getAllSources().isEmpty()) {
			throw new IllegalStateException("No asynchronous update sources were configured");
		}
	}

	@NotNull
	List<AsyncUpdateSourceType> getAllSources() {
		List<AsyncUpdateSourceType> allSources = new ArrayList<>();
		if (sources != null) {
			allSources.addAll(sources.getAmqp091());
			allSources.addAll(sources.getOther());
		}
		return allSources;
	}

	boolean hasSourcesChanged(AsyncUpdateConnectorConfiguration other) {
		// we can consider weaker comparison here in the future
		return other == null || !Objects.equals(other.sources, sources);
	}
}
