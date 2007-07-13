package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl
import org.hyperic.hq.events.server.session.Alert
import org.hyperic.hq.galerts.server.session.GalertLog

class AlertCategory {
    static String urlFor(Alert a, String context) {
        def d = a.alertDefinition
        "/alerts/Alerts.do?mode=viewAlert&eid=${d.appdefEntityId}&a=${a.id}"
    }
    
    static String urlFor(GalertLog a, String context) {
        def d = a.alertDef
        "/alerts/Alerts.do?mode=viewAlert&eid=${d.appdefID}&a=${a.id}"
    }

    static AuthzSubject getAcknowledgedBy(Alert a) {
        def id = a.ackedBy
        if (id == null)
            return null
            
        AuthzSubjectManagerEJBImpl.one.getSubjectById(id.toInteger())
    }
}
