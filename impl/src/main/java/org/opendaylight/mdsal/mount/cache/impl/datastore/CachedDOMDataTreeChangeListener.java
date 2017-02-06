/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.mount.cache.impl.datastore;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by adetalhouet on 2017-02-03.
 */
public class CachedDOMDataTreeChangeListener implements DOMDataTreeChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(CachedDOMDataTreeChangeListener.class);

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeCandidate> collection) {
        LOG.info("{}", collection);
    }
}
