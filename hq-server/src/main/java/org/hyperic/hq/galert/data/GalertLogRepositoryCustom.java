package org.hyperic.hq.galert.data;

import java.util.List;

import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface GalertLogRepositoryCustom {

    List<GalertLog> findByCreateTimeAndPriority(long begin, long end, AlertSeverity severity,
                                                boolean inEscalation, boolean notFixed, Integer groupId,
                                                Integer galertDefId, Pageable pageable);

    Page<GalertLog> findByGroupAndTimestampBetween(ResourceGroup group, long begin, long end,
                                                   Pageable pageable);

    GalertLog findLastByDefinition(GalertDef def, boolean fixed);
}