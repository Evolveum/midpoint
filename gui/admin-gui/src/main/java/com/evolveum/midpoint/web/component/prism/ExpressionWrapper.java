package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import org.jetbrains.annotations.Nullable;

/**
 * Created by honchar
 */
public class ExpressionWrapper<T> extends PropertyWrapper<T> {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionWrapper.class);
    private ConstructionType construction;

    public ExpressionWrapper(@Nullable ContainerValueWrapper container, PrismProperty property, boolean readonly, ValueStatus status, ItemPath path) {
        super(container, property, readonly, status, path);

        PrismContainer outboundPrismContainer = container.getContainer().getItem();
        if (outboundPrismContainer != null) {
            PrismContainerValue outboundValue = (PrismContainerValue) outboundPrismContainer.getParent();
            if (outboundValue != null) {
                PrismContainer associationContainer = (PrismContainer) outboundValue.getParent();
                if (associationContainer != null) {
                    PrismContainerValue<ConstructionType> constructionContainer = (PrismContainerValue<ConstructionType>) associationContainer.getParent();
                    if (constructionContainer != null) {
                        construction = constructionContainer.asContainerable();
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
        for (ValueWrapper valueWrapper : values) {
            ExpressionType expression = (ExpressionType) ((PrismPropertyValue) valueWrapper.getValue()).getValue();
            ExpressionType oldExpressionValue = (ExpressionType)((PrismPropertyValue)valueWrapper.getOldValue()).getValue();
            try {
                switch (valueWrapper.getStatus()) {
                    case DELETED:
                        return true;
                    case ADDED:
                    case NOT_CHANGED:
                        if (ExpressionUtil.areAllExpressionValuesEmpty(oldExpressionValue) && ExpressionUtil.areAllExpressionValuesEmpty(expression)) {
                            return false;
                        } else if (!ExpressionUtil.areAllExpressionValuesEmpty(oldExpressionValue) && ExpressionUtil.areAllExpressionValuesEmpty(expression)) {
                            return true;
                        } else if (ExpressionUtil.areAllExpressionValuesEmpty(oldExpressionValue) && !ExpressionUtil.areAllExpressionValuesEmpty(expression)) {
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
}
