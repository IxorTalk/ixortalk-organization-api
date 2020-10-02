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
import com.ixortalk.autoconfigure.oauth2.auth0.mgmt.api.Auth0Users;
import com.ixortalk.organization.api.callback.api.OrganizationCallbackAPI;
import com.ixortalk.organization.api.domain.Role;
import com.ixortalk.organization.api.domain.User;
import com.ixortalk.organization.api.error.BadRequestException;
import com.ixortalk.organization.api.error.ConflictException;
import com.ixortalk.organization.api.rest.OrganizationRestResource;
import com.ixortalk.organization.api.rest.dto.UserInOrganizationDTO;
import org.springframework.data.rest.core.annotation.*;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@RepositoryEventHandler
public class UserEventHandler {

    @Inject
    private OrganizationRestResource organizationRestResource;

    @Inject
    private OrganizationCallbackAPI organizationCallbackAPI;

    @Inject
    private Auth0Users auth0Users;

    @Inject
    private Auth0Roles auth0Roles;

    @HandleBeforeCreate
    @HandleBeforeSave
    public void loginToLowercase(User user) {
        user.loginToLowercase();
    }

    @HandleBeforeDelete
    public void handleBeforeDelete(User user) {
        organizationRestResource.findByUsers(user)
                .ifPresent(organization -> {
                    if (auth0Users.userExists(user.getLogin())) {
                        auth0Roles.removeRolesFromUser(
                                user.getLogin(),
                                organization.getMatchingRoles(auth0Roles.getUsersRoles(user.getLogin())));
                    }
                    organizationCallbackAPI.userRemoved(new UserInOrganizationDTO(user.getLogin(), organization.getId()));
                });
    }

    @HandleBeforeLinkSave
    public void handleBeforeLinkSave(User user, List<Role> roles) {
        if (!user.isAccepted())
            return;

        if (roles.contains(null))
            throw new BadRequestException();

        if (roles.size() != roles.stream().map(Role::getRole).distinct().count())
            throw new ConflictException();

        if (auth0Users.userExists(user.getLogin())) {
            auth0Roles.assignRolesToUser(user.getLogin(), roles.stream().map(Role::getRole).collect(toSet()));
        }
    }

    @HandleAfterLinkDelete
    public void handleAfterLinkDelete(User user, List<Role> roles) {
        if (!user.isAccepted()) {
            return;
        }

        if (auth0Users.userExists(user.getLogin())) {
            organizationRestResource.findByUsers(user)
                    .ifPresent(organization -> {
                        Set<String> roleNamesToRemove =
                                organization
                                        .getMatchingRoles(auth0Roles.getUsersRoles(user.getLogin()))
                                        .stream()
                                        .filter(matchingRole -> roles.stream().noneMatch(role -> role.getRole().equals(matchingRole)))
                                        .collect(toSet());
                        if (!roleNamesToRemove.isEmpty()) {
                            auth0Roles.removeRolesFromUser(
                                    user.getLogin(),
                                    roleNamesToRemove);
                        }
                    });
        }
    }
}
