/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.mount.cache.impl;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.mount.cache.impl.util.TopologyHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.cached.mount.point.rev170201.CachedMountPointNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by adetalhouet on 2017-02-02.
 */
public class CachedMountPointTopology implements DataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(CachedMountPointTopology.class);

    private static final Executor CLIENT_FUTURE_CALLBACK_EXECUTOR =
            SpecialExecutors.newBlockingBoundedCachedThreadPool(20, 1000, "CachedMoutpointCommitFutures");

    private final DOMMountPointService service;
    private final BindingNormalizedNodeSerializer codec;
    private final DataBroker broker;

    private final Map<String, CachedMountPointId> cachedMountPoints = new ConcurrentHashMap<>();

    private ListenerRegistration datastoreListenerRegistration;

    public CachedMountPointTopology(final DataBroker broker,
                                    final DOMMountPointService service,
                                    final BindingNormalizedNodeSerializer codec) {
        this.broker = broker;
        this.service = service;
        this.codec = codec;
    }

    /**
     * Invoke by blueprint
     */
    public void init() {
        // Init datastores
        final WriteTransaction wtx = broker.newWriteOnlyTransaction();
        TopologyHelper.initTopology(wtx);
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

        // Register listener
        datastoreListenerRegistration =
                broker.registerDataTreeChangeListener(
                        new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                                TopologyHelper.CACHED_MOUNT_POINT_TOPOLOGY.child(Node.class)), this);
    }

    /**
     * Invoke by blueprint
     */
    public void close() {
        if (datastoreListenerRegistration != null) {
            datastoreListenerRegistration.close();
        }
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Node>> collection) {
        for (DataTreeModification<Node> change : collection) {
            final DataObjectModification<Node> rootNode = change.getRootNode();
            final String nodeId = TopologyHelper.getNodeId(rootNode.getIdentifier());
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    if (cachedMountPoints.containsKey(nodeId)) {
                        LOG.warn("Cached mount point{{}} was already configured - ignore request", nodeId);
                        continue;
                    }
                    createCachedMountPoint(nodeId, rootNode.getDataAfter());
                    break;
                case DELETE:
                    deleteCachedMountPoint(nodeId);
                    break;
            }
        }
    }

    private Function<QName, YangInstanceIdentifier> fromQnameToYiid = YangInstanceIdentifier::of;

    private void createCachedMountPoint(final String nodeId, final Node node) {

        CachedMountPointNode cachedMountPointNode = node.getAugmentation(CachedMountPointNode.class);

        if (!cachedMountPoints.containsKey(nodeId)) {
            final CachedSchemaRepository cachedSchemaRepository = setupSchemaRepository(nodeId, cachedMountPointNode);
            final SchemaContext schemaContext = cachedSchemaRepository.getSchemaContext();
            final YangInstanceIdentifier rootNode = mountPointPath(nodeId);
            final InMemoryDOMDataStore inMemoryDOMDataStore = InMemoryDOMDataStoreFactory.newInstance(rootNode,
                    schemaContext, LogicalDatastoreType.CONFIGURATION);
            final CachedDOMDataBroker domDataBroker = new CachedDOMDataBroker(nodeId, inMemoryDOMDataStore,
                    CLIENT_FUTURE_CALLBACK_EXECUTOR);

            // Looks like one can't register a DTCL on mount point with current backend implementation
//            final Collection<ListenerRegistration<CachedDOMDataTreeChangeListener>> regs =
//                    registerDataTreeChangeListener(nodeId, cachedSchemaRepository.qNames(), domDataBroker);

            final CachedMountPointId mountPointId = new CachedMountPointId(nodeId, cachedSchemaRepository, inMemoryDOMDataStore);
            cachedMountPoints.put(nodeId, mountPointId);

            final DOMMountPointService.DOMMountPointBuilder cachedMountPointBuilder = service.createMountPoint(rootNode);
            cachedMountPointBuilder.addService(DOMDataBroker.class, domDataBroker);
            cachedMountPointBuilder.addInitialSchemaContext(schemaContext);
            // TODO save & close the reg
            cachedMountPointBuilder.register();
        } else {
            LOG.warn("{}: Mount point already exist", nodeId);
        }
    }

    private CachedSchemaRepository setupSchemaRepository(final String nodeId, final CachedMountPointNode cachedMountPointNode) {
        final Collection<String> caps = cachedMountPointNode.getYangModuleCapabilities().getCapability();
        if (caps == null || caps.isEmpty()) {
            throw new IllegalStateException("Trying to setup a cached mount point without any capabilities - this is wrong!");
        }
        final String cacheDirectoryName = cachedMountPointNode.getSchemaCacheDirectory();
        return CachedSchemaRepository.newInstance(nodeId, cacheDirectoryName, caps);
    }

    private YangInstanceIdentifier mountPointPath(final String nodeId) {
        NodeKey nodeKey = new NodeKey(new NodeId(nodeId));
        return codec.toYangInstanceIdentifier(TopologyHelper.CACHED_MOUNT_POINT_TOPOLOGY.child(Node.class, nodeKey));
    }

    private Collection<ListenerRegistration<CachedDOMDataTreeChangeListener>> registerDataTreeChangeListener(final String nodeId,
                                                                                                             final List<QName> qNames,
                                                                                                             final CachedDOMDataBroker domDataBroker) {
        final Collection<ListenerRegistration<CachedDOMDataTreeChangeListener>> regs = Lists.newArrayList();
        final Collection<YangInstanceIdentifier> yiids = Lists.newArrayList();
        qNames.forEach(q -> yiids.add(fromQnameToYiid.apply(q)));

        yiids.forEach(qName -> {
            LOG.info("{}: Registering DTCL for path={}", nodeId, qName);
            final DOMDataTreeIdentifier id = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, qName);
            regs.add(domDataBroker.registerDataTreeChangeListener(id, new CachedDOMDataTreeChangeListener()));
        });
        return regs;
    }

    private void deleteCachedMountPoint(String nodeId) {
        // TODO delete mount point
    }
}
