package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pmederly on 10. 2. 2016.
 */
public class CertMiscUtil {

    public static String getStopReviewOnText(List<AccessCertificationResponseType> stopOn, PageBase page) {
        if (stopOn == null) {
            return page.getString("PageCertDefinition.stopReviewOnDefault");
        } else if (stopOn.isEmpty()) {
            return page.getString("PageCertDefinition.stopReviewOnNone");
        } else {
            List<String> names = new ArrayList<>(stopOn.size());
            for (AccessCertificationResponseType r : stopOn) {
                names.add(page.createStringResource(r).getString());
            }
            return StringUtils.join(names, ", ");
        }
    }

}
