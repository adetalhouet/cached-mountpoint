/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.mount.cache.impl.tx;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.mdsal.mount.cache.impl.InMemoryDeviceDOMDataStorePool;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by adetalhouet on 2017-02-02.
 */
public class CachedDOMReadOnlyTransaction extends CachedAbstractWriteTransaction implements DOMDataReadOnlyTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(CachedDOMReadOnlyTransaction.class);

    private DOMStoreReadTransaction readTransaction;

    CachedDOMReadOnlyTransaction(final NodeId nodeId,
                                 final SchemaContext schemaContext,
                                 final InMemoryDeviceDOMDataStorePool pool) {
        super(nodeId, schemaContext, pool);
    }

    @Override
    public void close() {
        if (readTransaction != null) {
            readTransaction.close();
            readTransaction = null;
        }
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(LogicalDatastoreType store, YangInstanceIdentifier path) {

        LOG.debug("{}: Read store={} path={}", nodeId.getValue(), store, path);

        try {
            final InMemoryDOMDataStore dbStore = pool.getInMemoryDOMDataStore(path, schemaContext, store);
            Preconditions.checkState(readTransaction == null, "%s: A CachedDOMReadOnlyTransaction is already open" + nodeId.getValue());
            readTransaction = dbStore.newReadOnlyTransaction();

            return readTransaction.read(path);

        } catch (Exception e) {
            LOG.error("{}: Failed to read store={} for path={}", nodeId.getValue(), store, path);

            return null;
        }
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(LogicalDatastoreType store, YangInstanceIdentifier path) {
        LOG.debug("{}: Exists store={} path={}", nodeId.getValue(), store, path);

        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read = read(
                store, path);

        final ListenableFuture<Boolean> transform = Futures
                .transform(read, new Function<Optional<NormalizedNode<?, ?>>, Boolean>() {
                    @Nullable
                    @Override
                    public Boolean apply(final Optional<NormalizedNode<?, ?>> input) {
                        return input.isPresent();
                    }
                });

        return Futures.makeChecked(transform, new Function<Exception, ReadFailedException>() {
            @Nullable
            @Override
            public ReadFailedException apply(final Exception e) {
                return new ReadFailedException("%s: Unable to read from cache" + nodeId.getValue(), e);
            }
        });
    }

    @Override
    public Object getIdentifier() {
        return this;
    }
}
