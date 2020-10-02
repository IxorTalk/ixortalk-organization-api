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
package com.ixortalk.organization.api.events;

import com.ixortalk.autoconfigure.oauth2.auth0.mgmt.api.Auth0Roles;
import com.ixortalk.organization.api.callback.api.OrganizationCallbackAPI;
import com.ixortalk.organization.api.domain.Organization;
import com.ixortalk.organization.api.domain.Role;
import com.ixortalk.organization.api.domain.User;
import com.ixortalk.organization.api.error.BadRequestException;
import com.ixortalk.organization.api.error.ConflictException;
import com.ixortalk.organization.api.rest.OrganizationRestResource;
import com.ixortalk.organization.api.rest.RoleRestResource;
import com.ixortalk.organization.api.service.AssetMgmtFacade;
import com.ixortalk.organization.api.service.UserEmailProvider;
import org.springframework.data.rest.core.annotation.*;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@RepositoryEventHandler
public class OrganizationEventHandler {

    @Inject
    private OrganizationRestResource organizationRestResource;

    @Inject
    private RoleRestResource roleRestResource;

    @Inject
    private UserEmailProvider userEmailProvider;

    @Inject
    private Auth0Roles auth0Roles;

    @Inject
    private OrganizationCallbackAPI organizationCallbackAPI;

    @Inject
    private AssetMgmtFacade assetMgmtFacade;

    @HandleBeforeCreate
    public void handleBeforeCreate(Organization organization) {
        organization.generateRoleName();

        organizationRestResource.findByName(organization.getName()).ifPresent(existing -> {
            throw new ConflictException();
        });
        organizationRestResource.findByRole(organization.getRole()).ifPresent(existing -> {
            throw new ConflictException();
        });

        validateRoleDoesNotExistInAuth0(organization.getRole());

        userEmailProvider.getCurrentUsersEmail().ifPresent(email ->
        {
            organization.getUsers().add(User.initialOrganizationAdminUser(email));
        });
    }

    @HandleAfterCreate
    public void handleAfterCreate(Organization organization) {
        auth0Roles.addRole(organization.getRole());
        userEmailProvider.getCurrentUsersEmail().ifPresent(email -> auth0Roles.assignRolesToUser(email, newHashSet(organization.getRole())));
    }

    @HandleBeforeDelete
    public void handleBeforeDelete(Organization organization) {
        if (!organization.getUsers().isEmpty()) throw new BadRequestException("Users not empty");
        if (!organization.getRoles().isEmpty()) throw new BadRequestException("Roles not empty");
        organizationCallbackAPI.organizationPreDeleteCheck(organization.getId());
        if (assetMgmtFacade.getDevicesFromAssetMgmt(organization).findAny().isPresent())
            throw new BadRequestException("Devices still exist");
    }

    @HandleAfterDelete
    public void handleAfterDelete(Organization organization) {
        auth0Roles.deleteRole(organization.getRole());
    }

    @HandleBeforeSave
    public void handleBeforeSave(Organization organization) {
        organizationRestResource.findByName(organization.getName()).filter(found -> !found.getId().equals(organization.getId())).ifPresent(existing -> {
            throw new ConflictException();
        });
    }


    @HandleBeforeLinkSave
    public void handleBeforeLinkSave(Organization organization, Collection<?> linked) {
        if (!linked.isEmpty() && linked.iterator().next().getClass().isAssignableFrom(Role.class)) {
            handleRoleAdded(organization, (Collection<Role>) linked);
        }
        if (!linked.isEmpty() && linked.iterator().next().getClass().isAssignableFrom(User.class)) {
            removeDuplicateUsers(organization, (Collection<User>) linked);
        }
    }

    private void handleRoleAdded(Organization organization, Collection<Role> roles) {
        if (roles.size() != roles.stream().map(Role::getName).distinct().count()) {
            throw new ConflictException();
        }

        roles
                .stream()
                .filter(role -> role.getRole() == null)
                .map(role -> role.assignRoleName(organization.getRole()))
                .map(this::validateRoleNameNotInUse)
                .map(Role::getRole)
                .map(this::validateRoleDoesNotExistInAuth0)
                .forEach(role -> auth0Roles.addRole(role));
    }

    private void removeDuplicateUsers(Organization organization, Collection<User> linked) {
        Set<String> existingNonCreatedLogins = organization.getUsers().stream().filter(user -> !user.isCreated()).map(User::getLogin).collect(toSet());
        Map<String, User> createdUsersByLogin = organization.getUsers().stream().filter(User::isCreated).collect(toMap(User::getLogin, identity(), (o1, o2) -> o1));

        linked.removeIf(user -> user.isCreated() && (existingNonCreatedLogins.contains(user.getLogin().toLowerCase()) || !createdUsersByLogin.get(user.getLogin()).getId().equals(user.getId())));
    }

    private Role validateRoleNameNotInUse(Role role) {
        roleRestResource.findByRole(role.getRole()).filter(found -> !found.getId().equals(role.getId())).ifPresent(existing -> {
            throw new ConflictException();
        });
        return role;
    }

    private String validateRoleDoesNotExistInAuth0(String roleName) {
        if (auth0Roles.getAllRoleNames().contains(roleName)) {
            throw new ConflictException();
        }
        return roleName;
    }
}
