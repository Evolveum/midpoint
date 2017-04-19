package com.evolveum.midpoint.web.page.admin.reports.component;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.function.Function;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.web.component.form.multivalue.MultiValueChoosePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ConvertingMultiValueChoosePanel<U, T extends ObjectType> extends MultiValueChoosePanel<T> {
	private static final long serialVersionUID = 1L;
	protected Function<T, U> transformFunction;
	protected IModel<List<U>> targetModel;

	public ConvertingMultiValueChoosePanel(String id, List<Class<? extends T>> types,
			Function<T, U> transformFunction, IModel<List<U>> targetModel) {
		this(id, types, transformFunction, targetModel, true);
	}

	public ConvertingMultiValueChoosePanel(String id, List<Class<? extends T>> types, Function<T, U> transformFunction,
			IModel<List<U>> targetModel, boolean multiselect) {
		super(id, new ListModel<>(), types, multiselect);

		this.transformFunction = transformFunction;
		this.targetModel = targetModel;
	}

	@Override
	protected void choosePerformedHook(AjaxRequestTarget target, List<T> selected) {
		if(selected != null) {
			targetModel.setObject(
					selected.stream()
						.map(this::transform)
						.collect(toList()));
		}
	}

	@Override
	protected void removePerformedHook(AjaxRequestTarget target, T value) {
		super.removePerformedHook(target, value);
		targetModel.getObject().remove(transform(value));
	}
	
	protected U transform(T value) {
		return transformFunction.apply(value);
	}
}
