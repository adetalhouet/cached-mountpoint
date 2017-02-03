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
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by adetalhouet on 2017-02-02.
 */
public class CachedDOMReadOnlyTransaction implements DOMDataReadOnlyTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(CachedDOMReadOnlyTransaction.class);

    private final String nodeId;
    private final DOMStoreReadTransaction readTransaction;

    public CachedDOMReadOnlyTransaction(final String nodeId,
                                        final DOMStoreReadTransaction readTransaction) {
        this.nodeId = nodeId;
        this.readTransaction = Preconditions.checkNotNull(readTransaction);
    }

    @Override
    public void close() {
        if (readTransaction != null) {
            readTransaction.close();
        }
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(LogicalDatastoreType store, YangInstanceIdentifier path) {

        LOG.debug("{}: Read store={} path={}", nodeId, store, path);

        try {
            Preconditions.checkState(readTransaction != null, "%s: DOMStoreReadTransaction is null" + nodeId);

            return readTransaction.read(path);

        } catch (Exception e) {
            LOG.error("{}: Failed to read store={} for path={}", nodeId, store, path);

            return null;
        }
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(LogicalDatastoreType store, YangInstanceIdentifier path) {
        LOG.debug("{}: Exists store={} path={}", nodeId, store, path);

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
                return new ReadFailedException("%s: Unable to read from cache" + nodeId, e);
            }
        });
    }

    @Override
    public Object getIdentifier() {
        return this;
    }
}
