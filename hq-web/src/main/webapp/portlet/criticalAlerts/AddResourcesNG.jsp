<%@ page language="java" %>
<%@ page errorPage="/common/Error2.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".
  
  Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
  This file is part of HQ.
  
  HQ is free software; you can redistribute it and/or modify
  it under the terms version 2 of the GNU General Public License as
  published by the Free Software Foundation. This program is distributed
  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE. See the GNU General Public License for more
  details.
  
  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
  USA.
 --%>


<s:form method="POST" action="executeCAResourcesHandlePortlet">
<s:hidden theme="simple" id="key" name="key" value=".ng.dashContent.criticalalerts.resources"/>
<s:hidden theme="simple" id="token" name="token" />

<tiles:insertDefinition name=".portlet.error"/>

<tiles:insertDefinition name=".page.title">
  <tiles:putAttribute name="titleKey" value="dash.settings.alerts.AddResourcesPageTitle"/>
  <tiles:putAttribute name="titleBgStyle" value="PageTitle"/>
  <tiles:putAttribute name="titleImg" value="spacer.gif"/>  
</tiles:insertDefinition>

<tiles:insertTemplate template="/portlet/addresources/AddResourcesFormNG.jsp"/>

<tiles:insertDefinition name=".form.buttons">
  <tiles:putAttribute name="addToList" value="true"/>
  <tiles:putAttribute name="cancelAction"  value="cancelCriticalAlertsModifyAddResourcesPortlet" />
  <tiles:putAttribute name="resetAction"   value="resetCriticalAlertsModifyAddResourcesPortlet" />
  <tiles:putAttribute name="addedAction"   value="addCAResourcesHandlePortlet" />
</tiles:insertDefinition>

<tiles:insertDefinition name=".page.footer"/>

</s:form>
