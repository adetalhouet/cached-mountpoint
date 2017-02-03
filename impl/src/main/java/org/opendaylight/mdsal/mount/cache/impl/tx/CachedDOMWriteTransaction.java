/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.mount.cache.impl.tx;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.concurrent.Executor;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.broker.impl.TransactionCommitFailedExceptionMapper;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.util.concurrent.MappingCheckedFuture;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by adetalhouet on 2017-02-02.
 */
// FIXME partial duplicated code from ConcurrentDOMDataBroker
public class CachedDOMWriteTransaction implements DOMDataWriteTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(CachedDOMWriteTransaction.class);

    private static final String CAN_COMMIT = "CAN_COMMIT";
    private static final String PRE_COMMIT = "PRE_COMMIT";
    private static final String COMMIT = "COMMIT";

    private final Executor clientFutureCallbackExecutor;

    private final String nodeId;
    private final DOMStoreWriteTransaction writeTransaction;

    public CachedDOMWriteTransaction(final String nodeId,
                                     final DOMStoreWriteTransaction writeTransaction,
                                     final Executor clientFutureCallbackExecutor) {
        this.nodeId = nodeId;
        this.clientFutureCallbackExecutor = clientFutureCallbackExecutor;
        this.writeTransaction = writeTransaction;
    }

    @Override
    public boolean cancel() {
        if (writeTransaction != null) {
            writeTransaction.close();
        }
        return false;
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        DOMStoreThreePhaseCommitCohort cohort = writeTransaction.ready();

        Preconditions.checkArgument(cohort != null, "%s: Cohort must not be null." + nodeId);
        LOG.debug("{}: Tx: {} is submitted for execution.", nodeId, writeTransaction.getIdentifier());

        final AsyncNotifyingSettableFuture clientSubmitFuture =
                new AsyncNotifyingSettableFuture(clientFutureCallbackExecutor);

        doCanCommit(clientSubmitFuture, writeTransaction, cohort);

        return MappingCheckedFuture.create(clientSubmitFuture,
                TransactionCommitFailedExceptionMapper.COMMIT_ERROR_MAPPER);
    }

    private void doCanCommit(final AsyncNotifyingSettableFuture clientSubmitFuture,
                             final DOMStoreWriteTransaction transaction,
                             final DOMStoreThreePhaseCommitCohort cohort) {

        // Not using Futures.allAsList here to avoid its internal overhead.
        FutureCallback<Boolean> futureCallback = new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (result == null || !result) {
                    handleException(clientSubmitFuture, transaction, cohort,
                            CAN_COMMIT, TransactionCommitFailedExceptionMapper.CAN_COMMIT_ERROR_MAPPER,
                            new TransactionCommitFailedException(
                                    "%s: Can Commit failed, no detailed cause available." + nodeId));
                } else {
                    // All cohorts completed successfully - we can move on to the preCommit phase
                    doPreCommit(clientSubmitFuture, transaction, cohort);
                }
            }

            @Override
            public void onFailure(Throwable failure) {
                handleException(clientSubmitFuture, transaction, cohort, CAN_COMMIT,
                        TransactionCommitFailedExceptionMapper.CAN_COMMIT_ERROR_MAPPER, failure);
            }
        };

        ListenableFuture<Boolean> canCommitFuture = cohort.canCommit();
        Futures.addCallback(canCommitFuture, futureCallback, MoreExecutors.directExecutor());
    }

    private void doPreCommit(final AsyncNotifyingSettableFuture clientSubmitFuture,
                             final DOMStoreWriteTransaction transaction,
                             final DOMStoreThreePhaseCommitCohort cohort) {


        // Not using Futures.allAsList here to avoid its internal overhead.
        FutureCallback<Void> futureCallback = new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void notUsed) {
                // All cohorts completed successfully - we can move on to the commit phase
                doCommit(clientSubmitFuture, transaction, cohort);
            }

            @Override
            public void onFailure(Throwable failure) {
                handleException(clientSubmitFuture, transaction, cohort, PRE_COMMIT,
                        TransactionCommitFailedExceptionMapper.PRE_COMMIT_MAPPER, failure);
            }
        };

        ListenableFuture<Void> preCommitFuture = cohort.preCommit();
        Futures.addCallback(preCommitFuture, futureCallback, MoreExecutors.directExecutor());
    }

    private void doCommit(final AsyncNotifyingSettableFuture clientSubmitFuture,
                          final DOMStoreWriteTransaction transaction,
                          final DOMStoreThreePhaseCommitCohort cohort) {


        // Not using Futures.allAsList here to avoid its internal overhead.
        FutureCallback<Void> futureCallback = new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void notUsed) {
                clientSubmitFuture.set();
            }

            @Override
            public void onFailure(Throwable throwable) {
                handleException(clientSubmitFuture, transaction, cohort, COMMIT,
                        TransactionCommitFailedExceptionMapper.COMMIT_ERROR_MAPPER, throwable);
            }
        };

        ListenableFuture<Void> commitFuture = cohort.commit();
        Futures.addCallback(commitFuture, futureCallback, MoreExecutors.directExecutor());
    }

    @Override
    @Deprecated
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        throw new RuntimeException("Deprecated");
    }


    @Override
    public void delete(LogicalDatastoreType store, YangInstanceIdentifier path) {
        LOG.debug("{}: Delete store={} for path={}", nodeId, store, path);

        Preconditions.checkState(writeTransaction != null);

        try {
            writeTransaction.delete(path);
        } catch (Exception e) {
            LOG.error("{}: Failed to delete store={} for path={}", nodeId, store, path);
        }

    }

    @Override
    public void put(LogicalDatastoreType store, YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        LOG.debug("{}: Put data={} in store={} for path={}", nodeId, data, store, path);

        Preconditions.checkState(writeTransaction != null);

        try {
            writeTransaction.write(path, data);
        } catch (Exception e) {
            LOG.error("{}: Failed to put data={} in store={} for path={}", nodeId, data, store, path);
        }
    }

    @Override
    public void merge(LogicalDatastoreType store, YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        LOG.debug("{}: Merge data={} in store={} for path={}", nodeId, data, store, path);

        Preconditions.checkState(writeTransaction != null);

        try {
            writeTransaction.merge(path, data);
        } catch (Exception e) {
            LOG.error("{}: Failed to Merge data={} in store={} for path={}", nodeId, data, store, path);
        }
    }

    @Override
    public Object getIdentifier() {
        return this;
    }

    // FIXME DUPLICATE CODE BELLOW -------------------------------------------------------------------------------------

    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
            justification = "Pertains to the assignment of the 'clientException' var. FindBugs flags this as an "
                    + "uncomfirmed cast but the generic type in TransactionCommitFailedExceptionMapper is "
                    + "TransactionCommitFailedException and thus should be deemed as confirmed.")
    private static void handleException(final AsyncNotifyingSettableFuture clientSubmitFuture,
                                        final DOMStoreWriteTransaction transaction,
                                        final DOMStoreThreePhaseCommitCohort cohort,
                                        final String phase, final TransactionCommitFailedExceptionMapper exMapper,
                                        final Throwable throwable) {

        if (clientSubmitFuture.isDone()) {
            // We must have had failures from multiple cohorts.
            return;
        }

        LOG.warn("Tx: {} Error during phase {}, starting Abort", transaction.getIdentifier(), phase, throwable);
        final Exception e = new RuntimeException("Unexpected error occurred", throwable);

        final TransactionCommitFailedException clientException = exMapper.apply(e);

        // Transaction failed - tell all cohorts to abort.

        @SuppressWarnings("unchecked")
        ListenableFuture<Void>[] canCommitFutures = new ListenableFuture[1];
        canCommitFutures[0] = cohort.abort();

        ListenableFuture<List<Void>> combinedFuture = Futures.allAsList(canCommitFutures);
        Futures.addCallback(combinedFuture, new FutureCallback<List<Void>>() {
            @Override
            public void onSuccess(List<Void> notUsed) {
                // Propagate the original exception to the client.
                clientSubmitFuture.setException(clientException);
            }

            @Override
            public void onFailure(Throwable failure) {
                LOG.error("Tx: {} Error during Abort.", transaction.getIdentifier(), failure);

                // Propagate the original exception as that is what caused the Tx to fail and is
                // what's interesting to the client.
                clientSubmitFuture.setException(clientException);
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * A settable future that uses an {@link Executor} to execute listener callback Runnables,
     * registered via {@link #addListener}, asynchronously when this future completes. This is
     * done to guarantee listener executions are off-loaded onto another thread to avoid blocking
     * the thread that completed this future, as a common use case is to pass an executor that runs
     * tasks in the same thread as the caller (ie MoreExecutors#sameThreadExecutor)
     * to {@link #addListener}.
     * FIXME: This class should probably be moved to yangtools common utils for re-usability and
     * unified with AsyncNotifyingListenableFutureTask.
     */
    private static class AsyncNotifyingSettableFuture extends AbstractFuture<Void> {

        /**
         * ThreadLocal used to detect if the task completion thread is running the future listener Runnables.
         */
        private static final ThreadLocal<Boolean> ON_TASK_COMPLETION_THREAD_TL = new ThreadLocal<>();

        private final Executor listenerExecutor;

        AsyncNotifyingSettableFuture(Executor listenerExecutor) {
            this.listenerExecutor = Preconditions.checkNotNull(listenerExecutor);
        }

        @Override
        public void addListener(final Runnable listener, final Executor executor) {
            // Wrap the listener Runnable in a DelegatingRunnable. If the specified executor is one
            // that runs tasks in the same thread as the caller submitting the task
            // (e.g. {@link com.google.common.util.concurrent.MoreExecutors#sameThreadExecutor}) and
            // the listener is executed from the #set methods, then the DelegatingRunnable will detect
            // this via the ThreadLocal and submit the listener Runnable to the listenerExecutor.
            //
            // On the other hand, if this task is already complete, the call to ExecutionList#add in
            // superclass will execute the listener Runnable immediately and, since the ThreadLocal
            // won't be set, the DelegatingRunnable will run the listener Runnable inline.
            super.addListener(new DelegatingRunnable(listener, listenerExecutor), executor);
        }

        boolean set() {
            ON_TASK_COMPLETION_THREAD_TL.set(Boolean.TRUE);
            try {
                return super.set(null);
            } finally {
                ON_TASK_COMPLETION_THREAD_TL.set(null);
            }
        }

        @Override
        protected boolean setException(Throwable throwable) {
            ON_TASK_COMPLETION_THREAD_TL.set(Boolean.TRUE);
            try {
                return super.setException(throwable);
            } finally {
                ON_TASK_COMPLETION_THREAD_TL.set(null);
            }
        }

        private static final class DelegatingRunnable implements Runnable {
            private final Runnable delegate;
            private final Executor executor;

            DelegatingRunnable(final Runnable delegate, final Executor executor) {
                this.delegate = Preconditions.checkNotNull(delegate);
                this.executor = Preconditions.checkNotNull(executor);
            }

            @Override
            public void run() {
                if (ON_TASK_COMPLETION_THREAD_TL.get() != null) {
                    // We're running on the task completion thread so off-load to the executor.
                    LOG.trace("Submitting ListenenableFuture Runnable from thread {} to executor {}",
                            Thread.currentThread().getName(), executor);
                    executor.execute(delegate);
                } else {
                    // We're not running on the task completion thread so run the delegate inline.
                    LOG.trace("Executing ListenenableFuture Runnable on this thread: {}",
                            Thread.currentThread().getName());
                    delegate.run();
                }
            }
        }
    }
}
