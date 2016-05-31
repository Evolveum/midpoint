package com.evolveum.midpoint.gui.api.model;

/**
 * @author mederly
 */

import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

public class NonEmptyPropertyModel<T> extends PropertyModel<T> implements NonEmptyModel<T> {

	public NonEmptyPropertyModel(Object modelObject, String expression) {
		super(modelObject, expression);
	}

	@Override
	@NotNull
	public T getObject() {
		return super.getObject();
	}

	@Override
	public void setObject(@NotNull T object) {
		super.setObject(object);
	}
}
