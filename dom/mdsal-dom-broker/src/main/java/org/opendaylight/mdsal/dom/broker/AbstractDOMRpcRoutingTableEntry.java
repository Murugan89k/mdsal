/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.dom.broker;

import org.opendaylight.mdsal.dom.api.DOMRpcException;
import org.opendaylight.mdsal.dom.api.DOMRpcImplementation;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

abstract class AbstractDOMRpcRoutingTableEntry {
    private final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> impls;
    private final SchemaPath schemaPath;

    protected AbstractDOMRpcRoutingTableEntry(final SchemaPath schemaPath, final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> impls) {
        this.schemaPath = Preconditions.checkNotNull(schemaPath);
        this.impls = Preconditions.checkNotNull(impls);
    }

    protected final SchemaPath getSchemaPath() {
        return schemaPath;
    }

    protected final List<DOMRpcImplementation> getImplementations(final YangInstanceIdentifier context) {
        return impls.get(context);
    }

    final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> getImplementations() {
        return impls;
    }

    public boolean containsContext(final YangInstanceIdentifier contextReference) {
        return impls.containsKey(contextReference);
    }

    final Set<YangInstanceIdentifier> registeredIdentifiers() {
        return impls.keySet();
    }

    /**
     *
     * @param implementation
     * @param newRpcs List of new RPCs, must be mutable
     * @return
     */
    final AbstractDOMRpcRoutingTableEntry add(final DOMRpcImplementation implementation, final List<YangInstanceIdentifier> newRpcs) {
        final Builder<YangInstanceIdentifier, List<DOMRpcImplementation>> vb = ImmutableMap.builder();
        for (final Entry<YangInstanceIdentifier, List<DOMRpcImplementation>> ve : impls.entrySet()) {
            if (newRpcs.remove(ve.getKey())) {
                final ArrayList<DOMRpcImplementation> i = new ArrayList<>(ve.getValue().size() + 1);
                i.addAll(ve.getValue());
                i.add(implementation);
                vb.put(ve.getKey(), i);
            } else {
                vb.put(ve);
            }
        }
        for(final YangInstanceIdentifier ii : newRpcs) {
            final ArrayList<DOMRpcImplementation> impl = new ArrayList<>(1);
            impl.add(implementation);
            vb.put(ii,impl);
        }

        return newInstance(vb.build());
    }

    final AbstractDOMRpcRoutingTableEntry remove(final DOMRpcImplementation implementation, final List<YangInstanceIdentifier> removed) {
        final Builder<YangInstanceIdentifier, List<DOMRpcImplementation>> vb = ImmutableMap.builder();
        for (final Entry<YangInstanceIdentifier, List<DOMRpcImplementation>> ve : impls.entrySet()) {
            if (removed.remove(ve.getKey())) {
                final ArrayList<DOMRpcImplementation> i = new ArrayList<>(ve.getValue());
                i.remove(implementation);
                // We could trimToSize(), but that may perform another copy just to get rid
                // of a single element. That is probably not worth the trouble.
                if (!i.isEmpty()) {
                    vb.put(ve.getKey(), i);
                }
            } else {
                vb.put(ve);
            }
        }

        final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> v = vb.build();
        return v.isEmpty() ? null : newInstance(v);
    }

    protected abstract CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final NormalizedNode<?, ?> input);
    protected abstract AbstractDOMRpcRoutingTableEntry newInstance(final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> impls);
}
