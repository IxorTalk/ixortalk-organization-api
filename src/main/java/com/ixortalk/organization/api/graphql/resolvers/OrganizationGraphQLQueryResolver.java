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
package com.ixortalk.organization.api.graphql.resolvers;

import com.ixortalk.organization.api.domain.Organization;
import com.ixortalk.organization.api.graphql.type.OrganizationsContentPage;
import com.ixortalk.organization.api.graphql.util.GraphQLUtils;
import com.ixortalk.organization.api.rest.OrganizationRestResource;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.PageRequest;

import javax.inject.Inject;

public class OrganizationGraphQLQueryResolver extends GraphQLQueryResolver {

    @Inject
    private OrganizationRestResource organizationRestResource;

    public OrganizationsContentPage getOrganizationsPage(int page, int size, String sort, String direction, String filter) {

        Predicate predicate = querydslService.buildPredicate(Organization.class, filter);
        PageRequest pageRequest = GraphQLUtils.buildPageRequest(page, size, sort, direction);

        return new OrganizationsContentPage(organizationRestResource.findAll(predicate, pageRequest));
    }
}