/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.mount.cache.impl;

import java.util.Collection;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;

/**
 * Created by adetalhouet on 2017-02-03.
 */
public class CachedMountPointId implements AutoCloseable {

    private final ObjectRegistration<DOMMountPoint> mountPointReg;
    private final Collection<ListenerRegistration<DOMDataTreeChangeListener>> listenerRegistrations;

    CachedMountPointId(final ObjectRegistration<DOMMountPoint> registration,
                       final Collection<ListenerRegistration<DOMDataTreeChangeListener>> listenerRegistrations) {

        this.mountPointReg = registration;
        this.listenerRegistrations = listenerRegistrations;
    }

    @Override
    public void close() throws Exception {
        if (mountPointReg != null) {
            mountPointReg.close();
        }
        if (listenerRegistrations != null && !listenerRegistrations.isEmpty()) {
            listenerRegistrations.forEach(ListenerRegistration::close);
        }
    }
}
