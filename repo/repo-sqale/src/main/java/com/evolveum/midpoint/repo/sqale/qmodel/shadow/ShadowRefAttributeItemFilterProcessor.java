package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.ItemValueFilterProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.querydsl.core.types.Predicate;

import javax.xml.namespace.QName;

public class ShadowRefAttributeItemFilterProcessor extends ItemValueFilterProcessor<ValueFilter<?,?>> {

    public ShadowRefAttributeItemFilterProcessor(QName itemName, SqlQueryContext<?, ?, ?> context) {
        super(context);
    }

    @Override
    public Predicate process(ValueFilter<?,?> filter) throws RepositoryException {
        return null;
    }
}
