/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.mount.cache.impl;

import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;

/**
 * Created by adetalhouet on 2017-02-03.
 */
public class CachedMountPointId {

    private final String id;
    private final CachedSchemaRepository schemaRepository;
    private final InMemoryDOMDataStore inMemoryDOMDataStore;

    CachedMountPointId(final String id,
                       final CachedSchemaRepository schemaRepository,
                       final InMemoryDOMDataStore inMemoryDOMDataStore) {
        this.id = id;
        this.schemaRepository = schemaRepository;
        this.inMemoryDOMDataStore = inMemoryDOMDataStore;
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
}
