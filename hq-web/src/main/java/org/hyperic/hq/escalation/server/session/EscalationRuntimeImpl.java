/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.hq.escalation.server.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.TransactionListener;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.events.ActionExecutionInfo;
import org.hyperic.hq.events.server.session.Action;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import EDU.oswego.cs.dl.util.concurrent.ClockDaemon;
import EDU.oswego.cs.dl.util.concurrent.Executor;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.Semaphore;

/**
 * This class manages the runtime execution of escalation chains. The
 * persistence part of the escalation engine lives within
 * {@link EscalationManagerImpl}
 * 
 * This class does very little within hanging onto and remembering state data.
 * It only knows about the escalation state ID and the time that it is to wake
 * up and perform the next actions.
 * 
 * The workflow looks something like this:
 * 
 * /- EscalationRunner Runtime ->[ScheduleWatcher] -- EscalationRunner \_
 * EscalationRunner | ->EsclManager.executeState
 * 
 * 
 * The Runtime puts {@link EscalationState}s into the schedule. When the
 * schedule determines the state's time is ready to run, the task is passed off
 * into an EscalationRunner (which comes from a thread pool) and kicked off.
 */
@Component
public class EscalationRuntimeImpl implements EscalationRuntime {

    private final ThreadLocal _batchUnscheduleTxnListeners = new ThreadLocal();
    private final Log _log = LogFactory.getLog(EscalationRuntime.class);
    private final ClockDaemon _schedule = new ClockDaemon();
    private final Map _stateIdsToTasks = new HashMap();
    private final Map _esclEntityIdsToStateIds = new HashMap();

    private final Semaphore _mutex = new Semaphore(1);

    private final Set _uncomittedEscalatingEntities = Collections.synchronizedSet(new HashSet());
    private final PooledExecutor _executor;
    private final EscalationStateDAO escalationStateDao;
    private AuthzSubjectManager authzSubjectManager;
    private final Log log = LogFactory.getLog(EscalationRuntime.class);

    @Autowired
    public EscalationRuntimeImpl(EscalationStateDAO escalationStateDao, AuthzSubjectManager authzSubjectManager) {
        this.escalationStateDao = escalationStateDao;
        this.authzSubjectManager = authzSubjectManager;
        _executor = new PooledExecutor(new LinkedQueue());
        _executor.setKeepAliveTime(-1); // Threads never die off
        _executor.createThreads(3); // # of threads to service requests
    }

   

    /**
     * This class is invoked when the clock daemon wakes up and decides that it
     * is time to look at an escalation.
     */
    private class ScheduleWatcher implements Runnable {
        private Integer _stateId;
        private Executor _executor;

        private ScheduleWatcher(Integer stateId, Executor executor) {
            _stateId = stateId;
            _executor = executor;
        }

        public void run() {
            try {
                _executor.execute(new EscalationRunner(_stateId));
            } catch (InterruptedException e) {
                _log.warn("Interrupted while trying to execute state [" + _stateId + "]");
            }
        }
    }

    /**
     * Unschedule the execution of an escalation state. The unschedule will only
     * occur if the transaction successfully commits.
     */
    public void unscheduleEscalation(EscalationState state) {
        final Integer stateId = state.getId();

        HQApp.getInstance().addTransactionListener(new TransactionListener() {
            public void afterCommit(boolean success) {
                if (success) {
                    unscheduleEscalation_(stateId);
                }
            }

            public void beforeCommit() {
            }
        });
    }

    /**
     * Unschedule the execution of all escalation states associated with this
     * entity that performs escalations. The unschedule will only occur if the
     * transaction successfully commits.
     */
    public void unscheduleAllEscalationsFor(PerformsEscalations def) {
        BatchUnscheduleEscalationsTransactionListener batchTxnListener = (BatchUnscheduleEscalationsTransactionListener) _batchUnscheduleTxnListeners
            .get();

        if (batchTxnListener == null) {
            batchTxnListener = new BatchUnscheduleEscalationsTransactionListener();
            _batchUnscheduleTxnListeners.set(batchTxnListener);
            HQApp.getInstance().addTransactionListener(batchTxnListener);
        }

        batchTxnListener.unscheduleAllEscalationsFor(def);
    }

    /**
     * A txn listener that unschedules escalations in batch. This class is not
     * thread safe. We assume that this txn listener is called back by the same
     * thread originally unscheduling the escalations.
     */
    private class BatchUnscheduleEscalationsTransactionListener implements TransactionListener {
        private final Set _escalationsToUnschedule;

        public BatchUnscheduleEscalationsTransactionListener() {
            _escalationsToUnschedule = new HashSet();
        }

        /**
         * Unscheduled all escalations associated with this entity.
         * 
         * @param def The entity that performs escalations.
         */
        public void unscheduleAllEscalationsFor(PerformsEscalations def) {
            _escalationsToUnschedule.add(new EscalatingEntityIdentifier(def));
        }

        public void afterCommit(boolean success) {
            try {
                _log.debug("Transaction committed:  success=" + success);
                if (success) {
                    unscheduleAllEscalations_((EscalatingEntityIdentifier[]) _escalationsToUnschedule
                        .toArray(new EscalatingEntityIdentifier[_escalationsToUnschedule.size()]));
                }
            } finally {
                _batchUnscheduleTxnListeners.set(null);
            }
        }

        public void beforeCommit() {
            deleteAllEscalations_((EscalatingEntityIdentifier[]) _escalationsToUnschedule
                .toArray(new EscalatingEntityIdentifier[_escalationsToUnschedule.size()]));
        }

    }

    private void deleteAllEscalations_(EscalatingEntityIdentifier[] escalatingEntities) {
        List stateIds = new ArrayList(escalatingEntities.length);

        synchronized (_stateIdsToTasks) {
            for (int i = 0; i < escalatingEntities.length; i++) {
                Integer stateId = (Integer) _esclEntityIdsToStateIds.get(escalatingEntities[i]);
                // stateId may be null if an escalation has not been scheduled
                // for this escalating entity.
                if (stateId != null) {
                    stateIds.add(stateId);
                }

            }
        }
        escalationStateDao.removeAllEscalationStates((Integer[]) stateIds.toArray(new Integer[stateIds.size()]));
    }

    private void unscheduleEscalation_(Integer stateId) {
        synchronized (_stateIdsToTasks) {
            doUnscheduleEscalation_(stateId);
            _esclEntityIdsToStateIds.values().remove(stateId);
        }
    }

    private void unscheduleAllEscalations_(EscalatingEntityIdentifier[] esclEntityIds) {
        synchronized (_stateIdsToTasks) {
            for (int i = 0; i < esclEntityIds.length; i++) {
                Integer stateId = (Integer) _esclEntityIdsToStateIds.remove(esclEntityIds[i]);
                doUnscheduleEscalation_(stateId);
            }
        }
    }

    private void doUnscheduleEscalation_(Integer stateId) {
        if (stateId != null) {
            Object task = _stateIdsToTasks.remove(stateId);

            if (task != null) {
                ClockDaemon.cancel(task);
                _log.debug("Canceled state[" + stateId + "]");
            } else {
                _log.debug("Canceling state[" + stateId + "] but was " + "not found");
            }
        }
    }

    /**
     * Acquire the mutex.
     * 
     * @throws InterruptedException
     */
    public void acquireMutex() throws InterruptedException {
        _mutex.acquire();
    }

    /**
     * Release the mutex.
     */
    public void releaseMutex() {
        _mutex.release();
    }

    /**
     * Add the uncommitted escalation state for this entity performing
     * escalations to the uncommitted escalation state cache. This cache is used
     * to track escalation states that have been scheduled but are not visible
     * to other threads prior to the transaction commit.
     * 
     * @param def The entity that performs escalations.
     * @return <code>true</code> if there is already an uncommitted escalation
     *         state.
     */
    public boolean addToUncommittedEscalationStateCache(PerformsEscalations def) {
        return !_uncomittedEscalatingEntities.add(new EscalatingEntityIdentifier(def));
    }

    /**
     * Remove the uncommitted escalation state for this entity performing
     * escalations from the uncommitted escalation state cache.
     * 
     * @param def The entity that performs escalations.
     * @param postTxnCommit <code>true</code> to remove post txn commit;
     *        <code>false</code> to remove immediately.
     */
    public void removeFromUncommittedEscalationStateCache(final PerformsEscalations def, boolean postTxnCommit) {
        if (postTxnCommit) {
            boolean addedTxnListener = false;

            try {
                HQApp.getInstance().addTransactionListener(new TransactionListener() {

                    public void afterCommit(boolean success) {
                        removeFromUncommittedEscalationStateCache(def, false);
                    }

                    public void beforeCommit() {
                    }

                });

                addedTxnListener = true;
            } finally {
                if (!addedTxnListener) {
                    removeFromUncommittedEscalationStateCache(def, false);
                }
            }
        } else {
            _uncomittedEscalatingEntities.remove(new EscalatingEntityIdentifier(def));
        }

    }

    /**
     * This method introduces an escalation state to the runtime. The escalation
     * will be invoked according to the next action time of the state.
     * 
     * If the state had been previously scheduled, it will be rescheduled with
     * the new time.
     */
    public void scheduleEscalation(final EscalationState state) {
        final long schedTime = state.getNextActionTime();

        HQApp.getInstance().addTransactionListener(new TransactionListener() {
            public void afterCommit(boolean success) {
                _log.debug("Transaction committed:  success=" + success);

                if (success) {
                    scheduleEscalation_(state, schedTime);
                }
            }

            public void beforeCommit() {
            }
        });
    }

    private void scheduleEscalation_(EscalationState state, long schedTime) {
        Integer stateId = state.getId();

        if (stateId == null) {
            throw new IllegalStateException("Cannot schedule a " + "transient escalation state (stateId=null).");
        }

        synchronized (_stateIdsToTasks) {
            Object task = _stateIdsToTasks.get(stateId);

            if (task != null) {
                // Previously scheduled. Unschedule
                ClockDaemon.cancel(task);
                _log.debug("Rescheduling state[" + stateId + "]");
            } else {
                _log.debug("Scheduling state[" + stateId + "]");
            }

            task = _schedule.executeAt(new Date(schedTime), new ScheduleWatcher(stateId, _executor));

            _stateIdsToTasks.put(stateId, task);
            _esclEntityIdsToStateIds.put(new EscalatingEntityIdentifier(state), stateId);
        }
    }

    
    /**
     * Check if the escalation state or its associated escalating entity has
     * been deleted.
     * 
     * @param s The escalation state.
     * @return <code>true</code> if the escalation state or escalating entity
     *         has been deleted.
     */
    private boolean hasEscalationStateOrEscalatingEntityBeenDeleted(EscalationState escalationState) {
        if (escalationState == null) {
            return true;
        }

        try {
            PerformsEscalations alertDefinition = escalationState.getAlertType().findDefinition(
                new Integer(escalationState.getAlertDefinitionId()));

            // galert defs may be deleted from the DB when the group is deleted,
            // so we may get a null value.
            return alertDefinition == null || alertDefinition.isDeleted();
        } catch (Throwable e) {
            return true;
        }
    }
    
    @Transactional
    public void endEscalation(EscalationState escalationState) {
        if (escalationState != null) {
            escalationStateDao.remove(escalationState);
            unscheduleEscalation(escalationState);
        }
    }
    
    @Transactional
    public void executeState(Integer stateId) {
        // Use a get() so that the state is retrieved from the
        // database (in case the escalation state was deleted
        // in a separate session when ending an escalation).
        // The get() will return null if the escalation state
        // does not exist.
        EscalationState escalationState = escalationStateDao.get(stateId);

        if (hasEscalationStateOrEscalatingEntityBeenDeleted(escalationState)) {
            // just to be safe
            endEscalation(escalationState);

            return;
        }

        Escalation escalation = escalationState.getEscalation();
        int actionIdx = escalationState.getNextAction();

        // XXX -- Need to make sure the application is running before
        // we allow this to proceed
        log.debug("Executing state[" + escalationState.getId() + "]");

        if (actionIdx >= escalation.getActions().size()) {
            if (escalation.isRepeat() && escalation.getActions().size() > 0) {
                actionIdx = 0; // Loop back
            } else {
                log.debug("Reached the end of the escalation state[" + escalationState.getId() + "].  Ending it");

                endEscalation(escalationState);

                return;
            }
        }

        EscalationAction escalationAction = (EscalationAction) escalation.getActions().get(actionIdx);
        Action action = escalationAction.getAction();
        Escalatable alert = getEscalatable(escalationState);

        // HQ-1348: End escalation if alert is already fixed
        if (alert.getAlertInfo().isFixed()) {
            endEscalation(escalationState);

            return;
        }

        // Always make sure that we increase the state offset of the
        // escalation so we don't loop fo-eva
        Random random = new Random();
        long offset = 65000 + random.nextInt(25000);
        long nextTime = System.currentTimeMillis() + Math.max(offset, escalationAction.getWaitTime());

        log.debug("Moving onto next state of escalation, but waiting for " + escalationAction.getWaitTime() + " ms");

        escalationState.setNextAction(actionIdx + 1);
        escalationState.setNextActionTime(nextTime);
        escalationState.setAcknowledgedBy(null);
        scheduleEscalation(escalationState);

        try {
            EscalationAlertType type = escalationState.getAlertType();
            AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
            ActionExecutionInfo execInfo = new ActionExecutionInfo(alert.getShortReason(), alert.getLongReason(), alert
                .getAuxLogs());
            String detail = action.executeAction(alert.getAlertInfo(), execInfo);

            type.changeAlertState(alert, overlord, EscalationStateChange.ESCALATED);
            type.logActionDetails(alert, action, detail, null);
        } catch (Exception e) {
            log.error("Unable to execute action [" + action.getClassName() + "] for escalation definition [" +
                      escalationState.getEscalation().getName() + "]", e);
        }
    }
    
    @Transactional
    public Escalatable getEscalatable(EscalationState escalationState) {
        return escalationState.getAlertType().findEscalatable(new Integer(escalationState.getAlertId()));
    }

}