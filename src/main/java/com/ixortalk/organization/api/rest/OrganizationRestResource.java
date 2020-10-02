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

import com.ixortalk.organization.api.domain.Organization;
import com.ixortalk.organization.api.domain.Role;
import com.ixortalk.organization.api.domain.Status;
import com.ixortalk.organization.api.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Collection;
import java.util.Optional;

@RepositoryRestResource
@PreAuthorize("hasRole('ROLE_ADMIN')")
public interface OrganizationRestResource extends PagingAndSortingRepository<Organization, Long> {

    @Override
    @PreAuthorize("permitAll()")
    @PostFilter("hasRole('ROLE_ADMIN') or @securityService.isAdminOfOrganization(filterObject)")
    Collection<Organization> findAll();

    @Override
    @PreAuthorize("permitAll()")
    @PostFilter("hasRole('ROLE_ADMIN') or @securityService.isAdminOfOrganization(filterObject)")
    Iterable<Organization> findAll(Sort sort);

    @Override
    @PreAuthorize("permitAll()")
    @Query("from Organization o where ?#{ hasRole('ROLE_ADMIN') } = true or exists " +
            "(from Organization o2 join o2.users u where (u.login = ?#{ @userEmailProvider.currentUsersEmail.orElse(null) } and u.isAdmin = true and o2.id = o.id)) ")
    Page<Organization> findAll(Pageable pageable);

    @Override
    @PreAuthorize("permitAll()")
    @PostAuthorize("hasRole('ROLE_ADMIN') or (returnObject.present && @securityService.isAdminOfOrganization(returnObject))")
    Optional<Organization> findById(Long id);

    @RestResource(exported = false)
    @PreAuthorize("permitAll()")
    Organization findOneById(Long id);

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isAdminOfOrganization(#organization)" +
            "or #organization.id == null")
    <S extends Organization> S save(@P("organization") S organization);

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isAdminOfOrganization(#organization)")
    void delete(Organization organization);

    @PreAuthorize("permitAll()")
    @RestResource(exported = false)
    Optional<Organization> findByName(String name);

    @PreAuthorize("permitAll()")
    @RestResource(exported = false)
    Optional<Organization> findByUsers(User user);

    @PreAuthorize("permitAll()")
    @RestResource(exported = false)
    Optional<Organization> findByRoles(Role role);

    @PreAuthorize("permitAll()")
    @RestResource(exported = false)
    Collection<Organization> findByUsersLoginAndUsersStatus(String login, Status status);

    @PreAuthorize("permitAll()")
    @RestResource(exported = false)
    @Query("from Organization o where exists(from Organization o2, User u2 where :user member of o2.users and o2.id = o.id and u2 in elements(o2.users) and u2.login = :login and u2.isAdmin = true)")
    Optional<Organization> hasAdminAccess(String login, User user);
}
