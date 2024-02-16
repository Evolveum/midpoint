package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.businessRoles.JobBusinessRole.getRandomJobBusinessRole;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.businessRoles.LocationBusinessRole.getRandomLocationBusinessRole;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.businessRoles.PlanktonApplicationRole.getLargeRandomPlanktonRoles;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.businessRoles.PlanktonApplicationRole.getRandomPlanktonRoles;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.objectTypes.JobTitles.getRandomlyJobTitles;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.objectTypes.JobTitles.getRandomlyJobTitlesWithNone;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.polystring.PolyString;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.businessRoles.BirthrightRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.businessRoles.JobBusinessRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.businessRoles.LocationBusinessRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.businessRoles.PlanktonApplicationRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.objectTypes.BirthRoleType;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.objectTypes.EmployeeType;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.objectTypes.JobTitles;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

public class RoleAnalysisDataGeneratorUtils {

    public static void unnassignAll(@NotNull RepositoryService repositoryService,
            @NotNull PageBase pageBase,
            RoleAnalysisService roleAnalysisService) {
        Task task = pageBase.createSimpleTask("resolveRegularUsers");
        OperationResult result = task.getResult();
        SearchResultList<PrismObject<UserType>> users;
        try {
            users = repositoryService.searchObjects(UserType.class, null, null, result);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        for (PrismObject<UserType> user : users) {

            String userOid = user.getOid();
            PolyString name = user.getName();
            if (name == null) {
                continue;
            }

            String stringName = name.toString();

            if (stringName.equals("administrator")) {
                continue;
            }

            UserType userObject = user.asObjectable();

            List<PrismObject<RoleType>> rolesOidAssignment = getRoles(userObject, roleAnalysisService, task, result);

            for (PrismObject<RoleType> roleTypePrismObject : rolesOidAssignment) {
                RoleType objectable = roleTypePrismObject.asObjectable();

                List<ItemDelta<?, ?>> modifications = new ArrayList<>();
                try {

                    modifications.add(PrismContext.get().deltaFor(UserType.class)
                            .item(UserType.F_ASSIGNMENT).delete(createAssignment(objectable.getOid(), pageBase))
                            .asItemDelta());

                    repositoryService.modifyObject(UserType.class, userOid, modifications, result);

                } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
                    throw new RuntimeException(e);
                }

            }

        }

    }

    public static void remakeBusinessRoles(@NotNull RepositoryService repositoryService,
            @NotNull PageBase pageBase,
            RoleAnalysisService roleAnalysisService) {
        Task task = pageBase.createSimpleTask("resolveRegularUsers");
        OperationResult result = task.getResult();
        SearchResultList<PrismObject<UserType>> users;
        try {
            users = repositoryService.searchObjects(UserType.class, null, null, result);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        for (PrismObject<UserType> user : users) {

            String userOid = user.getOid();
            PolyString name = user.getName();
            if (name == null) {
                continue;
            }

            String stringName = name.toString();

            if (stringName.equals("administrator")) {
                continue;
            }

            UserType userObject = user.asObjectable();

            List<PrismObject<RoleType>> rolesOidAssignment = getRolesOidAssignment(userObject, roleAnalysisService, task, result);

            for (PrismObject<RoleType> roleTypePrismObject : rolesOidAssignment) {
                RoleType objectable = roleTypePrismObject.asObjectable();
                List<AssignmentType> inducement = objectable.getInducement();

                List<ItemDelta<?, ?>> modifications = new ArrayList<>();
                try {

                    for (AssignmentType assignmentType : inducement) {
                        modifications.add(PrismContext.get().deltaFor(UserType.class)
                                .item(UserType.F_ASSIGNMENT).add(createAssignment(assignmentType.getTargetRef().getOid(), pageBase))
                                .asItemDelta());
                    }

                    modifications.add(PrismContext.get().deltaFor(UserType.class)
                            .item(UserType.F_ASSIGNMENT).delete(createAssignment(objectable.getOid(), pageBase))
                            .asItemDelta());

                    repositoryService.modifyObject(UserType.class, userOid, modifications, result);

                } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
                    throw new RuntimeException(e);
                }

            }

        }

    }

    public static List<PrismObject<RoleType>> getRolesOidAssignment(@NotNull AssignmentHolderType object, RoleAnalysisService roleAnalysisService, Task task, OperationResult result) {
        List<AssignmentType> assignments = object.getAssignment();

        List<PrismObject<RoleType>> list = new ArrayList<>();
        for (AssignmentType assignment : assignments) {
            ObjectReferenceType targetRef = assignment.getTargetRef();
            if (targetRef != null) {
                if (targetRef.getType().equals(RoleType.COMPLEX_TYPE)) {
                    String oid = targetRef.getOid();
                    PrismObject<RoleType> roleTypeObject = roleAnalysisService.getRoleTypeObject(oid, task, result);
                    if (roleTypeObject != null) {
                        RoleType objectable = roleTypeObject.asObjectable();
                        List<ObjectReferenceType> archetypeRef = objectable.getArchetypeRef();
                        if (archetypeRef != null && !archetypeRef.isEmpty()) {
                            ObjectReferenceType objectReferenceType = archetypeRef.get(0);
                            String oid1 = objectReferenceType.getOid();
                            if (oid1.equals("00000000-0000-0000-0000-000000000321")) {
                                list.add(roleTypeObject);
                            }
                        }
                    }
                }
            }
        }

        return list;
    }

    public static List<PrismObject<RoleType>> getRoles(@NotNull AssignmentHolderType object,
            RoleAnalysisService roleAnalysisService, Task task, OperationResult result) {
        List<AssignmentType> assignments = object.getAssignment();

        List<PrismObject<RoleType>> list = new ArrayList<>();
        for (AssignmentType assignment : assignments) {
            ObjectReferenceType targetRef = assignment.getTargetRef();
            if (targetRef != null) {
                if (targetRef.getType().equals(RoleType.COMPLEX_TYPE)) {
                    String oid = targetRef.getOid();
                    PrismObject<RoleType> roleTypeObject = roleAnalysisService.getRoleTypeObject(oid, task, result);
                    if (roleTypeObject != null) {
                        list.add(roleTypeObject);
                    }
                }
            }
        }

        return list;
    }

    public static void generateTD(@NotNull RepositoryService repositoryService, @NotNull PageBase pageBase) {
        Task task = pageBase.createSimpleTask("resolveRegularUsers");
        OperationResult result = task.getResult();
        SearchResultList<PrismObject<UserType>> users;
        try {
            users = repositoryService.searchObjects(UserType.class, null, null, result);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        List<String> birthEmployeeUsersOid = new ArrayList<>();
        List<String> birthContractorUsersOid = new ArrayList<>();

        List<String> regularUsersOid = new ArrayList<>();
        List<String> semiRegularUsersOid = new ArrayList<>();
        List<String> managerUsersOid = new ArrayList<>();
        List<String> salesUsersOid = new ArrayList<>();
        List<String> securityOfficerUsersOid = new ArrayList<>();
        List<String> irregularUsersOid = new ArrayList<>();
        List<String> contractorUsersOid = new ArrayList<>();

        ItemPath itemPath = ItemPath.create(UserType.F_EXTENSION, "category");
        ItemPath itemPathBR = ItemPath.create(UserType.F_EXTENSION, "birthRole");

        for (PrismObject<UserType> user : users) {
            Item<PrismValue, ItemDefinition<?>> item = user.findItem(itemPath);
            if (item == null) {
                continue;
            }
            String string = Objects.requireNonNull(item.getRealValue()).toString();
            if (string.equals(EmployeeType.REGULAR.getStringValue())) {
                String oid = user.getOid();
                regularUsersOid.add(oid);
            } else if (string.equals(EmployeeType.SEMI_REGULAR.getStringValue())) {
                String oid = user.getOid();
                semiRegularUsersOid.add(oid);
            } else if (string.equals(EmployeeType.MANAGER.getStringValue())) {
                String oid = user.getOid();
                managerUsersOid.add(oid);
            } else if (string.equals(EmployeeType.SALES.getStringValue())) {
                String oid = user.getOid();
                salesUsersOid.add(oid);
            } else if (string.equals(EmployeeType.SECURITY_OFFICER.getStringValue())) {
                String oid = user.getOid();
                securityOfficerUsersOid.add(oid);
            } else if (string.equals(EmployeeType.IRREGULAR.getStringValue())) {
                String oid = user.getOid();
                irregularUsersOid.add(oid);
            } else if (string.equals(EmployeeType.CONTRACTOR.getStringValue())) {
                String oid = user.getOid();
                contractorUsersOid.add(oid);
            }

            Item<PrismValue, ItemDefinition<?>> item2 = user.findItem(itemPathBR);
            if (item2 == null) {
                continue;
            }

            String string2 = Objects.requireNonNull(item2.getRealValue()).toString();

            if (string2.equals(BirthRoleType.EMPLOYEE.getStringValue())) {
                String oid = user.getOid();
                birthEmployeeUsersOid.add(oid);
            } else if (string2.equals(BirthRoleType.CONTRACTOR.getStringValue())) {
                String oid = user.getOid();
                birthContractorUsersOid.add(oid);
            }
        }

        resolveBirthRoles(repositoryService, pageBase, result, birthEmployeeUsersOid, birthContractorUsersOid);
        resolveRegularUsers(repositoryService, pageBase, result, regularUsersOid);
        resolveSemiRegularUsers(repositoryService, pageBase, result, semiRegularUsersOid);
        resolveManagerUsers(repositoryService, pageBase, result, managerUsersOid);
        resolveSalesUsers(repositoryService, pageBase, result, salesUsersOid);
        resolveSecurityOfficersUsers(repositoryService, pageBase, result, securityOfficerUsersOid);
        resolveIrregularUsers(repositoryService, pageBase, result, irregularUsersOid);
        resolveContractorUsers(repositoryService, pageBase, result, contractorUsersOid);
    }

    public static void resolveBirthRoles(@NotNull RepositoryService repositoryService,
            @NotNull PageBase pageBase,
            OperationResult result,
            @NotNull List<String> birthEmployeeUsersOid, @NotNull List<String> birthContractorUsersOid) {

        String roleOid = BirthrightRole.EMPLOYEE.getStringValue();
        AssignmentType assignmentTo = createAssignment(roleOid, pageBase);
        for (String oid : birthEmployeeUsersOid) {
            List<ItemDelta<?, ?>> modifications = new ArrayList<>();
            try {
                modifications.add(PrismContext.get().deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).add(assignmentTo.clone())
                        .asItemDelta());

                repositoryService.modifyObject(UserType.class, oid, modifications, result);

            } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
                throw new RuntimeException(e);
            }

        }

        roleOid = BirthrightRole.CONTRACTOR.getStringValue();
        assignmentTo = createAssignment(roleOid, pageBase);
        for (String oid : birthContractorUsersOid) {
            List<ItemDelta<?, ?>> modifications = new ArrayList<>();
            try {
                modifications.add(PrismContext.get().deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).add(assignmentTo.clone())
                        .asItemDelta());

                repositoryService.modifyObject(UserType.class, oid, modifications, result);

            } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public static void resolveRegularUsers(@NotNull RepositoryService repositoryService,
            @NotNull PageBase pageBase,
            OperationResult result,
            @NotNull List<String> regularUsersOid) {

        for (String oid : regularUsersOid) {
            LocationBusinessRole randomLocationBusinessRole = getRandomLocationBusinessRole();
            String lbrOid = randomLocationBusinessRole.getOid();
            PolyString locale = randomLocationBusinessRole.getLocale();
            JobBusinessRole randomJobBusinessRole = getRandomJobBusinessRole();
            String jbrOid = randomJobBusinessRole.getStringValue();
            JobTitles randomlyJobTitles = getRandomlyJobTitles();

            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            try {
                modifications.add(PrismContext.get().deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).add(createAssignment(lbrOid, pageBase)).asItemDelta());

                modifications.add(PrismContext.get().deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).add(createAssignment(jbrOid, pageBase)).asItemDelta());

                modifications.add(PrismContext.get().deltaFor(UserType.class)
                        .item(UserType.F_LOCALITY).replace(locale).asItemDelta());

                modifications.add(PrismContext.get().deltaFor(UserType.class)
                        .item(UserType.F_TITLE).replace(PolyString.fromOrig(randomlyJobTitles.getStringValue())).asItemDelta());

                repositoryService.modifyObject(UserType.class, oid, modifications, result);

            } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public static void resolveSemiRegularUsers(@NotNull RepositoryService repositoryService,
            @NotNull PageBase pageBase,
            OperationResult result,
            @NotNull List<String> semiRegularUsersOid) {

        int size = semiRegularUsersOid.size();

        int ninetyPercent = (int) (size * 0.9);

        for (int i = 0; i < semiRegularUsersOid.size(); i++) {
            String oid = semiRegularUsersOid.get(i);
            LocationBusinessRole randomLocationBusinessRole = getRandomLocationBusinessRole();
            String lbrOid = randomLocationBusinessRole.getOid();
            PolyString locale = randomLocationBusinessRole.getLocale();
            JobTitles randomlyJobTitles = getRandomlyJobTitlesWithNone();

            List<PlanktonApplicationRole> randomPlanktonRoles = getRandomPlanktonRoles();

            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            try {
                modifications.add(PrismContext.get().deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).add(createAssignment(lbrOid, pageBase)).asItemDelta());

                for (PlanktonApplicationRole randomPlanktonRole : randomPlanktonRoles) {
                    modifications.add(PrismContext.get().deltaFor(UserType.class)
                            .item(UserType.F_ASSIGNMENT).add(createAssignment(randomPlanktonRole.getStringValue(), pageBase))
                            .asItemDelta());
                }
                if (i < ninetyPercent) {
                    modifications.add(PrismContext.get().deltaFor(UserType.class)
                            .item(UserType.F_LOCALITY).replace(locale).asItemDelta());
                }

                if (!randomlyJobTitles.equals(JobTitles.NONE)) {
                    modifications.add(PrismContext.get().deltaFor(UserType.class)
                            .item(UserType.F_TITLE).replace(PolyString.fromOrig(randomlyJobTitles.getStringValue())).asItemDelta());
                }

                repositoryService.modifyObject(UserType.class, oid, modifications, result);

            } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public static void resolveManagerUsers(@NotNull RepositoryService repositoryService,
            @NotNull PageBase pageBase,
            OperationResult result,
            @NotNull List<String> managerUsersOid) {

        int size = managerUsersOid.size();

        int ninetyPercent = (int) (size * 0.9);

        for (int i = 0; i < managerUsersOid.size(); i++) {
            JobBusinessRole managerRole = JobBusinessRole.MANAGER;
            String oid = managerUsersOid.get(i);
            LocationBusinessRole randomLocationBusinessRole = getRandomLocationBusinessRole();
            String lbrOid = randomLocationBusinessRole.getOid();
            PolyString locale = randomLocationBusinessRole.getLocale();
            String jobTitle = "manager";

            List<PlanktonApplicationRole> randomPlanktonRoles = getRandomPlanktonRoles();

            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            try {

                modifications.add(PrismContext.get().deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).add(createAssignment(managerRole.getStringValue(), pageBase)).asItemDelta());

                modifications.add(PrismContext.get().deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).add(createAssignment(lbrOid, pageBase)).asItemDelta());

                for (PlanktonApplicationRole randomPlanktonRole : randomPlanktonRoles) {
                    modifications.add(PrismContext.get().deltaFor(UserType.class)
                            .item(UserType.F_ASSIGNMENT).add(createAssignment(randomPlanktonRole.getStringValue(), pageBase))
                            .asItemDelta());
                }
                if (i < ninetyPercent) {
                    modifications.add(PrismContext.get().deltaFor(UserType.class)
                            .item(UserType.F_LOCALITY).replace(locale).asItemDelta());
                }

                modifications.add(PrismContext.get().deltaFor(UserType.class)
                        .item(UserType.F_TITLE).replace(PolyString.fromOrig(jobTitle)).asItemDelta());

                repositoryService.modifyObject(UserType.class, oid, modifications, result);

            } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public static void resolveContractorUsers(@NotNull RepositoryService repositoryService,
            @NotNull PageBase pageBase,
            OperationResult result,
            @NotNull List<String> contractorUsersOid) {

        //oid
        for (String oid : contractorUsersOid) {

            //already has contractor role from birth role resolver

            List<PlanktonApplicationRole> randomPlanktonRoles = getRandomPlanktonRoles();

            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            try {

                for (PlanktonApplicationRole randomPlanktonRole : randomPlanktonRoles) {
                    modifications.add(PrismContext.get().deltaFor(UserType.class)
                            .item(UserType.F_ASSIGNMENT).add(createAssignment(randomPlanktonRole.getStringValue(), pageBase)).asItemDelta());
                }

                repositoryService.modifyObject(UserType.class, oid, modifications, result);

            } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public static void resolveSecurityOfficersUsers(@NotNull RepositoryService repositoryService,
            @NotNull PageBase pageBase,
            OperationResult result,
            @NotNull List<String> securityOfficerUsersOid) {

        for (String oid : securityOfficerUsersOid) {

            JobBusinessRole securityOfficerRole = JobBusinessRole.SECURITY_OFFICER;
            String securityOfficerRoleStringValue = securityOfficerRole.getStringValue();

            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            try {
                modifications.add(PrismContext.get().deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).add(createAssignment(securityOfficerRoleStringValue, pageBase)).asItemDelta());

                repositoryService.modifyObject(UserType.class, oid, modifications, result);

            } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public static void resolveIrregularUsers(@NotNull RepositoryService repositoryService,
            @NotNull PageBase pageBase,
            OperationResult result,
            @NotNull List<String> irregularUsersOid) {

        for (String oid : irregularUsersOid) {
            JobTitles randomlyJobTitlesWithNone = getRandomlyJobTitlesWithNone();
            List<ItemDelta<?, ?>> modifications = new ArrayList<>();
            List<PlanktonApplicationRole> randomPlanktonRoles = getLargeRandomPlanktonRoles();

            try {
                for (PlanktonApplicationRole randomPlanktonRole : randomPlanktonRoles) {
                    modifications.add(PrismContext.get().deltaFor(UserType.class)
                            .item(UserType.F_ASSIGNMENT).add(createAssignment(randomPlanktonRole.getStringValue(), pageBase))
                            .asItemDelta());
                }

                if (!randomlyJobTitlesWithNone.equals(JobTitles.NONE)) {
                    modifications.add(PrismContext.get().deltaFor(UserType.class)
                            .item(UserType.F_TITLE).replace(PolyString.fromOrig(randomlyJobTitlesWithNone.getStringValue()))
                            .asItemDelta());
                }
                repositoryService.modifyObject(UserType.class, oid, modifications, result);

            } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public static void resolveSalesUsers(@NotNull RepositoryService repositoryService,
            @NotNull PageBase pageBase,
            OperationResult result,
            @NotNull List<String> salesUsersOid) {

        JobBusinessRole salesRole = JobBusinessRole.SALES;
        LocationBusinessRole newYorkLocation = LocationBusinessRole.NEW_YORK;

        int size = salesUsersOid.size();

        int ninetyPercent = (int) (size * 0.9);

        int seventyPercent = (int) (size * 0.7);

        for (int i = 0; i < salesUsersOid.size(); i++) {
            String oid = salesUsersOid.get(i);
            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            LocationBusinessRole randomLocationBusinessRole = getRandomLocationBusinessRole();

            try {

                if (i < ninetyPercent) {

                    modifications.add(PrismContext.get().deltaFor(UserType.class)
                            .item(UserType.F_TITLE).replace(PolyString.fromOrig("salesperson")).asItemDelta());

                    if (i < seventyPercent) {

                        modifications.add(PrismContext.get().deltaFor(UserType.class)
                                .item(UserType.F_ASSIGNMENT).add(createAssignment(newYorkLocation.getOid(), pageBase))
                                .asItemDelta());

                        modifications.add(PrismContext.get().deltaFor(UserType.class)
                                .item(UserType.F_LOCALITY).replace(newYorkLocation.getLocale()).asItemDelta());

                    } else {
                        modifications.add(PrismContext.get().deltaFor(UserType.class)
                                .item(UserType.F_ASSIGNMENT).add(createAssignment(randomLocationBusinessRole.getOid(), pageBase))
                                .asItemDelta());
                        modifications.add(PrismContext.get().deltaFor(UserType.class)
                                .item(UserType.F_LOCALITY).replace(randomLocationBusinessRole.getLocale()).asItemDelta());
                    }

                }

                modifications.add(PrismContext.get().deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).add(createAssignment(salesRole.getStringValue(), pageBase))
                        .asItemDelta());

                repositoryService.modifyObject(UserType.class, oid, modifications, result);

            } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
                throw new RuntimeException(e);
            }

        }
    }

    private static AssignmentType createAssignment(String oid, PageBase pageBase) {
        if (oid == null || oid.isEmpty()) {
            return null;
        }
        ObjectTypes role = ObjectTypes.ROLE;
        if (pageBase == null) {
            return null;
        }
        PrismContext prismContext = pageBase.getPrismContext();
        if (prismContext == null) {
            return null;
        }
        return createAssignmentTo(oid, role, prismContext).clone();
    }

}
