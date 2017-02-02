/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.mount.cache.impl;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.mount.cache.impl.tx.CachedDOMDataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.cached.mountpoint.rev170201.CachedMounpointNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by adetalhouet on 2017-02-02.
 */
public class CachedMountPointTopology implements DataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(CachedMountPointTopology.class);

    private static final String TOPOLOGY_NAME = "cached-mountpoint";

    private static final InstanceIdentifier<Topology> TOPOLOGY_PATH = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(TOPOLOGY_NAME)));

    private static final Executor CLIENT_FUTURE_CALLBACK_EXECUTOR =
            SpecialExecutors.newBlockingBoundedCachedThreadPool(20, 1000, "CachedMoutpointCommitFutures");


    private final DOMMountPointService service;
    private final BindingNormalizedNodeSerializer codec;
    private final InMemoryDeviceDOMDataStorePool pool;
    private final DataBroker broker;

    private ListenerRegistration datastoreListenerRegistration;

    private final Map<NodeId, CachedSchemaRepository> cachedMountPoints = new ConcurrentHashMap<>();


    public CachedMountPointTopology(final DataBroker broker,
                                    final DOMMountPointService service,
                                    final BindingNormalizedNodeSerializer codec,
                                    final InMemoryDeviceDOMDataStorePool pool) {
        this.broker = broker;
        this.service = service;
        this.codec = codec;
        this.pool = pool;
    }

    /**
     * Invoke by blueprint
     */
    public void init() {
        final WriteTransaction wtx = broker.newWriteOnlyTransaction();
        initTopology(wtx, LogicalDatastoreType.CONFIGURATION);
        initTopology(wtx, LogicalDatastoreType.OPERATIONAL);
        Futures.addCallback(wtx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                LOG.info("topology initialization successful");
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Unable to initialize netconf-topology, {}", t);
            }
        });

        LOG.info("Registering datastore listener");
        datastoreListenerRegistration =
                broker.registerDataTreeChangeListener(
                        new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                                TOPOLOGY_PATH.child(Node.class)), this);

    }

    private void initTopology(final WriteTransaction wtx, final LogicalDatastoreType datastoreType) {
        final NetworkTopology networkTopology = new NetworkTopologyBuilder().build();
        final InstanceIdentifier<NetworkTopology> networkTopologyId = InstanceIdentifier.builder(NetworkTopology.class).build();
        wtx.merge(datastoreType, networkTopologyId, networkTopology);
        final Topology topology = new TopologyBuilder().setTopologyId(new TopologyId(TOPOLOGY_NAME)).build();
        wtx.merge(datastoreType, networkTopologyId.child(Topology.class, new TopologyKey(new TopologyId(TOPOLOGY_NAME))), topology);
    }

    /**
     * Invoke by blueprint
     */
    public void close() {
        if (datastoreListenerRegistration != null) {
            datastoreListenerRegistration.close();
        }
    }

    public static NodeId getNodeId(final InstanceIdentifier.PathArgument pathArgument) {
        if (pathArgument instanceof InstanceIdentifier.IdentifiableItem<?, ?>) {

            final Identifier key = ((InstanceIdentifier.IdentifiableItem) pathArgument).getKey();
            if (key instanceof NodeKey) {
                return ((NodeKey) key).getNodeId();
            }
        }
        throw new IllegalStateException("Unable to create NodeId from: " + pathArgument);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Node>> collection) {
        for (DataTreeModification<Node> change : collection) {
            final DataObjectModification<Node> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    if (cachedMountPoints.containsKey(getNodeId(rootNode.getIdentifier()))) {
                        LOG.warn("Cached mount point{{}} was already configured - ignore request", getNodeId(rootNode.getIdentifier()));
                        continue;
                    }
                    createCachedMountPoint(getNodeId(rootNode.getIdentifier()), rootNode.getDataAfter());
                    break;
                case DELETE:
                    deleteCachedMountPoint(getNodeId(rootNode.getIdentifier()));
                    break;
            }
        }
    }

    private void createCachedMountPoint(final NodeId nodeId, final Node node) {
        final CachedMounpointNode cachedMounpointNode = node.getAugmentation(CachedMounpointNode.class);

        final Collection<String> caps = cachedMounpointNode.getYangModuleCapabilities().getCapability();
        if (caps == null || caps.isEmpty()) {
            LOG.error("Trying to setup a cached mount point without any capabilities - this is wrong!");
            return;
        }
        final String cacheDirectoryName = cachedMounpointNode.getSchemaCacheDirectory();
        final CachedSchemaRepository cachedSchemaRepository = cachedMountPoints
                .computeIfAbsent(nodeId, k -> CachedSchemaRepository.newInstance(nodeId, cacheDirectoryName, caps));

        NodeKey nodeKey = new NodeKey(nodeId);
        YangInstanceIdentifier yangInstanceIdentifier = codec.toYangInstanceIdentifier(TOPOLOGY_PATH.child(Node.class, nodeKey));

        final DOMMountPointService.DOMMountPointBuilder cachedMountPointBuilder = service.createMountPoint(yangInstanceIdentifier);

        final SchemaContext schemaContext = cachedSchemaRepository.getSchemaContext();

        cachedMountPointBuilder.addService(DOMDataBroker.class, new CachedDOMDataBroker(nodeId, schemaContext, pool, CLIENT_FUTURE_CALLBACK_EXECUTOR));
        cachedMountPointBuilder.addInitialSchemaContext(schemaContext);
        cachedMountPointBuilder.register();
    }

    private void deleteCachedMountPoint(NodeId nodeId) {
        // TODO delete mount point
    }
}
