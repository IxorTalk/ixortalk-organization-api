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

import com.ixortalk.organization.api.callback.api.OrganizationCallbackAPI;
import com.ixortalk.organization.api.domain.EnhancedUserProjection;
import com.ixortalk.organization.api.domain.Organization;
import com.ixortalk.organization.api.domain.Status;
import com.ixortalk.organization.api.domain.User;
import com.ixortalk.organization.api.events.OrganizationCascadedDeleteEvent;
import com.ixortalk.organization.api.events.OrganizationEventHandler;
import com.ixortalk.organization.api.events.RoleEventHandler;
import com.ixortalk.organization.api.mail.InviteUserService;
import com.ixortalk.organization.api.service.OrganizationService;
import com.ixortalk.organization.api.service.UserEmailProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.noContent;

@Transactional
@RestController
@RequestMapping("/organizations")
public class OrganizationRestController {

    @Inject
    private ApplicationEventPublisher applicationEventPublisher;

    @Inject
    private OrganizationRestResource organizationRestResource;

    @Inject
    private InviteUserService inviteUserService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private OrganizationCallbackAPI organizationCallbackAPI;

    @Inject
    private UserEmailProvider userEmailProvider;

    @Inject
    private OrganizationEventHandler organizationEventHandler;

    @Inject
    private RoleEventHandler roleEventHandler;

    @DeleteMapping(path = "/{organizationId}/cascade")
    public ResponseEntity<?> deleteCascade(@PathVariable("organizationId") Long organizationId) {
        Organization organization = organizationRestResource.findById(organizationId).orElseThrow(ResourceNotFoundException::new);
        organizationRestResource.delete(organization);
        organizationCallbackAPI.organizationRemoved(organizationId);
        organizationEventHandler.handleAfterDelete(organization);
        organization.getRoles().forEach(role -> roleEventHandler.handleAfterDelete(role));
        applicationEventPublisher.publishEvent(new OrganizationCascadedDeleteEvent(organization));
        return noContent().build();
    }

    @GetMapping(path = "/{organizationId}/adminUsers", produces = APPLICATION_JSON_VALUE)
    public CollectionModel<EntityModel<EnhancedUserProjection>> getAdminUsers(@PathVariable("organizationId") Long organizationId) {
        return organizationService.getAdminUsers(organizationId);
    }



    @PostMapping(path = "/{organizationId}/users/used")
    public void usersUsed(
            @PathVariable("organizationId") Long organizationId,
            @RequestBody List<String> logins) {

        Organization organization = organizationRestResource.findById(organizationId).orElseThrow(ResourceNotFoundException::new);
        organization
                .getUsers()
                .stream()
                .filter(User::isCreated)
                .filter(user -> logins.contains(user.getLogin()))
                .forEach(user -> inviteUserService.inviteUser(user, organization));
    }

    @GetMapping(path = "/search/findAcceptedOrganizationIds")
    public List<Long> findAcceptedOrganizationIds() {
        return userEmailProvider.getCurrentUsersEmail()
                .map(this::findAcceptedOrganizationIds)
                .orElse(newArrayList());
    }

    @GetMapping(path = "/search/findAcceptedOrganizationIds", params = {"userId"})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isCurrentUser(#userId)")
    public List<Long> findAcceptedOrganizationIds(@RequestParam(name = "userId") String userId) {
        return organizationRestResource.findByUsersLoginAndUsersStatus(userId.toLowerCase(), Status.ACCEPTED).stream().map(Organization::getId).collect(toList());
    }
}
