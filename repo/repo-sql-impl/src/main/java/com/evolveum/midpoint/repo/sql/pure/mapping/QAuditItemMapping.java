package com.evolveum.midpoint.repo.sql.pure.mapping;

import static com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditItem.*;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.pure.SqlTransformer;
import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditItem;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditItem;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Mapping for {@link QAuditItem}, model type is non-containerable {@link ItemPathType}.
 */
public class QAuditItemMapping
        extends QueryModelMapping<ItemPathType, QAuditItem, MAuditItem> {

    public static final String DEFAULT_ALIAS_NAME = "ai";

    public static final QAuditItemMapping INSTANCE = new QAuditItemMapping();

    private QAuditItemMapping() {
        super(TABLE_NAME, DEFAULT_ALIAS_NAME,
                ItemPathType.class, QAuditItem.class,
                RECORD_ID, CHANGED_ITEM_PATH);
    }

    @Override
    public SqlTransformer<ItemPathType, MAuditItem> createTransformer(PrismContext prismContext) {
        throw new UnsupportedOperationException("TODO"); // TODO
    }
}
