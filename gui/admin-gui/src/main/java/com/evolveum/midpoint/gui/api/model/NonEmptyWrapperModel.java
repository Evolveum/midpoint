package com.evolveum.midpoint.gui.api.model;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public class NonEmptyWrapperModel<T> implements NonEmptyModel<T> {

	private final IModel<T> model;

	public NonEmptyWrapperModel(@NotNull IModel<T> model) {
		this.model = model;
	}

	@NotNull
	@Override
	public T getObject() {
		return model.getObject();
	}

	@Override
	public void setObject(@NotNull T object) {
		model.setObject(object);
	}

	@Override
	public void detach() {
		model.detach();
	}
}
