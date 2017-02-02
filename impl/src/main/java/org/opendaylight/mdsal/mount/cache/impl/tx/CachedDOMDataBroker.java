/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.mount.cache.impl.tx;

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
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.mount.cache.impl.InMemoryDeviceDOMDataStorePool;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by adetalhouet on 2017-02-02.
 */
public class CachedDOMDataBroker implements DOMDataBroker {

    private static final Logger LOG = LoggerFactory.getLogger(CachedDOMDataBroker.class);

    private final NodeId nodeId;
    private final SchemaContext schemaContext;
    private final InMemoryDeviceDOMDataStorePool pool;
    private final Executor clientFutureCallbackExecutor;

    public CachedDOMDataBroker(final NodeId nodeId,
                               final SchemaContext schemaContext,
                               final InMemoryDeviceDOMDataStorePool pool,
                               final Executor clientFutureCallbackExecutor) {
        LOG.info("{}: Create CachedDOMDataBroker instance for schemaContext={}",nodeId.getValue(), schemaContext);
        this.nodeId = nodeId;
        this.schemaContext = schemaContext;
        this.pool = pool;
        this.clientFutureCallbackExecutor = clientFutureCallbackExecutor;
    }



    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        return new CachedDOMReadOnlyTransaction(nodeId, schemaContext, pool);
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        return new CachedDOMReadWriteTransaction(newReadOnlyTransaction(), newWriteOnlyTransaction());
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return new CachedDOMWriteTransaction(nodeId, schemaContext, pool, clientFutureCallbackExecutor);
    }

    @Override
    public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(
            final LogicalDatastoreType store, final YangInstanceIdentifier path, final DOMDataChangeListener listener,
            final DataChangeScope triggeringScope) {
        throw new UnsupportedOperationException("Data change listeners not supported for mount point");
    }

    @Override
    public DOMTransactionChain createTransactionChain(final TransactionChainListener transactionChainListener) {
        throw new RuntimeException("Not implemented");
    }

    @Nonnull
    @Override
    public Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> getSupportedExtensions() {
        return Collections.emptyMap();
    }
}
