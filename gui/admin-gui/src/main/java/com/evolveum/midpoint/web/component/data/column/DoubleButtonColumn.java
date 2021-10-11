/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.web.component.data.DoubleButtonPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 *  @author shood
 */
public class DoubleButtonColumn<T extends Serializable>  extends AbstractColumn<T, String>{

    public static final String BUTTON_BASE_CLASS = "btn";
    public static final String BUTTON_DISABLED = "disabled";

    private DoubleButtonPanel panel;

    public enum ButtonColorClass {
        DEFAULT("btn-default"), PRIMARY("btn-primary"), SUCCESS("btn-success"),
        INFO("btn-info"), WARNING("btn-warning"), DANGER("btn-danger");

        private final String stringValue;

        private ButtonColorClass(final String s) { stringValue = s; }
        public String toString() { return stringValue; }
    }

    public enum ButtonSizeClass {
        LARGE("btn-lg"), DEFAULT(""), SMALL("btn-sm"), EXTRA_SMALL("btn-xs");

        private final String stringValue;

        private ButtonSizeClass(final String s) { stringValue = s; }
        public String toString() { return stringValue; }
    }

    private String firstCaption;
    private String secondCaption;
    private IModel<T> rowModel;

    private String propertyExpression;

    public DoubleButtonColumn(IModel<String> displayModel, String propertyExpression){
        super(displayModel);
        this.propertyExpression = propertyExpression;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<T>> cellItem, String componentId,
                             final IModel<T> rowModel){
        this.rowModel = rowModel;

        panel = new DoubleButtonPanel<T>(componentId, rowModel){

            @Override
            public String getFirstCssSizeClass(){
                return getFirstSizeCssClass();
            }

            @Override
            public String getSecondCssSizeClass(){
                return getSecondSizeCssClass();
            }

            @Override
            public String getFirstCssColorClass(){
                return getFirstColorCssClass();
            }

            @Override
            public String getSecondCssColorClass(){
                return getSecondColorCssClass();
            }

            @Override
            public String getFirstCaption(){
                return getFirstCap();
            }

            @Override
            public String getSecondCaption(){
                return getSecondCap();
            }

            @Override
            public void firstPerformed(AjaxRequestTarget target, IModel<T> model){
                firstClicked(target, model);
            }

            @Override
            public void secondPerformed(AjaxRequestTarget target, IModel<T> model){
                secondClicked(target, model);
            }

            @Override
            public boolean isFirstEnabled(IModel<T> model){
                return isFirstButtonEnabled(model);
            }

            @Override
            public boolean isSecondEnabled(IModel<T> model){
                return isSecondButtonEnabled(model);
            }
        };
        cellItem.add(panel);
    }

    public void firstClicked(AjaxRequestTarget target, IModel<T> model){}
    public void secondClicked(AjaxRequestTarget target, IModel<T> model){}

    public String getFirstSizeCssClass(){
        return ButtonSizeClass.SMALL.toString();
    }

    public String getSecondSizeCssClass(){
        return ButtonSizeClass.SMALL.toString();
    }

    public String getFirstColorCssClass(){
        return ButtonColorClass.DEFAULT.toString();
    }

    public String getSecondColorCssClass(){
        return ButtonColorClass.DEFAULT.toString();
    }

    public String getFirstCap(){
        return firstCaption;
    }

    public String getSecondCap(){
        return secondCaption;
    }

    public boolean isFirstButtonEnabled(IModel<T> model){
        return true;
    }

    public boolean isSecondButtonEnabled(IModel<T> model){
        return true;
    }

    public DoubleButtonPanel getButtonPanel(){
        return panel;
    }

    protected IModel<T> getRowModel(){
        return rowModel;
    }
}
