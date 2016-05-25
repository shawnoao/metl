/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.ui.views.deploy;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.AgentDeploymentParameter;
import org.jumpmind.metl.core.model.AgentDeploymentSummary;
import org.jumpmind.metl.core.model.AgentResource;
import org.jumpmind.metl.core.model.AgentStartMode;
import org.jumpmind.metl.core.model.DeploymentStatus;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.FlowParameter;
import org.jumpmind.metl.core.runtime.IAgentManager;
import org.jumpmind.metl.core.runtime.resource.Datasource;
import org.jumpmind.metl.core.runtime.resource.LocalFile;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.IBackgroundRefreshable;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.init.BackgroundRefresherService;
import org.jumpmind.metl.ui.views.manage.ExecutionLogPanel;
import org.jumpmind.util.AppUtils;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container.Sortable;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.DefaultItemSorter;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.ResourceReference;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class EditAgentPanel extends VerticalLayout implements IUiPanel, IBackgroundRefreshable, AgentDeploymentChangeListener {

    private static final long serialVersionUID = 1L;

    final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    TabbedPanel tabbedPanel;

    Agent agent;

    Table table;

    BeanItemContainer<AgentDeploymentSummary> container;

    Button addDeploymentButton;

    Button enableButton;

    Button disableButton;

    Button removeButton;

    Button editButton;

    Button runButton;

    FlowSelectWindow flowSelectWindow;

    BackgroundRefresherService backgroundRefresherService;

    public EditAgentPanel(ApplicationContext context, TabbedPanel tabbedPanel, Agent agent) {
        this.context = context;
        this.tabbedPanel = tabbedPanel;
        this.agent = agent;
        this.backgroundRefresherService = context.getBackgroundRefresherService();

        HorizontalLayout editAgentLayout = new HorizontalLayout();
        editAgentLayout.setSpacing(true);
        editAgentLayout.setMargin(new MarginInfo(true, false, false, true));
        editAgentLayout.addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);
        addComponent(editAgentLayout);

        final ComboBox startModeCombo = new ComboBox("Start Mode");
        startModeCombo.setImmediate(true);
        startModeCombo.setNullSelectionAllowed(false);
        AgentStartMode[] modes = AgentStartMode.values();
        for (AgentStartMode agentStartMode : modes) {
            startModeCombo.addItem(agentStartMode.name());
        }
        startModeCombo.setValue(agent.getStartMode());
        startModeCombo.addValueChangeListener(event -> {
            agent.setStartMode((String) startModeCombo.getValue());
            context.getConfigurationService().save((AbstractObject) EditAgentPanel.this.agent);
        });

        editAgentLayout.addComponent(startModeCombo);
        editAgentLayout.setComponentAlignment(startModeCombo, Alignment.BOTTOM_LEFT);

        Button parameterButton = new Button("Parameters");
        parameterButton.addClickListener(new ParameterClickListener());
        editAgentLayout.addComponent(parameterButton);
        editAgentLayout.setComponentAlignment(parameterButton, Alignment.BOTTOM_LEFT);

        HorizontalLayout buttonGroup = new HorizontalLayout();

        final TextField hostNameField = new TextField("Hostname");
        hostNameField.setImmediate(true);
        hostNameField.setTextChangeEventMode(TextChangeEventMode.LAZY);
        hostNameField.setTextChangeTimeout(100);
        hostNameField.setWidth(20, Unit.EM);
        hostNameField.setNullRepresentation("");
        hostNameField.setValue(agent.getHost());
        hostNameField.addValueChangeListener(event -> {
            agent.setHost((String) hostNameField.getValue());
            EditAgentPanel.this.context.getConfigurationService().save((AbstractObject) agent);
            EditAgentPanel.this.context.getAgentManager().refresh(agent);
        });
        
        buttonGroup.addComponent(hostNameField);
        buttonGroup.setComponentAlignment(hostNameField, Alignment.BOTTOM_LEFT);

        Button getHostNameButton = new Button("Get Host");
        getHostNameButton.addClickListener(event -> hostNameField.setValue(AppUtils.getHostName()));
        buttonGroup.addComponent(getHostNameButton);
        buttonGroup.setComponentAlignment(getHostNameButton, Alignment.BOTTOM_LEFT);

        editAgentLayout.addComponent(buttonGroup);
        editAgentLayout.setComponentAlignment(buttonGroup, Alignment.BOTTOM_LEFT);

        Button exportButton = new Button("Export Agent Config", event -> exportConfiguration());
        editAgentLayout.addComponent(exportButton);
        editAgentLayout.setComponentAlignment(exportButton, Alignment.BOTTOM_LEFT);

        CheckBox autoRefresh = new CheckBox("Auto Refresh", Boolean.valueOf(agent.isAutoRefresh()));
        autoRefresh.setImmediate(true);        
        autoRefresh.addValueChangeListener(event -> {
            agent.setAutoRefresh(autoRefresh.getValue());
            EditAgentPanel.this.context.getConfigurationService().save((AbstractObject) agent);
            EditAgentPanel.this.context.getAgentManager().refresh(agent);
        });
        editAgentLayout.addComponent(autoRefresh);
        editAgentLayout.setComponentAlignment(autoRefresh, Alignment.BOTTOM_LEFT);
        
        CheckBox allowTestFlowsField = new CheckBox("Allow Test Flows", Boolean.valueOf(agent.isAllowTestFlows()));
        allowTestFlowsField.setImmediate(true);        
        allowTestFlowsField.addValueChangeListener(event -> {
            agent.setAllowTestFlows(allowTestFlowsField.getValue());
            EditAgentPanel.this.context.getConfigurationService().save((AbstractObject) agent);
            EditAgentPanel.this.context.getAgentManager().refresh(agent);
        });
        editAgentLayout.addComponent(allowTestFlowsField);
        editAgentLayout.setComponentAlignment(allowTestFlowsField, Alignment.BOTTOM_LEFT);
 
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);
               
        addDeploymentButton = buttonBar.addButton("Add Deployment", Icons.DEPLOYMENT);
        addDeploymentButton.addClickListener(new AddDeploymentClickListener());

        editButton = buttonBar.addButton("Edit", FontAwesome.EDIT);
        editButton.addClickListener(event -> editClicked());

        enableButton = buttonBar.addButton("Enable", FontAwesome.CHAIN);
        enableButton.addClickListener(event -> enableClicked());

        disableButton = buttonBar.addButton("Disable", FontAwesome.CHAIN_BROKEN);
        disableButton.addClickListener(event -> disableClicked());

        removeButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);
        removeButton.addClickListener(event -> removeClicked());

        runButton = buttonBar.addButton("Run", Icons.RUN);
        runButton.addClickListener(event -> runClicked());

        container = new BeanItemContainer<AgentDeploymentSummary>(AgentDeploymentSummary.class);
        container.setItemSorter(new TableItemSorter());

        table = new Table();
        table.setSizeFull();
        table.setCacheRate(100);
        table.setPageLength(100);
        table.setImmediate(true);
        table.setSelectable(true);
        table.setMultiSelect(true);

        table.setContainerDataSource(container);
        table.setVisibleColumns("name", "projectName", "type", "status", "logLevel", "startType", "startExpression");
        table.setColumnHeaders("Deployment", "Project", "Type", "Status", "Log Level", "Start Type", "Start Expression");
        table.addGeneratedColumn("status", new StatusRenderer());
        table.addItemClickListener(new TableItemClickListener());
        table.addValueChangeListener(new TableValueChangeListener());
        table.setSortContainerPropertyId("type");
        table.setSortAscending(true);

        addComponent(table);
        setExpandRatio(table, 1.0f);
        refresh();
        setButtonsEnabled();
        backgroundRefresherService.register(this);
    }

    protected void exportConfiguration() {
        final String export = context.getConfigurationService().export(agent);
        StreamSource ss = new StreamSource() {
            private static final long serialVersionUID = 1L;

            public InputStream getStream() {
                try {
                    return new ByteArrayInputStream(export.getBytes());
                } catch (Exception e) {
                    log.error("Failed to export configuration", e);
                    CommonUiUtils.notify("Failed to export configuration.", Type.ERROR_MESSAGE);
                    return null;
                }

            }
        };
        String datetime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        StreamResource resource = new StreamResource(ss,
                String.format("%s-config-%s.sql", agent.getName().toLowerCase().replaceAll(" ", "-"), datetime));
        final String KEY = "export";
        setResource(KEY, resource);
        Page.getCurrent().open(ResourceReference.create(resource, this, KEY).getURL(), null);
    }

    @Override
    public boolean closing() {
        backgroundRefresherService.unregister(this);
        return true;
    }

    @Override
    public void selected() {
    }

    @Override
    public void deselected() {
    }

    public void changed(AgentDeployment agentDeployment) {
        for (AgentDeploymentSummary summary : container.getItemIds()) {
            if (summary.getId().equals(agentDeployment.getId())) {
                summary.copy(agentDeployment);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object onBackgroundDataRefresh() {
        return getRefreshData();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBackgroundUIRefresh(Object backgroundData) {
        updateItems((List<AgentDeploymentSummary>) backgroundData);
    }

    protected List<AgentDeploymentSummary> getRefreshData() {
        return context.getConfigurationService().findAgentDeploymentSummary(agent.getId());
    }

    public void refresh() {
        updateItems(getRefreshData());
    }

    protected void updateItem(AgentDeploymentSummary summary) {
        Set<AgentDeploymentSummary> selectedItems = getSelectedItems();
        container.removeItem(summary);
        container.addItem(summary);
        table.sort();
        setSelectedItems(selectedItems);
        setButtonsEnabled();
    }

    protected void updateItems(List<AgentDeploymentSummary> summaries) {
        boolean isChanged = false;
        Set<AgentDeploymentSummary> selectedItems = getSelectedItems();
        for (AgentDeploymentSummary summary : summaries) {
            BeanItem<AgentDeploymentSummary> beanItem = container.getItem(summary);
            if (beanItem == null || beanItem.getBean().isChanged(summary)) {
                container.removeItem(summary);
                container.addItem(summary);
                isChanged = true;
            }
        }
        Set<AgentDeploymentSummary> items = new HashSet<AgentDeploymentSummary>(container.getItemIds());
        for (AgentDeploymentSummary summary : items) {
            if (!summaries.contains(summary)) {
                container.removeItem(summary);
                isChanged = true;
            }
        }
        if (isChanged) {
            table.sort();
            setSelectedItems(selectedItems);
            setButtonsEnabled();
        }
    }

    protected void setButtonsEnabled() {
        boolean canRemove = false;
        boolean canEnable = false;
        boolean canDisable = false;
        boolean canRun = false;
        Set<AgentDeploymentSummary> selectedIds = getSelectedItems();
        for (AgentDeploymentSummary summary : selectedIds) {
            if (summary.isFlow()) {
                if (summary.getStatus().equals(DeploymentStatus.DEPLOYED.name())
                        || summary.getStatus().equals(DeploymentStatus.DISABLED.name())
                        || summary.getStatus().equals(DeploymentStatus.ERROR.name())) {
                    canRemove = true;
                }
                if (summary.getStatus().equals(DeploymentStatus.DEPLOYED.name())) {
                    canDisable = true;
                    if (summary.isFlow()) {
                        canRun = true;
                    }
                }
                if (summary.getStatus().equals(DeploymentStatus.DISABLED.name())
                        || summary.getStatus().equals(DeploymentStatus.ERROR.name())) {
                    canEnable = true;
                }
            }
        }
        runButton.setEnabled(canRun && selectedIds.size() == 1);
        enableButton.setEnabled(canEnable);
        disableButton.setEnabled(canDisable);
        removeButton.setEnabled(canRemove);
        editButton.setEnabled(getSelectedItems().size() > 0);
    }

    @SuppressWarnings("unchecked")
    protected Set<AgentDeploymentSummary> getSelectedItems() {
        return (Set<AgentDeploymentSummary>) table.getValue();
    }

    protected void setSelectedItems(Set<AgentDeploymentSummary> selectedItems) {
        table.setValue(null);
        for (AgentDeploymentSummary summary : selectedItems) {
            BeanItem<AgentDeploymentSummary> beanItem = container.getItem(summary);
            if (beanItem != null) {
                AgentDeploymentSummary updatedSummary = beanItem.getBean();
                table.select(updatedSummary);
            }
        }
    }

    class AddDeploymentClickListener implements ClickListener, FlowSelectListener {
        private static final long serialVersionUID = 1L;

        public void buttonClick(ClickEvent event) {
            if (isBlank(agent.getHost())) {
                CommonUiUtils.notify("Before you can deploy to an agent.  You must select a hostname.", Type.ASSISTIVE_NOTIFICATION);
            } else {
                if (flowSelectWindow == null) {
                    String introText = "Select one or more flows for deployment to this agent.";
                    flowSelectWindow = new FlowSelectWindow(context, "Add Deployment", introText, agent.isAllowTestFlows());
                    flowSelectWindow.setFlowSelectListener(this);
                }
                UI.getCurrent().addWindow(flowSelectWindow);
            }
        }

        public void selected(Collection<FlowName> flowCollection) {
            for (FlowName flowName : flowCollection) {
                Flow flow = context.getConfigurationService().findFlow(flowName.getId());
                AgentDeployment deployment = new AgentDeployment();
                deployment.setAgentId(agent.getId());
                deployment.setFlow(flow);
                deployment.setName(getName(flow.getName()));
                List<AgentDeploymentParameter> deployParams = deployment.getAgentDeploymentParameters();
                for (FlowParameter flowParam : flow.getFlowParameters()) {
                    AgentDeploymentParameter deployParam = new AgentDeploymentParameter();
                    deployParam.setFlowParameterId(flowParam.getId());
                    deployParam.setAgentDeploymentId(deployment.getId());
                    deployParam.setName(flowParam.getName());
                    deployParam.setValue(flowParam.getDefaultValue());
                    deployParams.add(deployParam);
                }
                context.getConfigurationService().save(deployment);
            }
            refresh();
        }

        protected String getName(String name) {
            for (Object deployment : container.getItemIds()) {
                if (deployment instanceof AgentDeployment) {
                    AgentDeployment agentDeployment = (AgentDeployment) deployment;
                    if (name.equals(agentDeployment.getName())) {
                        if (name.matches(".*\\([0-9]+\\)$")) {
                            String num = name.substring(name.lastIndexOf("(") + 1, name.lastIndexOf(")"));
                            name = name.replaceAll("\\([0-9]+\\)$", "(" + (Integer.parseInt(num) + 1) + ")");
                        } else {
                            name += " (1)";
                        }
                        return getName(name);
                    }
                }
            }
            return name;
        }
    }

    protected void runClicked() {
        AgentDeploymentSummary summary = (AgentDeploymentSummary) getSelectedItems().iterator().next();
        if (summary.isFlow()) {
            AgentDeployment deployment = context.getConfigurationService().findAgentDeployment(summary.getId());
            IAgentManager agentManager = context.getAgentManager();
            String executionId = agentManager.getAgentRuntime(deployment.getAgentId()).scheduleNow(deployment);
            if (executionId != null) {
                ExecutionLogPanel logPanel = new ExecutionLogPanel(executionId, context, tabbedPanel, null);
                tabbedPanel.addCloseableTab(executionId, "Run " + deployment.getName(), Icons.LOG, logPanel);
            }
        }
    }

    protected void editClicked() {
        AgentDeploymentSummary summary = (AgentDeploymentSummary) getSelectedItems().iterator().next();
        if (summary.isFlow()) {
            AgentDeployment deployment = context.getConfigurationService().findAgentDeployment(summary.getId());
            EditAgentDeploymentPanel editPanel = new EditAgentDeploymentPanel(context, deployment, EditAgentPanel.this, tabbedPanel);
            tabbedPanel.addCloseableTab(deployment.getId(), deployment.getName(), Icons.DEPLOYMENT, editPanel);
        } else {
            AgentResource agentResource = context.getConfigurationService().findAgentResource(agent.getId(), summary.getId());
            EditAgentResourcePanel editPanel = new EditAgentResourcePanel(context, agentResource);
            FontAwesome icon = Icons.GENERAL_RESOURCE;
            if (agentResource.getType().equals(Datasource.TYPE)) {
                icon = Icons.DATABASE;
            } else if (agentResource.getType().equals(LocalFile.TYPE)) {
                icon = Icons.FILE_SYSTEM;
            }
            tabbedPanel.addCloseableTab(summary.getId(), summary.getName(), icon, editPanel);
        }
    }

    protected void enableClicked() {
        Set<AgentDeploymentSummary> selectedIds = getSelectedItems();
        for (AgentDeploymentSummary summary : selectedIds) {
            if (summary.isFlow()) {
                AgentDeployment deployment = context.getConfigurationService().findAgentDeployment(summary.getId());
                deployment.setStatus(DeploymentStatus.REQUEST_ENABLE.name());
                summary.setStatus(DeploymentStatus.REQUEST_ENABLE.name());
                context.getConfigurationService().save(deployment);
                updateItem(summary);
            }
        }
    }

    protected void disableClicked() {
        Set<AgentDeploymentSummary> selectedIds = getSelectedItems();
        for (AgentDeploymentSummary summary : selectedIds) {
            if (summary.isFlow()) {
                AgentDeployment deployment = context.getConfigurationService().findAgentDeployment(summary.getId());
                deployment.setStatus(DeploymentStatus.REQUEST_DISABLE.name());
                summary.setStatus(DeploymentStatus.REQUEST_DISABLE.name());
                context.getConfigurationService().save(deployment);
                updateItem(summary);
            }
        }
    }

    protected void removeClicked() {
        Set<AgentDeploymentSummary> selectedIds = getSelectedItems();
        for (AgentDeploymentSummary summary : selectedIds) {
            if (summary.isFlow()) {
                AgentDeployment deployment = context.getConfigurationService().findAgentDeployment(summary.getId());
                deployment.setStatus(DeploymentStatus.REQUEST_REMOVE.name());
                summary.setStatus(DeploymentStatus.REQUEST_REMOVE.name());
                context.getConfigurationService().save(deployment);
                updateItem(summary);
            }
        }
    }
    
    class StatusRenderer implements ColumnGenerator {
        private static final long serialVersionUID = 1L;
        @Override
        public Object generateCell(Table source, Object itemId, Object columnId) {            
            String status = itemId != null ? ((AgentDeploymentSummary)itemId).getStatus() : null;
            return status != null ? DeploymentStatus.valueOf(status).toString() : null;
        }
    }

    class TableItemClickListener implements ItemClickListener {
        private static final long serialVersionUID = 1L;

        long lastClick;

        public void itemClick(ItemClickEvent event) {
            if (event.isDoubleClick()) {
                editButton.click();
            } else if (getSelectedItems().contains(event.getItemId()) && System.currentTimeMillis() - lastClick > 500) {
                table.setValue(null);
            }
            lastClick = System.currentTimeMillis();
        }
    }

    class TableValueChangeListener implements ValueChangeListener {
        private static final long serialVersionUID = 1L;

        public void valueChange(ValueChangeEvent event) {
            setButtonsEnabled();
        }
    }

    class TableItemSorter extends DefaultItemSorter {
        private static final long serialVersionUID = 1L;

        Object[] propertyId;

        boolean[] ascending;

        public void setSortProperties(Sortable container, Object[] propertyId, boolean[] ascending) {
            super.setSortProperties(container, propertyId, ascending);
            this.propertyId = propertyId;
            this.ascending = ascending;
        }

        public int compare(Object o1, Object o2) {
            AgentDeploymentSummary s1 = (AgentDeploymentSummary) o1;
            AgentDeploymentSummary s2 = (AgentDeploymentSummary) o2;
            if (propertyId != null && propertyId.length > 0 && propertyId[0].equals("projectName")) {
                return new CompareToBuilder().append(s1.getProjectName(), s2.getProjectName()).append(s1.getName(), s2.getName())
                        .toComparison() * (ascending[0] ? 1 : -1);
            }
            return super.compare(o1, o2);
        }
    }

    class ParameterClickListener implements ClickListener {
        private static final long serialVersionUID = 1L;

        public void buttonClick(ClickEvent event) {
            EditAgentParametersWindow window = new EditAgentParametersWindow(context, agent);
            window.showAtSize(0.5);
        }
    }

}
