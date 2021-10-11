/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyEnumValuesModel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.AccessCertificationService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterEntry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCampaignListItemDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCampaignListItemDtoProvider;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCampaignStateFilter;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCampaignsSearchDto;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.session.CertCampaignsStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/certification/campaigns", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL, label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL, description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_CAMPAIGNS, label = PageAdminCertification.AUTH_CERTIFICATION_CAMPAIGNS_LABEL, description = PageAdminCertification.AUTH_CERTIFICATION_CAMPAIGNS_DESCRIPTION) })

public class PageCertCampaigns extends PageAdminCertification {

    private static final Trace LOGGER = TraceManager.getTrace(PageCertCampaigns.class);

    private static final String DOT_CLASS = PageCertCampaigns.class.getName() + ".";
    private static final String OPERATION_DELETE_CAMPAIGNS = DOT_CLASS + "deleteCampaigns";
    private static final String OPERATION_OPEN_NEXT_STAGE = DOT_CLASS + "openNextStage";
    private static final String OPERATION_CLOSE_STAGE = DOT_CLASS + "closeStage";
    private static final String OPERATION_CLOSE_CAMPAIGN = DOT_CLASS + "closeCampaign";
    private static final String OPERATION_START_CAMPAIGN = DOT_CLASS + "startCampaign";
    private static final String OPERATION_START_REMEDIATION = DOT_CLASS + "startRemediation";
    private static final String OPERATION_REITERATE_CAMPAIGN = DOT_CLASS + "reiterateCampaign";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_CAMPAIGNS_TABLE = "campaignsTable";
    static final String OP_START_CAMPAIGN = "PageCertCampaigns.button.startCampaign";
    static final String OP_CLOSE_CAMPAIGN = "PageCertCampaigns.button.closeCampaign";
    static final String OP_CLOSE_STAGE = "PageCertCampaigns.button.closeStage";
    static final String OP_OPEN_NEXT_STAGE = "PageCertCampaigns.button.openNextStage";
    static final String OP_START_REMEDIATION = "PageCertCampaigns.button.startRemediation";

    private static final String ID_TABLE_HEADER = "tableHeader";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH_STATE = "state";
    private static final String ID_SEARCH_CLEAR = "searchClear";


    // campaign on which close-stage/close-campaign/delete/reiterate has to be executed
    // (if chosen directly from row menu)
    private CertCampaignListItemDto relevantCampaign;
    private String definitionOid;

    private IModel<CertCampaignsSearchDto> searchModel;

    public PageCertCampaigns(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        definitionOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER).toString();
        searchModel = LoadableModel.create(this::loadSearchDto, false);
        initLayout();
    }

    private CertCampaignsSearchDto loadSearchDto() {
        CertCampaignsStorage storage = getSessionStorage().getCertCampaigns();
        CertCampaignsSearchDto dto = storage.getCampaignsSearch();
        return dto != null ? dto : new CertCampaignsSearchDto();
    }

    // region Data management
    private CertCampaignListItemDtoProvider createProvider() {
        CertCampaignListItemDtoProvider provider = new CertCampaignListItemDtoProvider(this) {
            @Override
            public CertCampaignListItemDto createDataObjectWrapper(
                    PrismObject<AccessCertificationCampaignType> obj) {
                CertCampaignListItemDto dto = super.createDataObjectWrapper(obj);
                createInlineMenuForItem(dto);
                return dto;
            }
        };
        provider.setOptions(null);
        return provider;
    }
    // endregion

    // region Layout

    @Override
    protected IModel<String> createPageTitleModel() {
        if (definitionOid == null) {
            return super.createPageTitleModel();
        } else {
            return new IModel<String>() {

                @Override
                public String getObject() {
                    Task task = createSimpleTask("dummy");
                    PrismObject<AccessCertificationDefinitionType> definitionPrismObject = WebModelServiceUtils
                            .loadObject(AccessCertificationDefinitionType.class, definitionOid,
                                    PageCertCampaigns.this, task, task.getResult());

                    String name = definitionPrismObject == null ? ""
                            : WebComponentUtil.getName(definitionPrismObject);

                    return createStringResource("PageCertCampaigns.title", name).getString();
                }
            };
        }
    }

    private void initLayout() {
        Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
        add(mainForm);

        CertCampaignListItemDtoProvider provider = createProvider();
        provider.setQuery(createCampaignsQuery());
        int itemsPerPage = (int) getItemsPerPage(UserProfileStorage.TableId.PAGE_CERT_CAMPAIGNS_PANEL);
        BoxedTablePanel<CertCampaignListItemDto> table = new BoxedTablePanel<CertCampaignListItemDto>(ID_CAMPAIGNS_TABLE, provider,
                initColumns(), UserProfileStorage.TableId.PAGE_CERT_CAMPAIGNS_PANEL, itemsPerPage) {
            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                return new SearchFragment(headerId, ID_TABLE_HEADER, PageCertCampaigns.this, searchModel);
            }
        };
        table.setShowPaging(true);
        table.setOutputMarkupId(true);
        table.setItemsPerPage(itemsPerPage);
        mainForm.add(table);
    }

    private static class SearchFragment extends Fragment {

        SearchFragment(String id, String markupId, MarkupContainer markupProvider, IModel<CertCampaignsSearchDto> model) {
            super(id, markupId, markupProvider, model);
            initLayout();
        }

        private void initLayout() {
            final Form searchForm = new com.evolveum.midpoint.web.component.form.Form(ID_SEARCH_FORM);
            add(searchForm);
            searchForm.setOutputMarkupId(true);

            //noinspection unchecked
            final IModel<CertCampaignsSearchDto> searchModel = (IModel<CertCampaignsSearchDto>) getDefaultModel();

            DropDownChoicePanel<CertCampaignStateFilter> listSelect = new DropDownChoicePanel<>(ID_SEARCH_STATE,
                    new PropertyModel<>(searchModel, CertCampaignsSearchDto.F_STATE_FILTER),
                    new ReadOnlyEnumValuesModel<>(CertCampaignStateFilter.class),
                    new EnumChoiceRenderer<>(this));
            listSelect.getBaseFormComponent().add(createFilterAjaxBehaviour());
            listSelect.setOutputMarkupId(true);
            searchForm.add(listSelect);

            AjaxSubmitButton clearButton = new AjaxSubmitButton(ID_SEARCH_CLEAR) {

                @Override
                protected void onSubmit(AjaxRequestTarget target) {
                    clearSearchPerformed(target);
                }

                @Override
                protected void onError(AjaxRequestTarget target) {
                    target.add(((PageBase) getPage()).getFeedbackPanel());
                }

                private void clearSearchPerformed(AjaxRequestTarget target) {
                    PageCertCampaigns page = (PageCertCampaigns) getPage();
                    CertCampaignsSearchDto cleanSearchDto = new CertCampaignsSearchDto();
                    searchModel.setObject(cleanSearchDto);
                    CertCampaignsStorage storage = page.getSessionStorage().getCertCampaigns();
                    storage.setCampaignsSearch(cleanSearchDto);

                    Table panel = page.getCampaignsTable();
                    DataTable table = panel.getDataTable();
                    CertCampaignListItemDtoProvider provider = (CertCampaignListItemDtoProvider) table.getDataProvider();
                    provider.setQuery(page.createCampaignsQuery());
                    panel.setCurrentPage(storage.getPaging());

                    target.add((Component) panel);
                }
            };
            searchForm.add(clearButton);
        }

        private AjaxFormComponentUpdatingBehavior createFilterAjaxBehaviour() {
            return new AjaxFormComponentUpdatingBehavior("change") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    ((PageCertCampaigns) getPage()).searchFilterPerformed(target);
                }
            };
        }
    }

    private void searchFilterPerformed(AjaxRequestTarget target) {
        CertCampaignsSearchDto dto = searchModel.getObject();

        Table panel = getCampaignsTable();
        DataTable table = panel.getDataTable();
        CertCampaignListItemDtoProvider provider = (CertCampaignListItemDtoProvider) table.getDataProvider();
        provider.setQuery(createCampaignsQuery());
        table.setCurrentPage(0);

        CertCampaignsStorage storage = getSessionStorage().getCertCampaigns();
        storage.setCampaignsSearch(dto);

        target.add(getFeedbackPanel());
        target.add((Component) getCampaignsTable());
    }

    private ObjectQuery createCampaignsQuery() {
        S_AtomicFilterEntry q = getPrismContext().queryFor(AccessCertificationCampaignType.class);
        if (definitionOid != null) {
            q = q.item(AccessCertificationCampaignType.F_DEFINITION_REF).ref(definitionOid).and();
        }
        CertCampaignsSearchDto dto = searchModel.getObject();
        q = dto.getStateFilter().appendFilter(q);
        return q.all().build();
    }

    private IModel<String> createCloseStageConfirmString() {
        return new IModel<String>() {

            @Override
            public String getObject() {

                return createStringResource("PageCertCampaigns.message.closeStageConfirmSingle",
                        relevantCampaign.getName()).getString();
            }
        };
    }

    private IModel<String> createCloseCampaignConfirmString() {
        return new ReadOnlyModel<>(() ->
                createStringResource("PageCertCampaigns.message.closeCampaignConfirmSingle",
                        relevantCampaign.getName()).getString());
    }

    private IModel<String> createReiterateCampaignConfirmString() {
        return new ReadOnlyModel<>(() ->
                createStringResource("PageCertCampaigns.message.reiterateCampaignConfirmSingle",
                        relevantCampaign.getName()).getString());
    }

    private IModel<String> createCloseSelectedCampaignsConfirmString() {
        return new IModel<String>() {

            @Override
            public String getObject() {

                final List<Selectable> selectedData = WebComponentUtil.getSelectedData(getCampaignsTable());
                if (selectedData.size() > 1) {
                    return createStringResource("PageCertCampaigns.message.closeCampaignConfirmMultiple",
                            selectedData.size()).getString();
                } else if (selectedData.size() == 1) {
                    return createStringResource("PageCertCampaigns.message.closeCampaignConfirmSingle",
                            ((CertCampaignListItemDto) selectedData.get(0)).getName()).getString();
                } else {
                    return "EMPTY";
                }
            }
        };
    }

    private IModel<String> createReiterateSelectedCampaignsConfirmString() {
        return new ReadOnlyModel<>(() -> {
            final List<Selectable> selectedData = WebComponentUtil.getSelectedData(getCampaignsTable());
            if (selectedData.size() > 1) {
                return createStringResource("PageCertCampaigns.message.reiterateCampaignConfirmMultiple",
                        selectedData.size()).getString();
            } else if (selectedData.size() == 1) {
                return createStringResource("PageCertCampaigns.message.reiterateCampaignConfirmSingle",
                        ((CertCampaignListItemDto) selectedData.get(0)).getName()).getString();
            } else {
                return "EMPTY";
            }
        });
    }

    private IModel<String> createDeleteCampaignConfirmString() {
        return new IModel<String>() {

            @Override
            public String getObject() {
                return createStringResource("PageCertCampaigns.message.deleteCampaignConfirmSingle",
                        relevantCampaign.getName()).getString();
            }
        };
    }

    private IModel<String> createDeleteSelectedCampaignsConfirmString() {
        return new IModel<String>() {

            @Override
            public String getObject() {
                final List<Selectable> selectedData = WebComponentUtil.getSelectedData(getCampaignsTable());
                if (selectedData.size() > 1) {
                    return createStringResource("PageCertCampaigns.message.deleteCampaignConfirmMultiple",
                            selectedData.size()).getString();
                } else if (selectedData.size() == 1) {
                    return createStringResource("PageCertCampaigns.message.deleteCampaignConfirmSingle",
                            ((CertCampaignListItemDto) selectedData.get(0)).getName()).getString();
                } else {
                    return "EMPTY";
                }
            }
        };
    }

    private Table getTable() {
        return (Table) get(createComponentPath(ID_MAIN_FORM, ID_CAMPAIGNS_TABLE));
    }

    private List<IColumn<CertCampaignListItemDto, String>> initColumns() {
        List<IColumn<CertCampaignListItemDto, String>> columns = new ArrayList<>();

        IColumn<CertCampaignListItemDto, String> column;

        column = new CheckBoxHeaderColumn<>();
        columns.add(column);

        column = new LinkColumn<CertCampaignListItemDto>(createStringResource("PageCertCampaigns.table.name"),
                AccessCertificationCampaignType.F_NAME.getLocalPart(), CertCampaignListItemDto.F_NAME) {
            @Override
            public void onClick(AjaxRequestTarget target, IModel<CertCampaignListItemDto> rowModel) {
                campaignDetailsPerformed(rowModel.getObject().getOid());
            }
        };
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("PageCertCampaigns.table.description"),
                CertCampaignListItemDto.F_DESCRIPTION);
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("PageCertCampaigns.table.iteration"),
                CertCampaignListItemDto.F_ITERATION);
        columns.add(column);

        column = new EnumPropertyColumn<CertCampaignListItemDto>(createStringResource("PageCertCampaigns.table.state"),
                CertCampaignListItemDto.F_STATE) {
            @Override
            protected String translate(Enum en) {
                return createStringResourceStatic(getPage(), en).getString();
            }
        };
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("PageCertCampaigns.table.stage"),
                CertCampaignListItemDto.F_CURRENT_STAGE_NUMBER);
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("PageCertCampaigns.table.escalationLevel"),
                CertCampaignListItemDto.F_ESCALATION_LEVEL_NUMBER);
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("PageCertCampaigns.table.stages"),
                CertCampaignListItemDto.F_NUMBER_OF_STAGES);
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("PageCertCampaigns.table.deadline"),
                CertCampaignListItemDto.F_DEADLINE_AS_STRING);
        columns.add(column);

        column = new SingleButtonColumn<CertCampaignListItemDto>(new Model<>(), null) {

            @Override
            public boolean isButtonEnabled(IModel<CertCampaignListItemDto> model) {
                final AccessCertificationCampaignType campaign = model.getObject().getCampaign();
                String button = determineAction(campaign);
                return button != null;
            }

            @Override
            public boolean isButtonVisible(IModel<CertCampaignListItemDto> model) {
                final AccessCertificationCampaignType campaign = model.getObject().getCampaign();

                return campaign.getState() != AccessCertificationCampaignStateType.IN_REMEDIATION
                        && campaign.getState() != AccessCertificationCampaignStateType.CLOSED;
            }

            @Override
            public String getCaption() {
                AccessCertificationCampaignType campaign = getRowModel().getObject().getCampaign();
                String button = determineAction(campaign);
                if (button != null) {
                    return PageCertCampaigns.this.createStringResource(button).getString();
                } else {
                    return "";
                }
            }

            @Override
            public String getButtonCssColorClass() {
                return DoubleButtonColumn.ButtonColorClass.PRIMARY.toString();
            }

            @Override
            public String getButtonCssSizeClass() {
                return DoubleButtonColumn.ButtonSizeClass.SMALL.toString();
            }

            @Override
            public void clickPerformed(AjaxRequestTarget target, IModel<CertCampaignListItemDto> model) {
                AccessCertificationCampaignType campaign = model.getObject().getCampaign();
                String action = determineAction(campaign);
                switch (action) {
                    case OP_START_CAMPAIGN:
                    case OP_OPEN_NEXT_STAGE:
                        openNextStagePerformed(target, campaign);
                        break;
                    case OP_CLOSE_STAGE:
                        closeStageConfirmation(target, model.getObject());
                        break;
                    case OP_START_REMEDIATION:
                        startRemediationPerformed(target, campaign);
                        break;
                    case OP_CLOSE_CAMPAIGN: // not used
                    default:
                        throw new IllegalStateException("Unknown action: " + action);
                }
            }
        };
        columns.add(column);

        columns.add(new InlineMenuHeaderColumn(createInlineMenu()));

        return columns;
    }

    private List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.startSelected")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(PageCertCampaigns.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        startSelectedCampaignsPerformed(target);
                    }
                };
            }
        });
        items.add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.closeSelected")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(PageCertCampaigns.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        closeSelectedCampaignsConfirmation(target);
                    }
                };
            }
        });
        items.add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.reiterateSelected")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(PageCertCampaigns.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        reiterateSelectedCampaignsConfirmation(target);
                    }
                };
            }
        });
        items.add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.deleteSelected")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(PageCertCampaigns.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteSelectedCampaignsConfirmation(target);
                    }
                };
            }
        });
        return items;
    }

    private void createInlineMenuForItem(final CertCampaignListItemDto dto) {

        dto.getMenuItems().clear();
        dto.getMenuItems().add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.close")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<CertCampaignListItemDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        closeCampaignConfirmation(target, dto);
                    }
                };
            }

            @Override
            public IModel<Boolean> getVisible() {
                return new IModel<Boolean>() {
                    @Override
                    public Boolean getObject() {
                        return dto.getState() != AccessCertificationCampaignStateType.CLOSED;
                    }
                };
            }
        });

        dto.getMenuItems().add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.reiterate")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<CertCampaignListItemDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        reiterateCampaignConfirmation(target, dto);
                    }
                };
            }

            @Override
            public IModel<Boolean> getVisible() {
                return new ReadOnlyModel<>(dto::isReiterable);
            }
        });
        dto.getMenuItems().add(new InlineMenuItem(createStringResource("PageCertCampaigns.menu.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<CertCampaignListItemDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteCampaignConfirmation(target, dto);
                    }
                };
            }


        });

    }

    private Table getCampaignsTable() {
        return (Table) get(createComponentPath(ID_MAIN_FORM, ID_CAMPAIGNS_TABLE));
    }

    // endregion

    // region Actions

    // first, actions requiring confirmations are listed here (state = before
    // confirmation)
    // These actions are responsible for setting/unsetting relevantCampaign
    // field.

    // multi-item versions

    private void closeSelectedCampaignsConfirmation(AjaxRequestTarget target) {
        this.relevantCampaign = null;
        if (!ensureSomethingIsSelected(target)) {
            return;
        }
        showMainPopup(getCloseSelectedCampaignsConfirmationPanel(), target);
    }

    private void reiterateSelectedCampaignsConfirmation(AjaxRequestTarget target) {
        this.relevantCampaign = null;
        if (!ensureCampaignForReiterationIsSelected(target)) {
            return;
        }
        showMainPopup(getReiterateSelectedCampaignsConfirmationPanel(), target);
    }

    private Popupable getCloseSelectedCampaignsConfirmationPanel() {
        return new ConfirmationPanel(getMainPopupBodyId(), createCloseSelectedCampaignsConfirmString()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                closeSelectedCampaignsConfirmedPerformed(target);
            }

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("PageCertCampaigns.dialog.title.confirmCloseCampaign");
            }
        };
    }

    private Popupable getReiterateSelectedCampaignsConfirmationPanel() {
        return new ConfirmationPanel(getMainPopupBodyId(), createReiterateSelectedCampaignsConfirmString()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                reiterateSelectedCampaignsConfirmedPerformed(target);
            }

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("PageCertCampaigns.dialog.title.confirmReiterateCampaign");
            }
        };
    }

    private void deleteSelectedCampaignsConfirmation(AjaxRequestTarget target) {
        this.relevantCampaign = null;
        if (!ensureSomethingIsSelected(target)) {
            return;
        }
        showMainPopup(getDeleteSelectedCampaignsConfirmationPanel(),
                target);
    }

    private Popupable getDeleteSelectedCampaignsConfirmationPanel() {
        return new ConfirmationPanel(getMainPopupBodyId(), createDeleteSelectedCampaignsConfirmString()) {
            private static final long serialVersionUID = 1L;
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                deleteSelectedCampaignsConfirmedPerformed(target);
            }

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("PageCertCampaigns.dialog.title.confirmDeleteCampaign");
            }
        };
    }

    private boolean ensureSomethingIsSelected(AjaxRequestTarget target) {
        if (relevantCampaign != null) {
            return true;
        } else if (!WebComponentUtil.getSelectedData(getTable()).isEmpty()) {
            return true;
        } else {
            warn(getString("PageCertCampaigns.message.noCampaignsSelected"));
            target.add(getFeedbackPanel());
            return false;
        }
    }

    private boolean ensureCampaignForReiterationIsSelected(AjaxRequestTarget target) {
        if (relevantCampaign != null) {
            if (relevantCampaign.getCampaign().getState() != AccessCertificationCampaignStateType.CLOSED) {
                warn(getString("PageCertCampaigns.message.campaignToReiterateMustBeClosed"));
                target.add(getFeedbackPanel());
                return false;
            } else {
                return true;
            }
        }

        List<CertCampaignListItemDto> selected = WebComponentUtil.getSelectedData(getTable());
        if (selected.isEmpty()) {
            warn(getString("PageCertCampaigns.message.noCampaignsSelected"));
            target.add(getFeedbackPanel());
            return false;
        }

        // The filtering below is partly redundant (actOnCampaignsPerformed filters campaigns to be reiterated itself)
        // but having it in place we can provide more precise confirmation and informational messages.
        List<CertCampaignListItemDto> notClosed = selected.stream()
                .filter(dto -> dto.getCampaign().getState() != AccessCertificationCampaignStateType.CLOSED)
                .collect(Collectors.toList());
        if (notClosed.size() == selected.size()) {
            warn(selected.size() > 1
                    ? getString("PageCertCampaigns.message.campaignsToReiterateMustBeClosed")
                    : getString("PageCertCampaigns.message.campaignToReiterateMustBeClosed"));
            target.add(getFeedbackPanel());
            return false;
        }
        if (!notClosed.isEmpty()) {
            warn(getString("PageCertCampaigns.message.someCampaignsToReiterateAreNotClosed", notClosed.size()));
            target.add(getFeedbackPanel());
            notClosed.forEach(dto -> dto.setSelected(false));
            return true;
        } else {
            return true;
        }
    }

    // single-item versions

    private void closeStageConfirmation(AjaxRequestTarget target, CertCampaignListItemDto campaignDto) {
        this.relevantCampaign = campaignDto;
        showMainPopup(getCloseStageConfirmationPanel(),
                target);
    }

    private Popupable getCloseStageConfirmationPanel() {
        return new ConfirmationPanel(getMainPopupBodyId(), createCloseStageConfirmString()) {
            private static final long serialVersionUID = 1L;
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                closeStageConfirmedPerformed(target, relevantCampaign);
            }

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("PageCertCampaigns.dialog.title.confirmCloseStage");
            }
        };
    }

    private void closeCampaignConfirmation(AjaxRequestTarget target, CertCampaignListItemDto campaignDto) {
        this.relevantCampaign = campaignDto;
        showMainPopup(getCloseCampaignConfirmationPanel(), target);
    }

    private void reiterateCampaignConfirmation(AjaxRequestTarget target, CertCampaignListItemDto campaignDto) {
        this.relevantCampaign = campaignDto;
        showMainPopup(getReiterateCampaignConfirmationPanel(), target);
    }

    private Popupable getCloseCampaignConfirmationPanel() {
        return new ConfirmationPanel(getMainPopupBodyId(), createCloseCampaignConfirmString()) {
            private static final long serialVersionUID = 1L;
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                closeCampaignConfirmedPerformed(target, relevantCampaign);
            }

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("PageCertCampaigns.dialog.title.confirmCloseCampaign");
            }
        };
    }

    private Popupable getReiterateCampaignConfirmationPanel() {
        return new ConfirmationPanel(getMainPopupBodyId(), createReiterateCampaignConfirmString()) {
            private static final long serialVersionUID = 1L;
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                reiterateCampaignConfirmedPerformed(target, relevantCampaign);
            }

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("PageCertCampaigns.dialog.title.confirmReiterateCampaign");
            }
        };
    }

    private void deleteCampaignConfirmation(AjaxRequestTarget target, CertCampaignListItemDto campaignDto) {
        this.relevantCampaign = campaignDto;
        showMainPopup(getDeleteCampaignConfirmationPanel(),
                target);
    }

    private Popupable getDeleteCampaignConfirmationPanel() {
        return new ConfirmationPanel(getMainPopupBodyId(), createDeleteCampaignConfirmString()) {
            private static final long serialVersionUID = 1L;
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                deleteCampaignConfirmedPerformed(target);
            }

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("PageCertCampaigns.dialog.title.confirmDeleteCampaign");
            }
        };
    }

    // actions after confirmation (single and multiple versions mixed)

    private void deleteCampaignConfirmedPerformed(AjaxRequestTarget target) {
        deleteCampaignsPerformed(target, Collections.singletonList(relevantCampaign));
    }

    private void deleteSelectedCampaignsConfirmedPerformed(AjaxRequestTarget target) {
        deleteCampaignsPerformed(target, WebComponentUtil.getSelectedData(getCampaignsTable()));
    }

    private void closeSelectedCampaignsConfirmedPerformed(AjaxRequestTarget target) {
        actOnCampaignsPerformed(target, OPERATION_CLOSE_CAMPAIGN,
                WebComponentUtil.getSelectedData(getCampaignsTable()));
    }

    private void reiterateSelectedCampaignsConfirmedPerformed(AjaxRequestTarget target) {
        actOnCampaignsPerformed(target, OPERATION_REITERATE_CAMPAIGN,
                WebComponentUtil.getSelectedData(getCampaignsTable()));
    }

    private void startSelectedCampaignsPerformed(AjaxRequestTarget target) {
        actOnCampaignsPerformed(target, OPERATION_START_CAMPAIGN,
                WebComponentUtil.getSelectedData(getCampaignsTable()));
    }

    protected String determineAction(AccessCertificationCampaignType campaign) {
        int currentStage = campaign.getStageNumber();
        int numOfStages = CertCampaignTypeUtil.getNumberOfStages(campaign);
        AccessCertificationCampaignStateType state = campaign.getState();
        String button;
        switch (state) {
            case CREATED:
                button = numOfStages > 0 ? OP_START_CAMPAIGN : null;
                break;
            case IN_REVIEW_STAGE:
                button = OP_CLOSE_STAGE;
                break;
            case REVIEW_STAGE_DONE:
                button = currentStage < numOfStages ? OP_OPEN_NEXT_STAGE : OP_START_REMEDIATION;
                break;
            case IN_REMEDIATION:
            case CLOSED:
            default:
                button = null;
                break;
        }
        return button;
    }

    private void startRemediationPerformed(AjaxRequestTarget target,
            AccessCertificationCampaignType campaign) {
        LOGGER.debug("Start remediation performed for {}", campaign.asPrismObject());
        OperationResult result = new OperationResult(OPERATION_START_REMEDIATION);
        AccessCertificationService acs = getCertificationService();
        try {
            Task task = createSimpleTask(OPERATION_START_REMEDIATION);
            acs.startRemediation(campaign.getOid(), task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }
        WebComponentUtil.safeResultCleanup(result, LOGGER);
        showResult(result);
        target.add((Component) getCampaignsTable());
        target.add(getFeedbackPanel());
    }

    private void openNextStagePerformed(AjaxRequestTarget target, AccessCertificationCampaignType campaign) {
        LOGGER.debug("Start campaign / open next stage performed for {}", campaign.asPrismObject());
        OperationResult result = new OperationResult(OPERATION_OPEN_NEXT_STAGE);
        AccessCertificationService acs = getCertificationService();
        try {
            Task task = createSimpleTask(OPERATION_OPEN_NEXT_STAGE);
            int currentStage = campaign.getStageNumber();
            acs.openNextStage(campaign.getOid(), task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }
        WebComponentUtil.safeResultCleanup(result, LOGGER);
        showResult(result);
        target.add((Component) getCampaignsTable());
        target.add(getFeedbackPanel());
    }

    private void closeCampaignConfirmedPerformed(AjaxRequestTarget target,
            CertCampaignListItemDto campaignDto) {
        AccessCertificationCampaignType campaign = campaignDto.getCampaign();
        LOGGER.debug("Close certification campaign performed for {}", campaign.asPrismObject());

        OperationResult result = new OperationResult(OPERATION_CLOSE_CAMPAIGN);
        try {
            AccessCertificationService acs = getCertificationService();
            Task task = createSimpleTask(OPERATION_CLOSE_CAMPAIGN);
            acs.closeCampaign(campaign.getOid(), task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }
        WebComponentUtil.safeResultCleanup(result, LOGGER);
        showResult(result);
        target.add((Component) getCampaignsTable());
        target.add(getFeedbackPanel());
    }

    private void reiterateCampaignConfirmedPerformed(AjaxRequestTarget target,
            CertCampaignListItemDto campaignDto) {
        AccessCertificationCampaignType campaign = campaignDto.getCampaign();
        LOGGER.debug("Reiterate certification campaign performed for {}", campaign.asPrismObject());

        OperationResult result = new OperationResult(OPERATION_REITERATE_CAMPAIGN);
        try {
            AccessCertificationService acs = getCertificationService();
            Task task = createSimpleTask(OPERATION_REITERATE_CAMPAIGN);
            acs.reiterateCampaign(campaign.getOid(), task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }
        WebComponentUtil.safeResultCleanup(result, LOGGER);
        showResult(result);
        target.add((Component) getCampaignsTable());
        target.add(getFeedbackPanel());
    }

    private void closeStageConfirmedPerformed(AjaxRequestTarget target, CertCampaignListItemDto campaignDto) {
        AccessCertificationCampaignType campaign = campaignDto.getCampaign();
        LOGGER.debug("Close certification stage performed for {}", campaign.asPrismObject());

        OperationResult result = new OperationResult(OPERATION_CLOSE_STAGE);
        try {
            AccessCertificationService acs = getCertificationService();
            Task task = createSimpleTask(OPERATION_CLOSE_STAGE);
            acs.closeCurrentStage(campaign.getOid(), task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }
        WebComponentUtil.safeResultCleanup(result, LOGGER);
        showResult(result);
        target.add((Component) getCampaignsTable());
        target.add(getFeedbackPanel());
    }

    private void campaignDetailsPerformed(String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, oid);
        navigateToNext(PageCertCampaign.class, parameters);
    }

    private void deleteCampaignsPerformed(AjaxRequestTarget target,
            List<CertCampaignListItemDto> itemsToDelete) {
        if (itemsToDelete.isEmpty()) {
            warn(getString("PageCertCampaigns.message.noCampaignsSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        OperationResult result = new OperationResult(OPERATION_DELETE_CAMPAIGNS);
        for (CertCampaignListItemDto itemToDelete : itemsToDelete) {
            try {
                Task task = createSimpleTask(OPERATION_DELETE_CAMPAIGNS);
                ObjectDelta<AccessCertificationCampaignType> delta = getPrismContext().deltaFactory().object().createDeleteDelta(
                        AccessCertificationCampaignType.class, itemToDelete.getOid());
                getModelService().executeChanges(MiscUtil.createCollection(delta), null, task,
                        result);
            } catch (Exception ex) {
                result.recordPartialError(createStringResource("PageCertCampaigns.message.deleteCampaignsPerformed.partialError").getString(), ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete campaign", ex);
            }
        }

        result.recomputeStatus();
        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, createStringResource("PageCertCampaigns.message.deleteCampaignsPerformed.success").getString());
        }

        Table campaignsTable = getCampaignsTable();
        ObjectDataProvider provider = (ObjectDataProvider) campaignsTable.getDataTable().getDataProvider();
        provider.clearCache();
        WebComponentUtil.safeResultCleanup(result, LOGGER);
        showResult(result);
        target.add(getFeedbackPanel(), (Component) campaignsTable);
    }

    private void actOnCampaignsPerformed(AjaxRequestTarget target, String operationName, List<CertCampaignListItemDto> items) {
        int processed = 0;
        AccessCertificationService acs = getCertificationService();

        OperationResult result = new OperationResult(operationName);
        for (CertCampaignListItemDto item : items) {
            try {
                AccessCertificationCampaignType campaign = item.getCampaign();
                Task task = createSimpleTask(operationName);
                if (OPERATION_START_CAMPAIGN.equals(operationName)) {
                    if (campaign.getState() == AccessCertificationCampaignStateType.CREATED) {
                        acs.openNextStage(campaign.getOid(), task, result);
                        processed++;
                    }
                } else if (OPERATION_CLOSE_CAMPAIGN.equals(operationName)) {
                    if (campaign.getState() != AccessCertificationCampaignStateType.CLOSED) {
                        acs.closeCampaign(campaign.getOid(), task, result);
                        processed++;
                    }
                } else if (OPERATION_REITERATE_CAMPAIGN.equals(operationName)) {
                    if (item.isReiterable()) {
                        acs.reiterateCampaign(campaign.getOid(), task, result);
                        processed++;
                    }
                } else {
                    throw new IllegalStateException("Unknown action: " + operationName);
                }
            } catch (Exception ex) {
                result.recordPartialError(createStringResource("PageCertCampaigns.message.actOnCampaignsPerformed.partialError").getString(), ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't process campaign", ex);
            }
        }

        if (processed == 0) {
            warn(getString("PageCertCampaigns.message.noCampaignsSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        result.recomputeStatus();
        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, createStringResource("PageCertCampaigns.message.actOnCampaignsPerformed.success", processed).getString());
        }
        WebComponentUtil.safeResultCleanup(result, LOGGER);
        showResult(result);
        target.add(getFeedbackPanel(), (Component) getCampaignsTable());
    }
    // endregion

}
