/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.mount.cache.impl;

import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by adetalhouet on 2017-02-02.
 */
// FIXME potentially useless
public class CachedMountPointListener implements MountProvisionListener {

    private static final Logger LOG = LoggerFactory.getLogger(CachedMountPointListener.class);

    public CachedMountPointListener(final DOMMountPointService service) {
        service.registerProvisionListener(this);
    }

    @Override
    public void onMountPointCreated(final YangInstanceIdentifier yangInstanceIdentifier) {
        LOG.debug("MountPoint for path={} created", yangInstanceIdentifier);
    }

    @Override
    public void onMountPointRemoved(final YangInstanceIdentifier yangInstanceIdentifier) {
        LOG.debug("MountPoint for path={} removed", yangInstanceIdentifier);
    }
}
