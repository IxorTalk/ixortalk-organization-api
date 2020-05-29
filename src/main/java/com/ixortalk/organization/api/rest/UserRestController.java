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
import com.ixortalk.organization.api.callback.api.OrganizationCallbackAPI;
import com.ixortalk.organization.api.domain.Organization;
import com.ixortalk.organization.api.domain.Role;
import com.ixortalk.organization.api.domain.User;
import com.ixortalk.organization.api.rest.dto.UserInOrganizationDTO;
import com.ixortalk.organization.api.service.SecurityService;
import com.ixortalk.organization.api.error.BadRequestException;
import com.ixortalk.organization.api.mail.InviteUserService;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.inject.Inject;
import java.util.Optional;

import static java.util.stream.Collectors.toSet;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.noContent;

@RepositoryRestController
@RequestMapping("/users")
@Transactional
public class UserRestController {

    @Inject
    private OrganizationRestResource organizationRestResource;

    @Inject
    private UserRestResource userRestResource;

    @Inject
    private InviteUserService inviteUserService;

    @Inject
    private OrganizationCallbackAPI organizationCallbackAPI;

    @Inject
    private SecurityService securityService;

    @Inject
    private Auth0Roles auth0Roles;

    @Inject
    private Auth0Users auth0Users;

    @PostMapping(path = "/{userId}/{acceptKey}/accept-invite")
    public ResponseEntity<?> acceptInviteByKey(@PathVariable("userId") Long userId, @PathVariable("acceptKey") String acceptKey) {
        if (userRestResource.findById(userId).map(User::isAccepted).orElse(false)) {
            return noContent().build();
        }

        return acceptInvite(userRestResource.findByIdAndAcceptKeyAcceptKey(userId, acceptKey));
    }

    @PostMapping(path = "/{userId}/accept-invite")
    public ResponseEntity<?> acceptInvite(@PathVariable("userId") Long userId) {
        return acceptInvite(userRestResource.findById(userId).filter(user -> securityService.isCurrentUser(user)));
    }

    private ResponseEntity<Object> acceptInvite(Optional<User> optionalUser) {
        return optionalUser
                .map(user -> {
                    userRestResource.save(user.accepted());
                    organizationCallbackAPI.userAccepted(new UserInOrganizationDTO(user.getLogin(), organizationRestResource.findByUsers(user).map(Organization::getId).orElseThrow(BadRequestException::new)));
                    auth0Users.unblockUser(user.getLogin());
                    auth0Roles.assignRolesToUser(user.getLogin(), user.getRoles().stream().map(Role::getRole).collect(toSet()));
                    return noContent().build();
                })
                .orElse(badRequest().build());
    }


    @PostMapping(path = "/{userId}/decline-invite")
    public ResponseEntity<?> declineInvite(@PathVariable("userId") Long userId) {
        User user =
                userRestResource.findById(userId)
                        .filter(foundUser -> securityService.isCurrentUser(foundUser))
                        .orElseThrow(BadRequestException::new);

        return organizationRestResource.findByUsers(user)
                .map(organization -> organization.removeUser(user))
                .filter(removed -> removed)
                .map(organization -> {
                    userRestResource.delete(user);
                    return noContent().build();
                })
                .orElse(badRequest().build());
    }

    @PostMapping(path = "/{userId}/resend-invite")
    public ResponseEntity<?> resendInvite(@PathVariable("userId") Long userId) {
        return userRestResource.findById(userId)
                .filter(User::isInvited)
                .flatMap(
                        user ->
                                organizationRestResource.findByUsers(user)
                                        .map(organization -> {
                                            inviteUserService.inviteUser(user, organization);
                                            return noContent().build();
                                        })
                )
                .orElse(badRequest().build());
    }
}
