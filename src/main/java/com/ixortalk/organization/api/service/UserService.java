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

import com.ixortalk.autoconfigure.oauth2.auth0.mgmt.api.Auth0Roles;
import com.ixortalk.organization.api.domain.Organization;
import com.ixortalk.organization.api.domain.User;
import com.ixortalk.organization.api.rest.UserRestResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;

import static com.google.common.collect.Sets.newHashSet;

@Named
public class UserService {

    @Inject
    private UserRestResource userRestResource;

    @Inject
    private Auth0Roles auth0Roles;

    @Transactional
    public User assignAdminRole(Organization organization, User user) {
        user.setAdmin(true);
        auth0Roles.assignRolesToUser(user.getLogin(), newHashSet(organization.getRole()));
        return userRestResource.save(user);
    }

    @Transactional
    public User removeAdminRole(Organization organization, User user) {
        user.setAdmin(false);
        auth0Roles.removeRolesFromUser(user.getLogin(), newHashSet(organization.getRole()));
        return userRestResource.save(user);
    }
}
