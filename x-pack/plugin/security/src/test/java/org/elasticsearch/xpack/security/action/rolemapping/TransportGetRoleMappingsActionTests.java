/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.security.action.rolemapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.core.security.action.rolemapping.GetRoleMappingsRequest;
import org.elasticsearch.xpack.core.security.action.rolemapping.GetRoleMappingsResponse;
import org.elasticsearch.xpack.core.security.authc.support.mapper.ExpressionRoleMapping;
import org.elasticsearch.xpack.security.authc.support.mapper.NativeRoleMappingStore;
import org.hamcrest.Matchers;
import org.junit.Before;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class TransportGetRoleMappingsActionTests extends ESTestCase {

    private NativeRoleMappingStore store;
    private TransportGetRoleMappingsAction action;
    private AtomicReference<Set<String>> namesRef;
    private List<ExpressionRoleMapping> result;

    @Before
    public void setupMocks() {
        store = mock(NativeRoleMappingStore.class);
        TransportService transportService = new TransportService(Settings.EMPTY, null, null,
                TransportService.NOOP_TRANSPORT_INTERCEPTOR, x -> null, null, Collections.emptySet());
        action = new TransportGetRoleMappingsAction(Settings.EMPTY, mock(ThreadPool.class), mock(ActionFilters.class),
            transportService, store);

        namesRef = new AtomicReference<>(null);
        result = Collections.emptyList();

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            assert args.length == 2;
            namesRef.set((Set<String>) args[0]);
            ActionListener<List<ExpressionRoleMapping>> listener = (ActionListener) args[1];
            listener.onResponse(result);
            return null;
        }).when(store).getRoleMappings(any(Set.class), any(ActionListener.class));
    }

    public void testGetSingleRole() throws Exception {
        final PlainActionFuture<GetRoleMappingsResponse> future = new PlainActionFuture<>();
        final GetRoleMappingsRequest request = new GetRoleMappingsRequest();
        request.setNames("everyone");

        final ExpressionRoleMapping mapping = mock(ExpressionRoleMapping.class);
        result = Collections.singletonList(mapping);
        action.doExecute(request, future);
        assertThat(future.get(), notNullValue());
        assertThat(future.get().mappings(), arrayContaining(mapping));
        assertThat(namesRef.get(), containsInAnyOrder("everyone"));
    }

    public void testGetMultipleNamedRoles() throws Exception {
        final PlainActionFuture<GetRoleMappingsResponse> future = new PlainActionFuture<>();
        final GetRoleMappingsRequest request = new GetRoleMappingsRequest();
        request.setNames("admin", "engineering", "sales", "finance");

        final ExpressionRoleMapping mapping1 = mock(ExpressionRoleMapping.class);
        final ExpressionRoleMapping mapping2 = mock(ExpressionRoleMapping.class);
        final ExpressionRoleMapping mapping3 = mock(ExpressionRoleMapping.class);
        result = Arrays.asList(mapping1, mapping2, mapping3);

        action.doExecute(request, future);

        final GetRoleMappingsResponse response = future.get();
        assertThat(response, notNullValue());
        assertThat(response.mappings(), arrayContainingInAnyOrder(mapping1, mapping2, mapping3));
        assertThat(namesRef.get(), containsInAnyOrder("admin", "engineering", "sales", "finance"));
    }

    public void testGetAllRoles() throws Exception {
        final PlainActionFuture<GetRoleMappingsResponse> future = new PlainActionFuture<>();
        final GetRoleMappingsRequest request = new GetRoleMappingsRequest();
        request.setNames(Strings.EMPTY_ARRAY);

        final ExpressionRoleMapping mapping1 = mock(ExpressionRoleMapping.class);
        final ExpressionRoleMapping mapping2 = mock(ExpressionRoleMapping.class);
        final ExpressionRoleMapping mapping3 = mock(ExpressionRoleMapping.class);
        result = Arrays.asList(mapping1, mapping2, mapping3);

        action.doExecute(request, future);

        final GetRoleMappingsResponse response = future.get();
        assertThat(response, notNullValue());
        assertThat(response.mappings(), arrayContainingInAnyOrder(mapping1, mapping2, mapping3));
        assertThat(namesRef.get(), Matchers.nullValue(Set.class));
    }

}