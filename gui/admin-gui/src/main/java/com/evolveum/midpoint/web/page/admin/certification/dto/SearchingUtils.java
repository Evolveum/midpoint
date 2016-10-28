package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.prism.PrismConstants.T_PARENT;

/**
 * Created by pmederly on 29. 1. 2016.
 */
public class SearchingUtils {

    /**
     * Maps from "old style" of specifying sorting criteria to current one:
     *   targetRef -> targetRef/@/name
     *   objectRef -> objectRef/@/name
     *   campaignRef -> ../name
     *
     * Plus adds ID as secondary criteria, in order to avoid random shuffling the result set.
     *
     * Temporary solution - until we implement that in GUI.
     */
    static ObjectQuery hackPaging(ObjectQuery query) {
        if (query.getPaging() == null || !query.getPaging().hasOrdering()) {
            return query;
        }
        if (query.getPaging().getOrderingInstructions().size() > 1) {
            return query;
        }
        ItemPath oldPath = query.getPaging().getOrderBy();
        OrderDirection oldDirection = query.getPaging().getDirection();
        if (oldPath.size() != 1 || !(oldPath.first() instanceof NameItemPathSegment)) {
            return query;
        }
        QName oldName = ((NameItemPathSegment) oldPath.first()).getName();
        ItemPath newPath = CertCampaignTypeUtil.getOrderBy(oldName);
        ObjectPaging paging1 = query.getPaging().clone();
        ObjectOrdering primary = ObjectOrdering.createOrdering(newPath, oldDirection);
        ObjectOrdering secondary = ObjectOrdering.createOrdering(new ItemPath(PrismConstants.T_ID), OrderDirection.ASCENDING);     // to avoid random shuffling if first criteria is too vague
        ObjectOrdering tertiary = ObjectOrdering.createOrdering(new ItemPath(T_PARENT, PrismConstants.T_ID), OrderDirection.ASCENDING); // campaign OID
        paging1.setOrdering(primary, secondary, tertiary);
        query.setPaging(paging1);
        return query;
    }

}
