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

import com.ixortalk.organization.api.domain.*;
import com.ixortalk.organization.api.graphql.querydsl.ValidateFilteredOnOrganizationIdVisitor;
import com.ixortalk.organization.api.rest.OrganizationRestResource;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.NumberPath;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.core.GrantedAuthority;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;
import java.util.Set;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;

@Named
public class SecurityService {

    @Inject
    private UserEmailProvider userEmailProvider;

    @Inject
    private OrganizationRestResource organizationRestResource;

    public final static String ROLE_ADMIN = "ROLE_ADMIN";

    public boolean isAdmin() {
        return getPrincipalAuthorities().stream().anyMatch(role -> role.equals(ROLE_ADMIN));
    }

    public Set<String> getPrincipalAuthorities() {
        return getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(toSet());
    }

    public boolean isCurrentUser(Optional<User> user) {
        return user.map(this::isCurrentUser).orElse(false);
    }

    public boolean isCurrentUser(User user) {
        return userEmailProvider.getCurrentUsersEmail().map(email -> user.getLogin().equals(email)).orElse(false);
    }

    public boolean isCurrentUser(String login) {
        return userEmailProvider.getCurrentUsersEmail().map(email -> login.toLowerCase().equals(email)).orElse(false);
    }

    public boolean isAdminOfOrganization(Long organizationId) {
        return isAdminOfOrganization(organizationRestResource.findById(organizationId));
    }

    public boolean isAdminOfOrganization(Optional<Organization> organization) {
        return organization.map(this::isAdminOfOrganization).orElseThrow(ResourceNotFoundException::new);
    }

    public boolean isAdminOfOrganization(Organization organization) {
        return userEmailProvider.getCurrentUsersEmail().map(organization::hasAdminAccess).orElse(false);
    }

    public boolean hasAdminAccess(User user) {
        //TODO investigate: line below makes the user-deletion tests fail because of https://stackoverflow.com/a/34843369
        //return organizationRestResource.findByUsers(user).map(this::isAdminOfOrganization).orElse(true);
        return userEmailProvider.getCurrentUsersEmail().map(
                email -> organizationRestResource.hasAdminAccessToUser(email, user).isPresent()
        ).orElse(false);
    }

    public boolean hasAdminAccess(Optional<Role> role) {
        return role.map(this::hasAdminAccess).orElseThrow(ResourceNotFoundException::new);
    }

    public boolean hasAdminAccess(Role role) {
        return organizationRestResource.findByRoles(role).map(this::isAdminOfOrganization).orElse(true);
    }

    public boolean roleForOrganizationsWithOrganizationAdminAccess(Predicate predicate ) {
        return forOrganizationsWithOrganizationAdminAccess(predicate, QRole.role1.organizationId);
    }

    public boolean userForOrganizationsWithOrganizationAdminAccess(Predicate predicate ) {
        return forOrganizationsWithOrganizationAdminAccess(predicate, QUser.user.organizationId);
    }

    private boolean forOrganizationsWithOrganizationAdminAccess(Predicate predicate, NumberPath<Long> organizationPath) {
        return ofNullable(predicate.accept(
                new ValidateFilteredOnOrganizationIdVisitor(organizationPath), this::isAdminOfOrganization)
        ).orElse(false);
    }
}
