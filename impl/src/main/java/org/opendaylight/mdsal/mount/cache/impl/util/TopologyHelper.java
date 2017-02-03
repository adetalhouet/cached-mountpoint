/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.mount.cache.impl.util;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Created by adetalhouet on 2017-02-03.
 */
public final class TopologyHelper {

    private static final String TOPOLOGY_NAME = "cached-mount-point";

    public static final InstanceIdentifier<Topology> CACHED_MOUNT_POINT_TOPOLOGY = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(TOPOLOGY_NAME)));

    private TopologyHelper() {
        throw new AssertionError("Utility class");
    }

    public static void initTopology(final WriteTransaction wtx) {
        for (LogicalDatastoreType datastoreType : LogicalDatastoreType.values()) {
            final NetworkTopology networkTopology = new NetworkTopologyBuilder().build();
            final InstanceIdentifier<NetworkTopology> networkTopologyId = InstanceIdentifier.builder(NetworkTopology.class).build();
            wtx.merge(datastoreType, networkTopologyId, networkTopology);
            final Topology topology = new TopologyBuilder().setTopologyId(new TopologyId(TOPOLOGY_NAME)).build();
            wtx.merge(datastoreType, networkTopologyId.child(Topology.class, new TopologyKey(new TopologyId(TOPOLOGY_NAME))), topology);
        }
    }

    public static String getNodeId(final InstanceIdentifier.PathArgument pathArgument) {
        if (pathArgument instanceof InstanceIdentifier.IdentifiableItem<?, ?>) {

            final Identifier key = ((InstanceIdentifier.IdentifiableItem) pathArgument).getKey();
            if (key instanceof NodeKey) {
                return ((NodeKey) key).getNodeId().getValue();
            }
        }
        throw new IllegalStateException("Unable to create NodeId from: " + pathArgument);
    }
}
