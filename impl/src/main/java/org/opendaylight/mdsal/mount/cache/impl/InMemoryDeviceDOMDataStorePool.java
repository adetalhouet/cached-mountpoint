/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.mount.cache.impl;

import com.google.common.base.Preconditions;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreFactory;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by adetalhouet on 2017-02-02.
 */
public class InMemoryDeviceDOMDataStorePool {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDeviceDOMDataStorePool.class);

    private ConcurrentHashMap<YangInstanceIdentifier, InMemoryDOMDataStore> pool;

    public InMemoryDeviceDOMDataStorePool() {
        LOG.info("InMemoryDeviceDOMDataStorePool is created");
        this.pool = new ConcurrentHashMap<>();
    }

    public InMemoryDOMDataStore getInMemoryDOMDataStore(final YangInstanceIdentifier path,
                                                        final SchemaContext schemaContext,
                                                        final LogicalDatastoreType store) {
        InMemoryDOMDataStore inMemoryDOMDataStore = this.pool.get(path);

        if (inMemoryDOMDataStore != null) {
            LOG.debug("InMemoryDOMDataStore exists for path=" + path.toString());
            return inMemoryDOMDataStore;
        } else {
            LOG.info("Create InMemoryDOMDataStore instance for cached-mountpoint {}" + path.toString());

            final SchemaService schemaService = new SchemaServiceFactory(schemaContext);
            inMemoryDOMDataStore = createDataStore(path, schemaService, store);
            Preconditions.checkArgument(inMemoryDOMDataStore != null);

            this.pool.put(path, inMemoryDOMDataStore);
            return inMemoryDOMDataStore;
        }
    }

    private InMemoryDOMDataStore createDataStore(final YangInstanceIdentifier path,
                                                 final SchemaService schemaService,
                                                 final LogicalDatastoreType store) {
        switch (store) {
            case OPERATIONAL: {
                return InMemoryDOMDataStoreFactory.create(path.toString() + "-DOM-OPER",
                        LogicalDatastoreType.OPERATIONAL, schemaService, true, null);
            }
            case CONFIGURATION: {
                return InMemoryDOMDataStoreFactory.create(path.toString() + "-DOM-CFG",
                        LogicalDatastoreType.CONFIGURATION, schemaService, true, null);
            }
        }
        return null;
    }

    private class SchemaServiceFactory implements SchemaService {

        private SchemaContext schemaContext;

        public SchemaServiceFactory(final SchemaContext schemaContext) {
            this.schemaContext = schemaContext;
        }

        @Override
        public void addModule(Module module) {
            // TODO
        }

        @Override
        public void removeModule(Module module) {
            // TODO
        }

        @Override
        public SchemaContext getSessionContext() {
            return schemaContext;
        }

        @Override
        public SchemaContext getGlobalContext() {
            return schemaContext;
        }

        @Override
        public ListenerRegistration<SchemaContextListener> registerSchemaContextListener(SchemaContextListener schemaContextListener) {
            schemaContextListener.onGlobalContextUpdated(getGlobalContext());

            return new ListenerRegistration<SchemaContextListener>() {
                @Override
                public void close() {
                    // TODO
                }

                @Override
                public SchemaContextListener getInstance() {
                    return schemaContextListener;
                }
            };
        }
    }
}