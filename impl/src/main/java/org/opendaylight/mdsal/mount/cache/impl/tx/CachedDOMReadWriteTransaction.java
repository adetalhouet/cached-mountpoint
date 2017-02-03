/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.mount.cache.impl.tx;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by adetalhouet on 2017-02-02.
 */
public class CachedDOMReadWriteTransaction implements DOMDataReadWriteTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(CachedDOMReadWriteTransaction.class);

    private DOMDataReadOnlyTransaction readTransaction;
    private DOMDataWriteTransaction writeTransaction;

    public CachedDOMReadWriteTransaction(final DOMDataReadOnlyTransaction readTransaction,
                                         final DOMDataWriteTransaction writeTransaction) {
        this.readTransaction = readTransaction;
        this.writeTransaction = writeTransaction;
    }

    @Override
    public boolean cancel() {
        // TODO
        return false;
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        return writeTransaction.submit();
    }

    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(LogicalDatastoreType store, YangInstanceIdentifier path) {
        return readTransaction.read(store, path);
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(LogicalDatastoreType store, YangInstanceIdentifier path) {
        return readTransaction.exists(store, path);
    }

    @Override
    public void delete(LogicalDatastoreType store, YangInstanceIdentifier path) {
        writeTransaction.delete(store, path);
    }

    @Override
    public void put(LogicalDatastoreType store, YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        writeTransaction.put(store, path, data);
    }

    @Override
    public void merge(LogicalDatastoreType store, YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        writeTransaction.merge(store, path, data);
    }

    @Override
    public Object getIdentifier() {
        return this;
    }
}
