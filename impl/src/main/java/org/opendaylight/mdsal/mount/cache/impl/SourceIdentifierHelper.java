/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.mount.cache.impl;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by adetalhouet on 2017-02-02.
 */
// FIXME partially duplicate of NetconfSessionPreferences
public final class SourceIdentifierHelper {

    private static final Logger LOG = LoggerFactory.getLogger(SourceIdentifierHelper.class);

    private static final ParameterMatcher MODULE_PARAM = new ParameterMatcher("module=");
    private static final ParameterMatcher REVISION_PARAM = new ParameterMatcher("revision=");
    private static final ParameterMatcher BROKEN_REVISON_PARAM = new ParameterMatcher("amp;revision=");
    private static final Splitter AMP_SPLITTER = Splitter.on('&');
    private static final Predicate<String> CONTAINS_REVISION = input -> input.contains("revision=");

    private static final Function<QName, SourceIdentifier> QNAME_TO_SOURCE_ID_FUNCTION = input -> RevisionSourceIdentifier
            .create(input.getLocalName(), Optional.fromNullable(input.getFormattedRevision()));

    private SourceIdentifierHelper() {
        throw new AssertionError("Utility class");
    }

    private static final class ParameterMatcher {
        private final Predicate<String> predicate;
        private final int skipLength;

        ParameterMatcher(final String name) {
            predicate = input -> input.startsWith(name);

            this.skipLength = name.length();
        }

        private String from(final Iterable<String> params) {
            final Optional<String> o = Iterables.tryFind(params, predicate);
            if (!o.isPresent()) {
                return null;
            }

            return o.get().substring(skipLength);
        }
    }

    static Collection<SourceIdentifier> fromStrings(final Collection<String> capabilities) {

        final Collection<QName> moduleBasedCaps = Lists.newArrayList();

        final Set<String> nonModuleCaps = Sets.newHashSet(capabilities);

        for (final String capability : capabilities) {
            final int qmark = capability.indexOf('?');
            if (qmark == -1) {
                continue;
            }

            final String namespace = capability.substring(0, qmark);
            final Iterable<String> queryParams = AMP_SPLITTER.split(capability.substring(qmark + 1));
            final String moduleName = MODULE_PARAM.from(queryParams);
            if (Strings.isNullOrEmpty(moduleName)) {
                continue;
            }

            String revision = REVISION_PARAM.from(queryParams);
            if (!Strings.isNullOrEmpty(revision)) {
                addModuleQName(moduleBasedCaps, nonModuleCaps, capability, cachedQName(namespace, revision, moduleName));
                continue;
            }

            /*
             * We have seen devices which mis-escape revision, but the revision may not
             * even be there. First check if there is a substring that matches revision.
             */
            if (StreamSupport.stream(queryParams.spliterator(), false).anyMatch(CONTAINS_REVISION::apply)) {

                LOG.debug("Mount point was not reporting revision correctly, trying to get amp;revision=");
                revision = BROKEN_REVISON_PARAM.from(queryParams);
                if (Strings.isNullOrEmpty(revision)) {
                    LOG.warn("Mount point returned revision incorrectly escaped for {}, ignoring it", capability);
                    addModuleQName(moduleBasedCaps, nonModuleCaps, capability, cachedQName(namespace, moduleName));
                } else {
                    addModuleQName(moduleBasedCaps, nonModuleCaps, capability, cachedQName(namespace, revision, moduleName));
                }
                continue;
            }

            // Fallback, no revision provided for module
            addModuleQName(moduleBasedCaps, nonModuleCaps, capability, cachedQName(namespace, moduleName));
        }

        return Collections2.transform(moduleBasedCaps, QNAME_TO_SOURCE_ID_FUNCTION);
    }

    private static void addModuleQName(final Collection<QName> moduleBasedCaps, final Set<String> nonModuleCaps, final String capability, final QName qName) {
        moduleBasedCaps.add(qName);
        nonModuleCaps.remove(capability);
    }

    private static QName cachedQName(final String namespace, final String revision, final String moduleName) {
        return QName.create(namespace, revision, moduleName).intern();
    }

    private static QName cachedQName(final String namespace, final String moduleName) {
        return QName.create(URI.create(namespace), null, moduleName).withoutRevision().intern();
    }
}
