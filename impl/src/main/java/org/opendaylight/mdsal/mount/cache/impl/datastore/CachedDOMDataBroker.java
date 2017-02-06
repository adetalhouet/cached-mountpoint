/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.mount.cache.impl.datastore;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.mdsal.mount.cache.impl.tx.CachedDOMReadOnlyTransaction;
import org.opendaylight.mdsal.mount.cache.impl.tx.CachedDOMReadWriteTransaction;
import org.opendaylight.mdsal.mount.cache.impl.tx.CachedDOMWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by adetalhouet on 2017-02-02.
 */
public class CachedDOMDataBroker implements DOMDataBroker, DOMDataTreeChangeService {

    private static final Logger LOG = LoggerFactory.getLogger(CachedDOMDataBroker.class);

    private final String nodeId;
    private final InMemoryDOMDataStore inMemoryDOMDataStore;
    private final Executor clientFutureCallbackExecutor;

    public CachedDOMDataBroker(final String nodeId,
                               final InMemoryDOMDataStore inMemoryDOMDataStore,
                               final Executor clientFutureCallbackExecutor) {
        LOG.info("{}: Create CachedDOMDataBroker instance", nodeId);
        this.nodeId = nodeId;
        this.inMemoryDOMDataStore = inMemoryDOMDataStore;
        this.clientFutureCallbackExecutor = clientFutureCallbackExecutor;
    }

    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        return new CachedDOMReadOnlyTransaction(nodeId, inMemoryDOMDataStore.newReadOnlyTransaction());
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        return new CachedDOMReadWriteTransaction(newReadOnlyTransaction(), newWriteOnlyTransaction());
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return new CachedDOMWriteTransaction(nodeId, inMemoryDOMDataStore.newWriteOnlyTransaction(),
                clientFutureCallbackExecutor);
    }

    @Override
    @Deprecated
    public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(
            final LogicalDatastoreType store, final YangInstanceIdentifier path, final DOMDataChangeListener listener,
            final DataChangeScope triggeringScope) {
        return inMemoryDOMDataStore.registerChangeListener(path, listener, triggeringScope);
    }

    @Override
    public DOMTransactionChain createTransactionChain(final TransactionChainListener transactionChainListener) {
        throw new RuntimeException("Not implemented");
    }

    @Nonnull
    @Override
    public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerDataTreeChangeListener(@Nonnull DOMDataTreeIdentifier domDataTreeIdentifier, @Nonnull L l) {
        return inMemoryDOMDataStore.registerTreeChangeListener(domDataTreeIdentifier.getRootIdentifier(), l);
    }

    @Nonnull
    @Override
    public Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> getSupportedExtensions() {
        return Collections.emptyMap();
    }

}
