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
package com.ixortalk.organization.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toSet;
import static javax.persistence.CascadeType.ALL;
import static org.springframework.util.Assert.isTrue;

@Entity
public class Organization {

    public static final String ADMIN_ROLE_SUFFIX = "_ADMIN";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final int ROLE_MAX_LENGTH = 200;
    private static final int ROLE_NAME_DYNAMIC_PART_MAX_LENGTH = ROLE_MAX_LENGTH - ROLE_PREFIX.length() - ADMIN_ROLE_SUFFIX.length();

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String name;

    @Column(updatable = false, unique = true)
    @JsonIgnore
    private String role;

    @OneToMany(cascade = ALL)
    @JoinColumn(name = "organization_id")
    private List<User> users = newArrayList();

    @OneToMany(cascade = ALL)
    @JoinColumn(name = "organization_id")
    private List<Role> roles = newArrayList();

    @Embedded
    @NotNull
    private Address address;

    private String phoneNumber;

    private String emailAddress;

    private String image;

    private String logo;

    private Organization() {
    }

    public Long getId() {
        return id;
    }

    @JsonIgnore
    public OrganizationId getOrganizationId() {
        return OrganizationId.organizationId(getId());
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public Address getAddress() {
        return address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getImage() {
        return image;
    }

    public Organization setImage(String image) {
        this.image = image;
        return this;
    }

    public String getLogo() {
        return logo;
    }

    public boolean containsUser(User user) {
        return this.users.contains(user);
    }

    public boolean containsRole(Role role) {
        return this.roles.contains(role);
    }

    public Set<String> getMatchingRoles(Set<String> roles) {
        return this.roles.stream().filter(role -> roles.contains(role.getRole())).map(Role::getRole).collect(toSet());
    }

    public void generateRoleName() {
        isTrue(this.role == null, "role should not be set");

        String roleNameDynamicPart = this.name.trim().replaceAll("[\\W]+", "_").toUpperCase();
        this.role =
                new StringBuilder()
                        .append(ROLE_PREFIX)
                        .append(roleNameDynamicPart, 0, min(roleNameDynamicPart.length(), ROLE_NAME_DYNAMIC_PART_MAX_LENGTH))
                        .append(ADMIN_ROLE_SUFFIX)
                        .toString();
    }

    public boolean removeUser(User user) {
        return this.users.remove(user);
    }

    @JsonIgnore
    public List<User> getUsers() {
        return this.users;
    }

    @JsonIgnore
    public List<Role> getRoles() {
        return this.roles;
    }

    public Organization setLogo(String logo) {
        this.logo = logo;
        return this;
    }
}
