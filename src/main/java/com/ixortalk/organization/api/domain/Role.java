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

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.List;

import static java.lang.Math.min;

@Entity
@Table(name = "org_role")
public class Role {
    private static final String ROLE_PREFIX = "ROLE_";
    private static final int ROLE_MAX_LENGTH = 190;
    private static final int ROLE_NAME_DYNAMIC_PART_MAX_LENGTH = ROLE_MAX_LENGTH - ROLE_PREFIX.length();


    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String role;

    @ManyToMany(mappedBy = "roles")
    private List<User> users;

    @Column(name = "organization_id", updatable = false, insertable = false)
    private Long organizationId;

    private Role() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public Role assignRoleName(Organization organization) {
        this.role = generateRoleName(organization, this);
        return this;
    }

    private static String generateRoleName(Organization organization, Role role) {
        String dynamicPart = organization.getName().trim().replaceAll("[\\W]+", "_").toUpperCase();
        return new StringBuilder()
                        .append(ROLE_PREFIX)
                        .append(dynamicPart, 0, min(dynamicPart.length(), ROLE_NAME_DYNAMIC_PART_MAX_LENGTH))
                        .append("_")
                        .append(role.getId())
                        .toString();
    }
}
