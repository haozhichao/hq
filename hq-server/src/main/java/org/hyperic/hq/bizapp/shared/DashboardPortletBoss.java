/*
 * Generated by XDoclet - Do not edit!
 */
package org.hyperic.hq.bizapp.shared;

import java.util.List;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Local interface for DashboardPortletBoss.
 */
public interface DashboardPortletBoss {

    public JSONArray getMeasurementData(AuthzSubject subj, Integer resId, Integer mtid, AppdefEntityTypeID ctype,
                                        long begin, long end) throws PermissionException;

    public JSONObject getAllGroups(AuthzSubject subj) throws PermissionException, JSONException;

    public JSONObject getAlertCounts(AuthzSubject subj, List<Integer> groupIds, PageInfo pageInfo)
        throws PermissionException, JSONException;

}