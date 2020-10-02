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

import com.ixortalk.organization.api.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.time.Instant;
import java.util.Optional;

@RepositoryRestResource
@PreAuthorize("hasRole('ROLE_ADMIN')")
public interface UserRestResource extends PagingAndSortingRepository<User, Long> {

    String FIND_BY_ORGANIZATION_ID_QUERY = "from org_user u where u.organization_id = :organizationId";
    String FIND_BY_ORGANIZATION_ID_AND_LOGIN_QUERY = "from org_user u where (u.organization_id = :organizationId) and (u.login like lower(concat('%', :login,'%')))";

    @Override
    @PreAuthorize("hasAnyRole('ROLE_ADMIN') " +
            "or #user.id == null " +
            "or @securityService.isCurrentUser(#user) " +
            "or @securityService.hasAdminAccess(#user)")
    <S extends User> S save(@P("user") S user);

    @Override
    @PreAuthorize("permitAll()")
    @PostAuthorize("hasAnyRole('ROLE_ADMIN')" +
            "or !returnObject.isPresent() " +
            "or @securityService.isCurrentUser(returnObject) " +
            "or !@organizationRestResource.findByUsers(returnObject.get()).isPresent() " +
            "or @securityService.hasAdminAccess(returnObject.get())")
    Optional<User> findById(Long id);

    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isAdminOfOrganization(@organizationRestResource.findOneById(#organizationId))")
    @Query(
            value = "select * "+ FIND_BY_ORGANIZATION_ID_QUERY,
            countQuery = "select count(*) "+ FIND_BY_ORGANIZATION_ID_QUERY,
            nativeQuery = true)
    Page<User> findByOrganizationId(Pageable pageable, @Param("organizationId") Long organizationId);

    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isAdminOfOrganization(@organizationRestResource.findOneById(#organizationId))")
    @Query(
            value = "select * "+ FIND_BY_ORGANIZATION_ID_AND_LOGIN_QUERY,
            countQuery = "select count(*) "+ FIND_BY_ORGANIZATION_ID_AND_LOGIN_QUERY,
            nativeQuery = true)
    Page<User> findByOrganizationIdAndLogin(Pageable pageable, @Param("organizationId") Long organizationId, @Param("login") String login);

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isCurrentUser(#user) " +
            "or @securityService.hasAdminAccess(#user)")
    void delete(@P("user") User user);

    @RestResource(exported = false)
    @PreAuthorize("permitAll()")
    Optional<User> findByIdAndAcceptKeyAcceptKey(Long id, String acceptKey);

    @RestResource(exported = false)
    @PreAuthorize("permitAll()")
    Optional<User> findByLoginAndAcceptKeyAcceptKeyAndAcceptKeyAcceptKeyTimestampAfter(String login, String acceptKey, Instant timestamp);
}
