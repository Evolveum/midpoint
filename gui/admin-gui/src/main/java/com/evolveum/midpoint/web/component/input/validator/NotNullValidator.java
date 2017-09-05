package com.evolveum.midpoint.web.component.input.validator;

import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;

public class NotNullValidator<T> implements INullAcceptingValidator<T>{

	private static final long serialVersionUID = 1L;
	private String key;

	public NotNullValidator(String errorMessageKey) {
		this.key = errorMessageKey;
	}

	@Override
	public void validate(IValidatable<T> validatable) {
		if (validatable.getValue() == null) {
			ValidationError err = new ValidationError();
			err.addKey(key);
			validatable.error(err);

		}

	}


}
