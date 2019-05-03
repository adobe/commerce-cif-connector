<%--

    Copyright 2019 Adobe. All rights reserved.
    This file is licensed to you under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under
    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
    OF ANY KIND, either express or implied. See the License for the specific language
    governing permissions and limitations under the License.

--%><%
%><%@include file="/libs/commerce/gui/components/admin/products/products.jsp" %><%
%><%@page session="false"%><%
%><%@page import="java.util.Calendar,
                  java.util.Collections,
                  java.util.Comparator,
                  java.util.LinkedList,
                  java.util.List,
                  java.util.Locale,
                  java.time.format.DateTimeFormatter,
                  java.time.format.FormatStyle,
                  java.time.LocalDateTime,
                  java.time.ZoneId,
                  org.apache.commons.lang.StringUtils,
                  org.apache.sling.api.resource.ResourceResolver,
                  com.adobe.granite.security.user.util.AuthorizableUtil,
                  com.day.cq.commons.jcr.JcrConstants,
                  com.adobe.granite.ui.components.AttrBuilder,
                  com.adobe.granite.ui.components.Tag,
                  com.adobe.granite.workflow.exec.Workflow,
                  com.adobe.granite.workflow.job.AbsoluteTimeoutHandler,
                  com.day.cq.commons.servlets.AbstractListServlet,
                  com.adobe.granite.workflow.status.WorkflowStatus,
                  com.day.cq.replication.ReplicationQueue,
                  com.day.cq.replication.ReplicationStatus,
                  com.adobe.cq.commerce.common.CommerceHelper,
                  com.day.cq.i18n.I18n" %><%

    WorkflowStatus workflowStatus = resource.adaptTo(WorkflowStatus.class);
    ReplicationStatus replicationStatus = resource.adaptTo(ReplicationStatus.class);

    Calendar modifiedDateRaw = properties.get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
    String modifiedBy = AuthorizableUtil.getFormattedName(resourceResolver, properties.get(JcrConstants.JCR_LAST_MODIFIED_BY, String.class));
    if (modifiedBy == null) {
        modifiedBy = "";
    }

    Calendar publishedDateRaw = null;
    String publishedBy = null;
    Boolean isDeactivated = false;
    if (replicationStatus != null) {
        publishedDateRaw = replicationStatus.getLastPublished();
        publishedBy = AuthorizableUtil.getFormattedName(resourceResolver, replicationStatus.getLastPublishedBy());
        isDeactivated = replicationStatus.isDeactivated();
    }

    String title = CommerceHelper.getCardTitle(resource, pageManager);

    List<String> applicableRelationships = getActionRels(resource, properties, product, acm, sling);

    Tag tag = cmp.consumeTag();
    AttrBuilder attrs = tag.getAttrs();

    attrs.addClass("foundation-collection-navigator");
    //attrs passed to component may contain this tag, avoid setting it twice, tag already set is not our usecase.
    if (!attrs.build().contains("data-foundation-collection-navigator-href=")) {
        attrs.add("data-foundation-collection-navigator-href",
                xssAPI.getValidHref(request.getContextPath() + getAdminUrl(resource, currentPage)));
    }

    attrs.add("is", "coral-table-row");
    attrs.add("data-timeline", true);

%><tr <%= attrs %>>
    <td is="coral-table-cell" coral-table-rowselect><%
        if (product != null) {
            String thumbnailUrl = CommerceHelper.getProductCardThumbnail(request.getContextPath(), product);
    %><img class="foundation-collection-item-thumbnail" src="<%= xssAPI.getValidHref(thumbnailUrl) %>" alt=""><%
    } else {
        String icon = isVirtual(resource) || isCloudBoundFolder(resource) ? "cloud": "folder";
    %><coral-icon class="foundation-collection-item-thumbnail" icon="<%= icon %>"></coral-icon><%
        }
    %></td>
    <td class="foundation-collection-item-title" is="coral-table-cell" value="<%= xssAPI.encodeForHTMLAttr(title) %>">
        <%= xssAPI.encodeForHTML(title) %>
    </td>

    <td is="coral-table-cell" value="<%= (modifiedDateRaw != null) ? xssAPI.encodeForHTMLAttr(Long.toString(modifiedDateRaw.getTimeInMillis())) : "0" %>"><%
        if (modifiedDateRaw != null) {
    %><foundation-time type="datetime" value="<%= xssAPI.encodeForHTMLAttr(modifiedDateRaw.toInstant().toString()) %>"></foundation-time><%

        // Modified-after-publish indicator
        if (publishedDateRaw != null && publishedDateRaw.before(modifiedDateRaw)) {
            String modifiedAfterPublishStatus = i18n.get("Modified since last publication");
    %><coral-icon class="aem-PageRow-icon aem-PageRow-icon--warning" icon="alert" size="XS" title="<%= xssAPI.encodeForHTMLAttr(modifiedAfterPublishStatus) %>"></coral-icon><%
        }

    %><div class="foundation-layout-util-subtletext"><%= xssAPI.encodeForHTML(modifiedBy) %></div><%
        }
    %></td>
    <td is="coral-table-cell" value="<%= (!isDeactivated && publishedDateRaw != null) ? xssAPI.encodeForHTMLAttr(Long.toString(publishedDateRaw.getTimeInMillis())) : "0" %>"><%

        if (!isVirtual(resource)) {

            // Published date and status
            if (!isDeactivated && publishedDateRaw != null) {
    %><foundation-time type="datetime" value="<%= xssAPI.encodeForHTMLAttr(publishedDateRaw.toInstant().toString()) %>"></foundation-time><%
            } else {
    %><span><%= xssAPI.encodeForHTML(i18n.get("Not published")) %></span><%
            }

            // Publication/un-publication pending indicator
            String publicationPendingStatus = getPublicationPendingStatus(replicationStatus, i18n);
            if (publicationPendingStatus.length() > 0) {
    %><coral-icon class="aem-PageRow-icon aem-PageRow-icon--warning" icon="pending" size="XS" title="<%= xssAPI.encodeForHTMLAttr(publicationPendingStatus) %>"></coral-icon><%
            }

            // Publication/un-publication scheduled indicator
            List<Workflow> scheduledWorkflows = getScheduledWorkflows(workflowStatus);
            if (!isDeactivated && scheduledWorkflows.size() > 0) {
                String scheduledStatus = getScheduledStatus(scheduledWorkflows, i18n, request.getLocale(), resourceResolver);
                if (scheduledStatus.length() > 0) {
    %><coral-icon class="aem-PageRow-icon aem-PageRow-icon--info" icon="calendar" size="XS" title="<%= xssAPI.encodeForHTMLAttr(scheduledStatus) %>"></coral-icon><%
                }
            }

            // Published by
            if (!isDeactivated && publishedBy != null) {
    %><div class="foundation-layout-util-subtletext"><%= xssAPI.encodeForHTML(publishedBy) %></div><%
            }

        }

    %>
        <meta class="foundation-collection-quickactions" data-foundation-collection-quickactions-rel="<%= xssAPI.encodeForHTMLAttr(StringUtils.join(applicableRelationships, " ")) %>"/>
    </td>
    <td is="coral-table-cell" alignment="right">
        <button is="coral-button" variant="minimal" icon="dragHandle" coral-table-roworder></button>
    </td>
</tr><%!

    private String formatAbsoluteDate(Calendar time, Locale locale) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault());
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT).withLocale(locale).format(dateTime);
    }

    private String getPublicationPendingStatus(ReplicationStatus replicationStatus, I18n i18n) {
        if (replicationStatus == null) return "";

        String status = "";
        String actionType = "";
        int maxQueuePos = -1;

        for (ReplicationQueue.Entry e : replicationStatus.getPending()) {
            if (e.getQueuePosition() > maxQueuePos) {
                maxQueuePos = e.getQueuePosition();
                actionType = e.getAction().getType().getName();
            }
        }

        maxQueuePos = maxQueuePos + 1;

        if (maxQueuePos > 0) {
            if ("Activate".equals(actionType)) {
                status = i18n.get("Publication Pending. #{0} in the queue.", "0 is the position in the queue", maxQueuePos);
            } else {
                status = i18n.get("Un-publication Pending. #{0} in the queue.", "0 is the position in the queue", maxQueuePos);
            }
        }

        return status;
    }

    private String getScheduledStatus(List<Workflow> scheduledWorkflows, I18n i18n, Locale locale, ResourceResolver resourceResolver) {
        String status = "";
        int i = 0;

        for (Workflow scheduledWorkflow : scheduledWorkflows) {
            if (i > 0) {
                status += "\n\n";
            }

            if (isScheduledActivationWorkflow(scheduledWorkflow)) {
                status += i18n.get("Publication Pending") + "\n";
                status += i18n.get("Version") + ": ";
                status += scheduledWorkflow.getWorkflowData().getMetaDataMap().get("resourceVersion",
                        String.class) + "\n";
            } else {
                status += i18n.get("Un-publication Pending") + "\n";
            }

            status += i18n.get("Scheduled") + ": ";

            Long absoluteTime = scheduledWorkflow.getWorkflowData().getMetaDataMap().get(AbsoluteTimeoutHandler.ABS_TIME,
                    Long.class);
            Calendar timeout = Calendar.getInstance();
            timeout.setTimeInMillis(absoluteTime);
            status += formatAbsoluteDate(timeout, locale);

            status += " (" + AuthorizableUtil.getFormattedName(resourceResolver, scheduledWorkflow.getInitiator()) + ")";
            i++;
        }

        return status;
    }

    private List<Workflow> getScheduledWorkflows(WorkflowStatus workflowStatus) {
        List<Workflow> scheduledWorkflows = new LinkedList<Workflow>();

        // Get the scheduled workflows
        if (workflowStatus != null) {
            List<Workflow> workflows = workflowStatus.getWorkflows(false);
            for (Workflow workflow : workflows) {
                if (isScheduledActivationWorkflow(workflow) || isScheduledDeactivationWorkflow(workflow)) {
                    scheduledWorkflows.add(workflow);
                }
            }
        }

        // Sort the scheduled workflows by time started
        Collections.sort(scheduledWorkflows, new Comparator<Workflow>() {
            public int compare(Workflow o1, Workflow o2) {
                return o1.getTimeStarted().compareTo(o2.getTimeStarted());
            }
        });

        return scheduledWorkflows;
    }

    private boolean isScheduledActivationWorkflow(Workflow workflow) {
        if (workflow == null) return false;
        return workflow.getWorkflowModel().getId().equals(AbstractListServlet.ListItem.SCHEDULED_ACTIVATION_WORKFLOW_ID);
    }

    private boolean isScheduledDeactivationWorkflow(Workflow workflow) {
        if (workflow == null) return false;
        return workflow.getWorkflowModel().getId().equals(AbstractListServlet.ListItem.SCHEDULED_DEACTIVATION_WORKFLOW_ID);
    }

%>
