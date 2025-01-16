/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.panel.ObjectCollectionSearchItemPanel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

public class ObjectCollectionSearchItemWrapper extends FilterableSearchItemWrapper {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectCollectionSearchItemWrapper.class);
    private CompiledObjectCollectionView objectCollectionView;

    public ObjectCollectionSearchItemWrapper(@NotNull CompiledObjectCollectionView objectCollectionView) {
        Validate.notNull(objectCollectionView, "Collection must not be null.");
        this.objectCollectionView = objectCollectionView;
        setApplyFilter(true);
        setVisible(true);
    }

    @Override
    public Class<ObjectCollectionSearchItemPanel> getSearchItemPanelClass() {
        return ObjectCollectionSearchItemPanel.class;
    }

    @Override
    public IModel<String> getName() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                if (objectCollectionView.getDisplay() != null) {
                    if (objectCollectionView.getDisplay().getPluralLabel() != null) {
                        return WebComponentUtil.getTranslatedPolyString(objectCollectionView.getDisplay().getPluralLabel());
                    } else if (objectCollectionView.getDisplay().getSingularLabel() != null) {
                        return WebComponentUtil.getTranslatedPolyString(objectCollectionView.getDisplay().getSingularLabel());
                    } else if (objectCollectionView.getDisplay().getLabel() != null) {
                        return WebComponentUtil.getTranslatedPolyString(objectCollectionView.getDisplay().getLabel());
                    }
                }
                if (objectCollectionView.getCollection() != null) {
                    ObjectReferenceType collectionRef = null;
                    if (objectCollectionView.getCollection().getCollectionRef() != null) {
                        collectionRef = objectCollectionView.getCollection().getCollectionRef();

                    }
                    if (objectCollectionView.getCollection().getBaseCollectionRef() != null
                            && objectCollectionView.getCollection().getBaseCollectionRef().getCollectionRef() != null) {
                        collectionRef = objectCollectionView.getCollection().getBaseCollectionRef().getCollectionRef();
                    }
                    if (collectionRef != null) {
                        PolyStringType label = objectCollectionView.getCollection().getCollectionRef().getTargetName();
                        if (label == null) {
                            return objectCollectionView.getCollection().getCollectionRef().getOid();
                        }
                        return WebComponentUtil.getTranslatedPolyString(label);
                    }
                }
                return null;
            }
        };

    }

    @Override
    public IModel<String> getTitle() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                if (objectCollectionView.getFilter() == null) {
                    return null;
                }
                try {
                    SearchFilterType filter = PrismContext.get().getQueryConverter().createSearchFilterType(objectCollectionView.getFilter());
                    return PrismContext.get().xmlSerializer().serializeRealValue(filter);
                } catch (SchemaException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Cannot serialize filter", e);
                }
                return null;
            }
        };
    }

    @Override
    public IModel<String> getHelp() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                if (objectCollectionView == null) {
                    return null;
                }

                return objectCollectionView.getObjectCollectionDescription();
            }
        };
    }


    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        if (objectCollectionView.getFilter() == null) {
            return null;
        }
        return WebComponentUtil.evaluateExpressionsInFilter(objectCollectionView.getFilter(), variables, new OperationResult("evaluate filter"), pageBase);
    }

    @Override
    public DisplayableValue<String> getDefaultValue() {
        return new SearchValue<>();
    }

    public CompiledObjectCollectionView getObjectCollectionView() {
        return objectCollectionView;
    }
}
