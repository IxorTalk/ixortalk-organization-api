/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-present IxorTalk CVBA
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.ixortalk.organization.api.service;

import com.ixortalk.organization.api.domain.EnhancedUserProjection;
import com.ixortalk.organization.api.domain.User;
import com.ixortalk.organization.api.rest.OrganizationRestResource;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;

import javax.inject.Inject;
import javax.inject.Named;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.springframework.hateoas.CollectionModel.wrap;

@Named
public class OrganizationService {

    @Inject
    private OrganizationRestResource organizationRestResource;

    @Inject
    private ProjectionFactory projectionFactory;

    public CollectionModel<EntityModel<EnhancedUserProjection>> getAdminUsers(Long organizationId) {
        return organizationRestResource.findById(organizationId)
                .map(organization ->
                        wrap(
                                organization
                                        .getUsers()
                                        .stream()
                                        .filter(User::isAdmin)
                                        .map(user -> projectionFactory.createProjection(EnhancedUserProjection.class, user))
                                        .sorted(comparing(EnhancedUserProjection::getLogin))
                                        .collect(toList())))
                .orElseThrow(ResourceNotFoundException::new);
    }
}
