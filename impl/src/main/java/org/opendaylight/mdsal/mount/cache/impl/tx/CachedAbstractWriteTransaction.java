/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.mount.cache.impl.tx;

import com.google.common.base.Preconditions;
import org.opendaylight.mdsal.mount.cache.impl.InMemoryDeviceDOMDataStorePool;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Created by adetalhouet on 2017-02-02.
 */
abstract class CachedAbstractWriteTransaction {

    NodeId nodeId;
    SchemaContext schemaContext;
    InMemoryDeviceDOMDataStorePool pool;

    CachedAbstractWriteTransaction(final NodeId nodeId,
                                   final SchemaContext schemaContext,
                                   final InMemoryDeviceDOMDataStorePool pool) {
        this.nodeId = nodeId;
        this.schemaContext = Preconditions.checkNotNull(schemaContext);
        this.pool = Preconditions.checkNotNull(pool);
    }
}
