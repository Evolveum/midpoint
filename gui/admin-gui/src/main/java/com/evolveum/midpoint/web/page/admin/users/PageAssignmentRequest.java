package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.input.ListMultipleChoiceTransferPanel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.certification.PageAdminCertification;
import com.evolveum.midpoint.web.page.admin.roles.dto.RolesSearchDto;
import com.evolveum.midpoint.web.session.RolesStorage;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Honchar on 13.01.2016.
 */
@PageDescriptor(url = "/admin/users/request",
        action = {
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,         //TODO add new auth path
                        label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION) })

public class PageAssignmentRequest extends PageAdmin {
    private static final String ID_MULTIPLE_CHOICE_PANEL = "panel";
    private static final Trace LOGGER = TraceManager.getTrace(PageAssignmentRequest.class);

    private static final String DOT_CLASS = PageAssignmentRequest.class.getName() + ".";
    private static final String OPERATION_LOAD_ROLE_TYPES = DOT_CLASS + "loadRoleTypes";

    public PageAssignmentRequest(){
        initLayout();
    }

    private void initLayout() {
        ObjectDataProvider provider = new ObjectDataProvider(PageAssignmentRequest.this, RoleType.class) {

            @Override
            protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
                RolesStorage storage = getSessionStorage().getRoles();
                storage.setRolesPaging(paging);
            }
        };
        provider.setQuery(createQuery());




        IModel<List<String>> testModel1 = new IModel<List<String>>() {
            @Override
            public List<String> getObject() {
//                List<String> list = createAvailableRoleTypesList();
                List<String> list = new ArrayList<>();
                for (int i =0; i <5000; i++) {
                    list.add("Role" + i);
                }
               return list;
            }

            @Override
            public void setObject(List<String> userTypes) {

            }

            @Override
            public void detach() {
            }
        };
        IModel<List<String>> testModel2 = new IModel<List<String>>() {
            @Override
            public List<String> getObject() {
//               return createAvailableRoleTypesList();
                List<String> list = new ArrayList<>();
                list.add("Role 11");
                list.add("Role 444");
                return list;
            }

            @Override
            public void setObject(List<String> userTypes) {

            }

            @Override
            public void detach() {
            }
        };
        ListMultipleChoiceTransferPanel<String>  panel = new ListMultipleChoiceTransferPanel<>(ID_MULTIPLE_CHOICE_PANEL, testModel1, testModel2);
        add(panel);
    }


    private List<String> createAvailableRoleTypesList(){
        List<String> roleTypes = new ArrayList<>();
        List<? extends DisplayableValue<String>> displayableValues = getLookupDisplayableList();

        if (displayableValues != null) {
            for (DisplayableValue<String> displayable : displayableValues) {
                roleTypes.add(displayable.getLabel());
            }
        }

        return roleTypes;
    }

    private List<? extends DisplayableValue<String>> getLookupDisplayableList(){
        List<DisplayableValue<String>> list = new ArrayList<>();
        ModelInteractionService interactionService = WebMiscUtil.getPageBase(this).getModelInteractionService();
        OperationResult result = new OperationResult(OPERATION_LOAD_ROLE_TYPES);

        try {
            RoleSelectionSpecification roleSpecification = interactionService.getAssignableRoleSpecification(getUserDefinition(), result);
            return roleSpecification.getRoleTypes();

        } catch (SchemaException | ConfigurationException | ObjectNotFoundException e) {
            LOGGER.error("Could not retrieve available role types for search purposes.", e);
            result.recordFatalError("Could not retrieve available role types for search purposes.", e);
        }

        return list;
    }

    protected PrismObject<UserType>  getUserDefinition(){
        try {
            return getSecurityEnforcer().getPrincipal().getUser().asPrismObject();
        } catch (SecurityViolationException e) {
            LOGGER.error("Could not retrieve logged user for security evaluation.", e);
        }
        return null;
        }

    private ObjectQuery createQuery() {
//        RolesSearchDto dto = searchModel.getObject();
//        String text = dto.getText();
//        Boolean requestable = dto.getRequestableValue();
        ObjectQuery query = new ObjectQuery();
        List<ObjectFilter> filters = new ArrayList<>();

//        if (StringUtils.isNotEmpty(text)) {
//            PolyStringNormalizer normalizer = getPrismContext().getDefaultPolyStringNormalizer();
//            String normalizedText = normalizer.normalize(text);
//
//            ObjectFilter substring = SubstringFilter.createSubstring(RoleType.F_NAME, RoleType.class, getPrismContext(),
//                    PolyStringNormMatchingRule.NAME, normalizedText);
//            filters.add(substring);
//        }

//        if (requestable != null) {
//            EqualFilter requestableFilter = EqualFilter.createEqual(RoleType.F_REQUESTABLE, RoleType.class, getPrismContext(),
//                    null, requestable);
//
//            if (requestable) {
//                filters.add(requestableFilter);
//            } else {
//                requestableFilter = EqualFilter.createEqual(RoleType.F_REQUESTABLE, RoleType.class, getPrismContext(),
//                        null, false);
//                EqualFilter nullFilter = EqualFilter.createEqual(RoleType.F_REQUESTABLE, RoleType.class, getPrismContext(),
//                        null, null);
//                OrFilter or = OrFilter.createOr(requestableFilter, nullFilter);
//                filters.add(or);
//            }
//        }

        if (!filters.isEmpty()) {
            query.setFilter(AndFilter.createAnd(filters));
        }

        return query;
    }

}
