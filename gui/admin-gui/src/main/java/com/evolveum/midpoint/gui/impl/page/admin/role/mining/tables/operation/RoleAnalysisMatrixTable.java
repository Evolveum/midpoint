/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.operation;

public class RoleAnalysisMatrixTable { //<B extends MiningBaseTypeChunk, A extends MiningBaseTypeChunk> extends BasePanel<PrismObject<RoleAnalysisClusterType>> {

//    private static final String ID_DATATABLE = "datatable";
//    private static final String DOT_CLASS = RoleAnalysisMatrixTable.class.getName() + ".";
//    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";
//    private static final String OP_PROCESS_CANDIDATE_ROLE = DOT_CLASS + "processCandidate";
//    public static final String PARAM_DETECTED_PATER_ID = "detectedPatternId";
//    public static final String PARAM_TABLE_SETTING = "tableSetting";
//
//
////    private String valueTitle = null;
////    private int currentPageView = 0;
////    private int columnPageCount = 100;
////    private int fromCol = 1;
////    private int specialColumnCount;
////    boolean isRelationSelected = false;
//    boolean showAsExpandCard = false;
//
////    private final LoadableModel<MiningOperationChunk> miningOperationChunk;
//    private final LoadableDetachableModel<OperationPanelModel> operationPanelModel;
//
//    private final LoadableDetachableModel<DisplayValueOption> displayValueOptionModel;
//
//        //TODO new:
//
//
//    public RoleAnalysisMatrixTable(
//            @NotNull String id,
//            @NotNull LoadableDetachableModel<DisplayValueOption> displayValueOptionModel,
//            @NotNull IModel<PrismObject<RoleAnalysisClusterType>> cluster,
//            boolean isRoleMode) {
//        super(id, cluster);
//
//        this.displayValueOptionModel = displayValueOptionModel;
////        this.isRoleMode = isRoleMode;
//
//        this.operationPanelModel = new LoadableDetachableModel<>() {
//            @Override
//            protected OperationPanelModel load() {
//                OperationPanelModel model = new OperationPanelModel();
//                model.createDetectedPatternModel(getClusterPatterns());
//                model.createCandidatesRolesRoleModel(getClusterCandidateRoles());
//
//                Task task = getPageBase().createSimpleTask("Prepare mining structure");
//                OperationResult result = task.getResult();
//
//                List<DetectedPattern> patternsToAnalyze = loadPatternsToAnalyze(task, result);
//                model.addSelectedPattern(patternsToAnalyze);
//                return model;
//            }
//        };
//
////        this.miningOperationChunk = new LoadableModel<>(false) {
////
////            @Override
////            protected MiningOperationChunk load() {
////                Task task = getPageBase().createSimpleTask("Prepare mining structure");
////                OperationResult result = task.getResult();
////
////                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
////
////                List<DetectedPattern> defaultDisplayedPatterns = operationPanelModel.getObject().getSelectedPatterns();
////                //chunk mode
////                MiningOperationChunk chunk = roleAnalysisService.prepareMiningStructure(
////                        getModelObject().asObjectable(),
////                        displayValueOptionModel.getObject(),
////                        isRoleMode ? RoleAnalysisProcessModeType.ROLE : RoleAnalysisProcessModeType.USER,
////                        defaultDisplayedPatterns,
////                        result, task);
////
////                return chunk;
////            }
////        };
//
//    }
//
//    private List<DetectedPattern> loadPatternsToAnalyze(Task task, OperationResult result) {
//        if (getCandidateRoleContainerId() != null) {
//            return analysePattersForCandidateRole(task, result);
//        }
//
//        return loadDetectedPattern();
//    }
//
//    @Override
//    protected void onInitialize() {
//        super.onInitialize();
//        initLayout();
//    }
//
//
//        private void initLayout() {
//            RoleAnalysisTable<B, A> table = generateTable();
//            add(table);
//
//            initOperationPanel();
//        }
//
//    private void initOperationPanel() {
//        RoleAnalysisTableOpPanelItemPanel itemPanel = new RoleAnalysisTableOpPanelItemPanel("panel", operationPanelModel) {
//
//            @Override
//            public void onPatternSelectionPerform(@NotNull AjaxRequestTarget ajaxRequestTarget) {
//                loadDetectedPattern(ajaxRequestTarget);
//            }
//
//            @Override
//            protected void initHeaderItem(@NotNull RepeatingView headerItems) {
//                RoleAnalysisTableOpPanelItem refreshIcon = new RoleAnalysisTableOpPanelItem(
//                        headerItems.newChildId(), getModel()) {
//
//                    @Serial private static final long serialVersionUID = 1L;
//
//                    @Contract(pure = true)
//                    @Override
//                    public @NotNull String appendIconPanelCssClass() {
//                        return "bg-white";
//                    }
//
//                    @Override
//                    protected void performOnClick(AjaxRequestTarget target) {
//                        showAsExpandCard = !showAsExpandCard;
//                        toggleDetailsNavigationPanelVisibility(target);
//                    }
//
//                    @Contract(pure = true)
//                    @Override
//                    public @NotNull String replaceIconCssClass() {
//                        if (showAsExpandCard) {
//                            return "fa-2x fa fa-compress text-dark";
//                        }
//                        return "fa-2x fas fa-expand text-dark";
//                    }
//
//                    @Override
//                    public @NotNull Component getDescriptionTitleComponent(String id) {
//                        Label label = new Label(id, "Table view"); //TODO string resource model
//                        label.setOutputMarkupId(true);
//                        return label;
//                    }
//
//                    @Override
//                    protected void addDescriptionComponents() {
//                        appendText("Switch table view", null); //TODO string resource model
//                    }
//                };
//                refreshIcon.add(AttributeAppender.replace("class", "btn btn-outline-dark border-0 d-flex"
//                        + " align-self-stretch mt-1"));
//                headerItems.add(refreshIcon);
//            }
//
//        };
//        itemPanel.setOutputMarkupId(true);
//        add(itemPanel);
//    }
//
//
//
//        public  RoleAnalysisTable<B, A> generateTable() {
//
////            RoleMiningProvider<A> provider = createRoleMiningProvider();
//
//
//            RoleAnalysisTable<B, A> table = new RoleAnalysisTable<>(
//                    ID_DATATABLE,
//                    getModel(),
//                    operationPanelModel,
//                    true) {
//
////                @Override
////                public String getAdditionalBoxCssClasses() {
////                    return " m-0";
////                }
//
////                @Override
////                protected int getColumnCount() {
////                    return miningOperationChunk.getObject().getMainMiningChunk().size();
////                } //TODO special column size
//
////                @Override
////                protected void resetTable(AjaxRequestTarget target) {
////                    miningOperationChunk.reset();
////                    RoleAnalysisMatrixTable.this.refreshTable(target);
////                }
//
////                @Override
////                protected void refreshTable(AjaxRequestTarget target) {
////                    RoleAnalysisMatrixTable.this.refreshTable(target);
////                }
//
//                @Override
//                protected @Nullable Set<RoleAnalysisCandidateRoleType> getCandidateRoleContainer() {
//                    return getCandidateRoleToPerform(RoleAnalysisMatrixTable.this.getModelObject().asObjectable());
//                }
//
//                @Override
//                protected boolean getMigrationButtonVisibility() {
//                    Set<RoleAnalysisCandidateRoleType> candidateRole = getCandidateRole();
//                    if (candidateRole != null) {
//                        if (candidateRole.size() > 1) {
//                            return false;
//                        }
//                    }
//                    if (getSelectedPatterns().size() > 1) {
//                        return false;
//                    }
//
//                    return isRelationSelected;
//                }
//
//                @Override
//                protected void onSubmitEditButton(AjaxRequestTarget target) {
//                    onSubmitCandidateRolePerform(target, RoleAnalysisMatrixTable.this.getModelObject());
//                }
//
//            };
//            table.setItemsPerPage(50);
//            table.setOutputMarkupId(true);
//
//            return table;
//        }
//
//
////        protected RoleAnalysisTable<?> getTable() {
////            return ((RoleAnalysisTable<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE)));
////        }
//
//        private void navigateToClusterCandidateRolePanel(@NotNull PrismObject<RoleAnalysisClusterType> cluster) {
//            PageParameters parameters = new PageParameters();
//            String clusterOid = cluster.getOid();
//            parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
//            parameters.add("panelId", "candidateRoles");
//            Class<? extends PageBase> detailsPageClass = DetailsPageUtil
//                    .getObjectDetailsPage(RoleAnalysisClusterType.class);
//            getPageBase().navigateToNext(detailsPageClass, parameters);
//        }
//
////        private @Nullable Set<RoleAnalysisCandidateRoleType> getCandidateRole() {
////            List<String> candidateRoleContainerId = getCandidateRoleContainerId();
////
////            Set<RoleAnalysisCandidateRoleType> candidateRoleTypes = new HashSet<>();
////            if (candidateRoleContainerId != null && !candidateRoleContainerId.isEmpty()) {
////                RoleAnalysisClusterType clusterType = getModelObject().asObjectable();
////                List<RoleAnalysisCandidateRoleType> candidateRoles = clusterType.getCandidateRoles();
////
////                for (RoleAnalysisCandidateRoleType candidateRole : candidateRoles) {
////                    if (candidateRoleContainerId.contains(candidateRole.getId().toString())) {
////                        candidateRoleTypes.add(candidateRole);
////                    }
////                }
////                if (!candidateRoleTypes.isEmpty()) {
////                    return candidateRoleTypes;
////                }
////                return null;
////            }
////            return null;
////
////        }
//
//
//        private <F extends FocusType, CH extends MiningBaseTypeChunk> void fillCandidateList(Class<F> type,
//                Set<PrismObject<F>> candidateList,
//                List<CH> miningSimpleChunk,
//                Task task,
//                OperationResult result) {
//            for (CH roleChunk : miningSimpleChunk) {
//                if (roleChunk.getStatus().equals(RoleAnalysisOperationMode.INCLUDE)) {
//                    for (String roleOid : roleChunk.getRoles()) {
//                        PrismObject<F> roleObject = WebModelServiceUtils.loadObject(type, roleOid, getPageBase(), task, result);
//                        if (roleObject != null) {
//                            candidateList.add(roleObject);
//                        }
//                    }
//                }
//            }
//        }
//
//    private void onSubmitCandidateRolePerform(@NotNull AjaxRequestTarget target,
//                @NotNull PrismObject<RoleAnalysisClusterType> cluster) {
//        if (miningOperationChunk == null) {
//            warn(createStringResource("RoleAnalysis.candidate.not.selected").getString());
//            target.add(getPageBase().getFeedbackPanel());
//            return;
//        }
//
//        Task task = getPageBase().createSimpleTask(OP_PROCESS_CANDIDATE_ROLE);
//        OperationResult result = task.getResult();
//
//        MiningOperationChunk chunk = miningOperationChunk.getObject();
//
//        Set<PrismObject<RoleType>> candidateInducements = new HashSet<>();
//        fillCandidateList(RoleType.class, candidateInducements, chunk.getSimpleMiningRoleTypeChunks(), task, result);
//
//        Set<PrismObject<UserType>> candidateMembers = new HashSet<>();
//        fillCandidateList(UserType.class, candidateMembers, chunk.getSimpleMiningUserTypeChunks(), task, result);
//
//        Set<RoleAnalysisCandidateRoleType> candidateRoleToPerform = getCandidateRoleToPerform(cluster.asObjectable());
//        if (candidateRoleToPerform != null) {
//            @Nullable List<RoleAnalysisCandidateRoleType> candidateRole = new ArrayList<>(candidateRoleToPerform);
//            if (candidateRole.size() == 1) {
//                PageBase pageBase = getPageBase();
//                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
//
//                Set<AssignmentType> assignmentTypeSet = candidateInducements.stream()
//                        .map(candidateInducement -> ObjectTypeUtil.createAssignmentTo(candidateInducement.getOid(), ObjectTypes.ROLE))
//                        .collect(Collectors.toSet());
//
//                executeChangesOnCandidateRole(roleAnalysisService, pageBase, target,
//                        cluster,
//                        candidateRole,
//                        candidateMembers,
//                        assignmentTypeSet,
//                        task,
//                        result
//                );
//
//                result.computeStatus();
//                getPageBase().showResult(result);
//                navigateToClusterCandidateRolePanel(cluster);
//                return;
//            }
//        }
//
//        PrismObject<RoleType> businessRole = new RoleType().asPrismObject();
//
//        List<BusinessRoleDto> roleApplicationDtos = new ArrayList<>();
//
//        for (PrismObject<UserType> member : candidateMembers) {
//            BusinessRoleDto businessRoleDto = new BusinessRoleDto(member,
//                    businessRole, candidateInducements, getPageBase());
//            roleApplicationDtos.add(businessRoleDto);
//        }
//
//        BusinessRoleApplicationDto operationData = new BusinessRoleApplicationDto(
//                cluster, businessRole, roleApplicationDtos, candidateInducements);
//
//        if (!getSelectedPatterns().isEmpty() && getSelectedPatterns().get(0).getId() != null) {
//            operationData.setPatternId(getSelectedPatterns().get(0).getId());
//        }
//
//        List<BusinessRoleDto> businessRoleDtos = operationData.getBusinessRoleDtos();
//        Set<PrismObject<RoleType>> inducement = operationData.getCandidateRoles();
//        if (!inducement.isEmpty() && !businessRoleDtos.isEmpty()) {
//            PageRole pageRole = new PageRole(operationData.getBusinessRole(), operationData);
//            setResponsePage(pageRole);
//        } else {
//            warn(createStringResource("RoleAnalysis.candidate.not.selected").getString());
//            target.add(getPageBase().getFeedbackPanel());
//        }
//    }
//
//
//
//        public boolean isOutlierDetection() {
//            return false;
//        }
//
//        private List<DetectedPattern> getClusterPatterns() {
//            RoleAnalysisClusterType clusterType = getModelObject().asObjectable();
//            return transformDefaultPattern(clusterType);
//        }
//
//
//        @SuppressWarnings("rawtypes")
//        protected void toggleDetailsNavigationPanelVisibility(AjaxRequestTarget target) {
//            Page page = getPage();
//            if (page instanceof AbstractPageObjectDetails) {
//                AbstractPageObjectDetails<?, ?> pageObjectDetails = ((AbstractPageObjectDetails) page);
//                pageObjectDetails.toggleDetailsNavigationPanelVisibility(target);
//            }
//        }
//
//        public List<DetectedPattern> getSelectedPatterns() {
//            return operationPanelModel.getObject().getSelectedPatterns();
//        }
//
//        private @Nullable Set<RoleAnalysisCandidateRoleType> getCandidateRoleToPerform(RoleAnalysisClusterType cluster) {
//            if (getSelectedPatterns().size() > 1) {
//                return null;
//            } else if (getSelectedPatterns().size() == 1) {
//                DetectedPattern detectedPattern = getSelectedPatterns().get(0);
//                Long id = detectedPattern.getId();
//                List<RoleAnalysisCandidateRoleType> candidateRoles = cluster.getCandidateRoles();
//                for (RoleAnalysisCandidateRoleType candidateRole : candidateRoles) {
//                    if (candidateRole.getId().equals(id)) {
//                        return Collections.singleton(candidateRole);
//                    }
//                }
//            }
//
//            return getCandidateRole();
//        }
//
//        protected void refreshTable(AjaxRequestTarget target) {
////            var columns = RoleAnalysisMatrixTable.this.initColumns();
////            RoleAnalysisTable<?> table = RoleAnalysisMatrixTable.this.getTable();
////            table.getDataTable().getColumns().clear();
////            table.getDataTable().getColumns().addAll((List) columns);
////            target.add(RoleAnalysisMatrixTable.this);
//        }
//
//
//    private List<DetectedPattern> analysePattersForCandidateRole(Task task, OperationResult result) {
//        RoleAnalysisClusterType cluster = getModelObject().asObjectable();
//        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
//        return roleAnalysisService.findDetectedPatterns(cluster, getCandidateRoleContainerId(), task, result);
//    }
//
//        private List<DetectedPattern> loadDetectedPattern() {
//            RoleAnalysisClusterType cluster = getModelObject().asObjectable();
//            List<RoleAnalysisDetectionPatternType> detectedPattern = cluster.getDetectedPattern();
//
//            for (RoleAnalysisDetectionPatternType pattern : detectedPattern) {
//                Long id = pattern.getId();
//                if (id.equals(getDetectedPatternContainerId())) {
//                    return Collections.singletonList(transformPattern(pattern));
//                }
//            }
//            return new ArrayList<>();
//        }
//
//
//    public Long getDetectedPatternContainerId() {
//        StringValue stringValue = getPageBase().getPageParameters().get(PARAM_DETECTED_PATER_ID);
//        if (!stringValue.isNull()) {
//            return Long.valueOf(stringValue.toString());
//        }
//        return null;
//    }
//
////    public List<String> getCandidateRoleContainerId() {
////        StringValue stringValue = getPageBase().getPageParameters().get(PARAM_CANDIDATE_ROLE_ID);
////        if (!stringValue.isNull()) {
////            String[] split = stringValue.toString().split(",");
////            return Arrays.asList(split);
////        }
////        return null;
////    }
//
//
//    private List<DetectedPattern> getClusterCandidateRoles() {
//        RoleAnalysisClusterType clusterType = getModelObject().asObjectable();
//        return loadAllCandidateRoles(clusterType);
//    }
//
//    private @NotNull List<DetectedPattern> loadAllCandidateRoles(@NotNull RoleAnalysisClusterType cluster) {
//        List<RoleAnalysisCandidateRoleType> clusterCandidateRoles = cluster.getCandidateRoles();
//        List<DetectedPattern> candidateRoles = new ArrayList<>();
//        Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS); //TODO task name?
//        OperationResult result = task.getResult();
//        for (RoleAnalysisCandidateRoleType candidateRole : clusterCandidateRoles) {
//
//            RoleAnalysisOperationStatus operationStatus = candidateRole.getOperationStatus();
//            boolean isMigrated = operationStatus != null
//                    && operationStatus.getOperationChannel() != null
//                    && operationStatus.getOperationChannel().equals(RoleAnalysisOperation.MIGRATION);
//
//            if (isMigrated) {
//                continue;
//            }
//
//
//            String roleOid = candidateRole.getCandidateRoleRef().getOid();
//            //TODO does it make sense to create subresult for each iteration?
//            PrismObject<RoleType> rolePrismObject = getPageBase().getRoleAnalysisService().getRoleTypeObject(
//                    roleOid, task, result);
//            List<String> rolesOidInducements;
//            if (rolePrismObject == null) {
//                return new ArrayList<>();
//            }
//
//            //TODO what is this?
//            rolesOidInducements = getRolesOidInducements(rolePrismObject);
//            List<String> rolesOidAssignment = getRolesOidAssignment(rolePrismObject.asObjectable());
//
//            Set<String> accessOidSet = new HashSet<>(rolesOidInducements);
//            accessOidSet.addAll(rolesOidAssignment);
//
//            ListMultimap<String, String> mappedMembers = getPageBase().getRoleAnalysisService().extractUserTypeMembers(new HashMap<>(),
//                    null,
//                    Collections.singleton(roleOid),
//                    getPageBase().createSimpleTask(OP_PREPARE_OBJECTS),
//                    result);
//
//            List<ObjectReferenceType> candidateMembers = candidateRole.getCandidateMembers();
//            Set<String> membersOidSet = new HashSet<>();
//            for (ObjectReferenceType candidateMember : candidateMembers) {
//                String oid = candidateMember.getOid();
//                if (oid != null) {
//                    membersOidSet.add(oid);
//                }
//            }
//
//            membersOidSet.addAll(mappedMembers.get(roleOid));
//            double clusterMetric = (accessOidSet.size() * membersOidSet.size()) - membersOidSet.size();
//
//            DetectedPattern pattern = new DetectedPattern(
//                    accessOidSet,
//                    membersOidSet,
//                    clusterMetric,
//                    null,
//                    roleOid);
//            pattern.setIdentifier(rolePrismObject.getName().getOrig());
//            pattern.setId(candidateRole.getId());
//            pattern.setClusterRef(new ObjectReferenceType().oid(cluster.getOid()).type(RoleAnalysisClusterType.COMPLEX_TYPE));
//
//            candidateRoles.add(pattern);
//        }
//        return candidateRoles;
//    }
//
//    //TODO B as base chung, A as additional chunk
//
//
//
//    private boolean isPatternDetected() {
//        return !getSelectedPatterns().isEmpty();
//    }

}

