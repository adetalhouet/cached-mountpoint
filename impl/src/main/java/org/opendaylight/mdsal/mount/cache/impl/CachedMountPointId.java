/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.mount.cache.impl;

import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.yangtools.concepts.ObjectRegistration;

/**
 * Created by adetalhouet on 2017-02-03.
 */
public class CachedMountPointId {

    private final String id;
    private final CachedSchemaRepository schemaRepository;
    private final InMemoryDOMDataStore inMemoryDOMDataStore;
    private final ObjectRegistration<DOMMountPoint> registration;

    CachedMountPointId(final String id,
                       final CachedSchemaRepository schemaRepository,
                       final InMemoryDOMDataStore inMemoryDOMDataStore,
                       final ObjectRegistration<DOMMountPoint> registration) {
        this.id = id;
        this.schemaRepository = schemaRepository;
        this.inMemoryDOMDataStore = inMemoryDOMDataStore;
        this.registration = registration;
    }

    public String getId() {
        return id;
    }

    public CachedSchemaRepository getSchemaRepository() {
        return schemaRepository;
    }

    public InMemoryDOMDataStore getInMemoryDOMDataStore() {
        return inMemoryDOMDataStore;
    }

    public ObjectRegistration<DOMMountPoint> getRegistration() {
        return registration;
    }
}
