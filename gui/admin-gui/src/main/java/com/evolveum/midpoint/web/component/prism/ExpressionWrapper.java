package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.gui.impl.component.prism.ExpressionPropertyPanel;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.ExpressionValuePanel;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Nullable;

/**
 * Created by honchar
 */
public class ExpressionWrapper extends PropertyWrapper<ExpressionType> {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionWrapper.class);
    private ConstructionType construction;

    public ExpressionWrapper(@Nullable ContainerValueWrapper container, PrismProperty<ExpressionType> property, boolean readonly,
            ValueStatus status, ItemPath path, PrismContext prismContext) {
        super(container, property, readonly, status, path, prismContext);

        PrismContainer outboundPrismContainer = container.getContainer().getItem();
        if (outboundPrismContainer != null) {
            PrismContainerValue outboundValue = (PrismContainerValue) outboundPrismContainer.getParent();
            if (outboundValue != null) {
                PrismContainer associationContainer = (PrismContainer) outboundValue.getParent();
                if (associationContainer != null) {
                    PrismContainerValue<?> constructionContainer = (PrismContainerValue<?>) associationContainer.getParent();
                    if (constructionContainer != null && constructionContainer.asContainerable() instanceof ConstructionType) {
                        construction = (ConstructionType) constructionContainer.asContainerable();
                    }
                }
            }
        }
    }

    public ConstructionType getConstruction() {
        return construction;
    }

    public void setConstruction(ConstructionType construction) {
        this.construction = construction;
    }

    @Override
    public boolean hasChanged() {
        for (ValueWrapperOld valueWrapper : values) {
            ExpressionType expression = (ExpressionType) ((PrismPropertyValue) valueWrapper.getValue()).getValue();
            ExpressionType oldExpressionValue = (ExpressionType)((PrismPropertyValue)valueWrapper.getOldValue()).getValue();
            try {
                switch (valueWrapper.getStatus()) {
                    case DELETED:
                        return true;
                    case ADDED:
                    case NOT_CHANGED:
                        if (ExpressionUtil.areAllExpressionValuesEmpty(oldExpressionValue, prismContext) && ExpressionUtil.areAllExpressionValuesEmpty(expression, prismContext)) {
                            return false;
                        } else if (!ExpressionUtil.areAllExpressionValuesEmpty(oldExpressionValue, prismContext) && ExpressionUtil.areAllExpressionValuesEmpty(expression, prismContext)) {
                            return true;
                        } else if (ExpressionUtil.areAllExpressionValuesEmpty(oldExpressionValue, prismContext) && !ExpressionUtil.areAllExpressionValuesEmpty(expression, prismContext)) {
                            return true;
                        } else if (valueWrapper.hasValueChanged()) {
                            return true;
                        }
                }
            } catch (SchemaException e) {
                LoggingUtils.logException(LOGGER, "Cannot check changes of the expression value" + expression, e);
                return false;
            }
        }

        return false;
    }
    
    public Panel createPanel(String id, Form form, ItemVisibilityHandlerOld visibilityHandler) {
    	return new ExpressionPropertyPanel(id, this, form, visibilityHandler);
    };
    
}
