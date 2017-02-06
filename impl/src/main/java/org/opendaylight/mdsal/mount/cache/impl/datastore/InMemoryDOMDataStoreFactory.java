/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.mount.cache.impl.datastore;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
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
public final class InMemoryDOMDataStoreFactory {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDOMDataStoreFactory.class);

    private InMemoryDOMDataStoreFactory() {
        throw new AssertionError("Utility class");
    }

    public static InMemoryDOMDataStore newInstance(final YangInstanceIdentifier path,
                                                   final SchemaContext schemaContext,
                                                   final LogicalDatastoreType store) {

        LOG.info("Create InMemoryDOMDataStore instance for cached-mountpoint {}" + path.toString());
        final SchemaService schemaService = new SchemaServiceFactory(schemaContext);
        final InMemoryDOMDataStore inMemoryDOMDataStore = createDataStore(path, schemaService, store);
        Preconditions.checkArgument(inMemoryDOMDataStore != null);
        return inMemoryDOMDataStore;

    }

    private static InMemoryDOMDataStore createDataStore(final YangInstanceIdentifier path,
                                                        final SchemaService schemaService,
                                                        final LogicalDatastoreType store) {
        switch (store) {
            case OPERATIONAL: {
                return org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreFactory.create(path.toString() + "-DOM-OPER",
                        LogicalDatastoreType.OPERATIONAL, schemaService, true, null);
            }
            case CONFIGURATION: {
                return org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreFactory.create(path.toString() + "-DOM-CFG",
                        LogicalDatastoreType.CONFIGURATION, schemaService, true, null);
            }
        }
        return null;
    }

    private static class SchemaServiceFactory implements SchemaService {

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