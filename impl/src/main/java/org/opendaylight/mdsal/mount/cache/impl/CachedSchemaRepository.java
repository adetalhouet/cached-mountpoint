/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.mount.cache.impl;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import java.io.File;
import java.util.Collection;
import java.util.List;
import org.opendaylight.mdsal.mount.cache.impl.util.SourceIdentifierHelper;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaContextFactory;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaResolutionException;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceFilter;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.util.FilesystemSchemaSourceCache;
import org.opendaylight.yangtools.yang.parser.repo.SharedSchemaRepository;
import org.opendaylight.yangtools.yang.parser.util.TextToASTTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by adetalhouet on 2017-02-02.
 */
class CachedSchemaRepository {

    private static final Logger LOG = LoggerFactory.getLogger(CachedSchemaRepository.class);

    public static final String DEFAULT_CACHED_MOUNT_POINT_DIRECTORY = "cache/cached-mountpoint";

    private SchemaContextFactory schemaContextFactory;
    private SchemaContext schemaContext;
    private List<QName> qNameSet;

    private CachedSchemaRepository(final SchemaContextFactory schemaContextFactory, final Collection<String> caps) {
        this.schemaContextFactory = schemaContextFactory;
        this.qNameSet = Lists.newArrayList();
        this.schemaContext = setSchemaContext(caps, qNameSet);
    }

    static CachedSchemaRepository newInstance(final String nodeId, final String cacheDirectoryName, final Collection<String> caps) {

        final SharedSchemaRepository schemaRegistry = new SharedSchemaRepository(cacheDirectoryName);
        final SchemaContextFactory schemaContextFactory = schemaRegistry.createSchemaContextFactory(SchemaSourceFilter.ALWAYS_ACCEPT);

        schemaRegistry.registerSchemaSourceListener(TextToASTTransformer.create(schemaRegistry, schemaRegistry));

        final String relativeSchemaCacheDirectory = DEFAULT_CACHED_MOUNT_POINT_DIRECTORY + File.separator + cacheDirectoryName;
        final FilesystemSchemaSourceCache filesystemSchemaSourceCache = new FilesystemSchemaSourceCache<>(schemaRegistry,
                YangTextSchemaSource.class, new File(relativeSchemaCacheDirectory));
        schemaRegistry.registerSchemaSourceListener(filesystemSchemaSourceCache);
        LOG.info("{}: Cached mount point will use schema cache directory {} ", nodeId, relativeSchemaCacheDirectory);

        return new CachedSchemaRepository(schemaContextFactory, caps);
    }

    private SchemaContext setSchemaContext(final Collection<String> caps, final Collection<QName> qNameSet) {
        final CheckedFuture<SchemaContext, SchemaResolutionException> schemaBuilderFuture =
                schemaContextFactory.createSchemaContext(SourceIdentifierHelper.fromStrings(caps, qNameSet));
        try {
            return schemaBuilderFuture.checkedGet();
        } catch (SchemaResolutionException exception) {
            LOG.error("Failed to setup SchemaContext", exception);
            return null;
        }
    }

    SchemaContext getSchemaContext() {
        return this.schemaContext;
    }

    List<QName> qNames() {
        return this.qNameSet;
    }
}
