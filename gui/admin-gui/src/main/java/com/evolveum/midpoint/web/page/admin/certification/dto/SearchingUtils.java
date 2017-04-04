package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.prism.PrismConstants.T_PARENT;

/**
 * TODO better class name
 * @author mederly
 */
public class SearchingUtils {

	private static final Trace LOGGER = TraceManager.getTrace(SearchingUtils.class);

	public static final String TARGET_NAME = AccessCertificationCaseType.F_TARGET_REF.getLocalPart();
	public static final String OBJECT_NAME = AccessCertificationCaseType.F_OBJECT_REF.getLocalPart();
	public static final String TENANT_NAME = AccessCertificationCaseType.F_TENANT_REF.getLocalPart();	// seem to be unused now
	public static final String ORG_NAME = AccessCertificationCaseType.F_ORG_REF.getLocalPart();			// seem to be unused now
	public static final String CAMPAIGN_NAME = "campaignName";

	@NotNull
	public static List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam, boolean isWorkItem) {
		if (sortParam == null || sortParam.getProperty() == null) {
			return Collections.emptyList();
		}
		String propertyName = sortParam.getProperty();

		ItemPath campaignPath = isWorkItem ? new ItemPath(T_PARENT, T_PARENT) : new ItemPath(T_PARENT);
		ItemPath primaryItemPath;
		if (TARGET_NAME.equals(propertyName)) {
			primaryItemPath = new ItemPath(AccessCertificationCaseType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME);
		} else if (OBJECT_NAME.equals(propertyName)) {
			primaryItemPath = new ItemPath(AccessCertificationCaseType.F_OBJECT_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME);
		} else if (TENANT_NAME.equals(propertyName)) {
			primaryItemPath = new ItemPath(AccessCertificationCaseType.F_TENANT_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME);
		} else if (ORG_NAME.equals(propertyName)) {
			primaryItemPath = new ItemPath(AccessCertificationCaseType.F_ORG_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME);
		} else if (CAMPAIGN_NAME.equals(propertyName)) {
			primaryItemPath = campaignPath.subPath(ObjectType.F_NAME);
		} else {
			primaryItemPath = new ItemPath(new QName(SchemaConstantsGenerated.NS_COMMON, propertyName));
		}
		ObjectOrdering primary = ObjectOrdering.createOrdering(primaryItemPath, sortParam.isAscending() ? OrderDirection.ASCENDING : OrderDirection.DESCENDING);
		ObjectOrdering secondary = ObjectOrdering.createOrdering(new ItemPath(PrismConstants.T_ID), OrderDirection.ASCENDING);     // to avoid random shuffling if first criteria is too vague
		ObjectOrdering tertiary = ObjectOrdering.createOrdering(campaignPath.subPath(PrismConstants.T_ID), OrderDirection.ASCENDING); // campaign OID
		return Arrays.asList(primary, secondary, tertiary);
	}
}
