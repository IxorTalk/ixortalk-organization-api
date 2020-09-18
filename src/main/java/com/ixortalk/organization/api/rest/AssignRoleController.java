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
package com.ixortalk.organization.api.rest;

import com.ixortalk.autoconfigure.oauth2.auth0.mgmt.api.Auth0Roles;
import com.ixortalk.autoconfigure.oauth2.auth0.mgmt.api.Auth0Users;
import com.ixortalk.organization.api.domain.Organization;
import com.ixortalk.organization.api.domain.User;
import com.ixortalk.organization.api.service.UserService;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.Optional;


import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.noContent;

@RestController
@Transactional
public class AssignRoleController {

    @Inject
    private UserRestResource userRestResource;

    @Inject
    private OrganizationRestResource organizationRestResource;

    @Inject
    private UserService userService;

    @Inject
    private Auth0Users auth0Users;

    @PostMapping(path = "/{organizationId}/{userId}/assign-admin-role")
    public ResponseEntity<?> assignAdminRole(@PathVariable("organizationId") Long organizationId, @PathVariable("userId") Long userId) {
        return findOrganizationAndAssociatedUser(organizationId, userId).map(organizationAndUser -> {
            userService.assignAdminRole(organizationAndUser.getFirst(), organizationAndUser.getSecond());
            return noContent().build();
        }).orElse(badRequest().build());
    }

    @PostMapping(path = "/{organizationId}/{userId}/remove-admin-role")
    public ResponseEntity<?> removeAdminRole(@PathVariable("organizationId") Long organizationId, @PathVariable("userId") Long userId) {
        return findOrganizationAndAssociatedUser(organizationId, userId).map(organizationAndUser -> {
            userService.removeAdminRole(organizationAndUser.getFirst(), organizationAndUser.getSecond());
            return noContent().build();
        }).orElse(badRequest().build());
    }


    private Optional<Pair<Organization, User>> findOrganizationAndAssociatedUser(Long organizationId, Long userId) {
        Organization organization = organizationRestResource.findById(organizationId).orElseThrow(ResourceNotFoundException::new);
        User user = userRestResource.findById(userId).orElseThrow(ResourceNotFoundException::new);

        if (organization == null || user == null || !organization.containsUser(user) || !auth0Users.userExists(user.getLogin())) {
            return Optional.empty();
        }

        return Optional.of(Pair.of(organization, user));
    }
}
