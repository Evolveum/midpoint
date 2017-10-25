package com.evolveum.midpoint.web.page.admin.cases.dto;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.prism.PrismConstants.T_PARENT;

/**
 * TODO better class name
 * @author bpowers
 */
public class SearchingUtils {

    @SuppressWarnings("unused")
    private static final Trace LOGGER = TraceManager.getTrace(SearchingUtils.class);

    public static final String WORK_ITEM_CLOSE_TIMESTAMP = CaseWorkItemType.F_CLOSE_TIMESTAMP.getLocalPart();
    public static final String WORK_ITEM_DEADLINE = CaseWorkItemType.F_DEADLINE.getLocalPart();
    public static final String CASE_OPEN_TIMESTAMP = MetadataType.F_CREATE_TIMESTAMP.getLocalPart();
    public static final String CASE_DESCRIPTION = CaseType.F_DESCRIPTION.getLocalPart();
    public static final String CASE_OBJECT_NAME = CaseType.F_OBJECT_REF.getLocalPart();
    public static final String CASE_STATE = CaseType.F_STATE.getLocalPart();

    @NotNull
    public static List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
        if (sortParam == null || sortParam.getProperty() == null) {
            return Collections.emptyList();
        }
        String propertyName = sortParam.getProperty();
        ItemPath casePath = new ItemPath(T_PARENT);
        ItemPath workItemPath = ItemPath.EMPTY_PATH;
        ItemPath primaryItemPath;
        if (CASE_DESCRIPTION.equals(propertyName)) {
            primaryItemPath = casePath.subPath(CaseType.F_DESCRIPTION);
        } else if (WORK_ITEM_CLOSE_TIMESTAMP.equals(propertyName)) {
            primaryItemPath = workItemPath.subPath(CaseWorkItemType.F_CLOSE_TIMESTAMP);
        } else if (WORK_ITEM_DEADLINE.equals(propertyName)) {
            primaryItemPath = workItemPath.subPath(CaseWorkItemType.F_DEADLINE);
        } else if (CASE_OPEN_TIMESTAMP.equals(propertyName)) {
            primaryItemPath = casePath.subPath(CaseType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP);
        } else if (CASE_OBJECT_NAME.equals(propertyName)) {
            primaryItemPath = casePath.subPath(CaseType.F_OBJECT_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME);
        } else if (CASE_STATE.equals(propertyName)) {
            primaryItemPath = casePath.subPath(CaseType.F_STATE);
            LOGGER.debug("CASE STATE. PRIMARY ITEM PATH: {}", primaryItemPath);
        } else {
            primaryItemPath = new ItemPath(new QName(SchemaConstantsGenerated.NS_COMMON, propertyName));
        }
        List<ObjectOrdering> rv = new ArrayList<>();
        rv.add(ObjectOrdering.createOrdering(primaryItemPath, sortParam.isAscending() ? OrderDirection.ASCENDING : OrderDirection.DESCENDING));
        // additional criteria are used to avoid random shuffling if first criteria is too vague)
        rv.add(ObjectOrdering.createOrdering(casePath.subPath(PrismConstants.T_ID), OrderDirection.ASCENDING));			// case ID
        rv.add(ObjectOrdering.createOrdering(workItemPath.subPath(PrismConstants.T_ID), OrderDirection.ASCENDING));			// work item ID
        return rv;
    }
}
