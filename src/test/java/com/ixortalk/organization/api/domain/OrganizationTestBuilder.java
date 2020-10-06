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

import com.ixortalk.test.builder.ReflectionInstanceTestBuilder;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.ixortalk.organization.api.domain.AddressTestBuilder.anAddress;
import static com.ixortalk.test.util.Randomizer.nextString;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class OrganizationTestBuilder extends ReflectionInstanceTestBuilder<Organization> {

    private String name = nextString("testOrg");
    private Address address = anAddress().build();
    private String phoneNumber;
    private String emailAddress;
    private String image;
    private String logo;

    private List<User> users = newArrayList();

    private List<Role> roles = newArrayList();

    private OrganizationTestBuilder() {}

    public static OrganizationTestBuilder anOrganization() {
        return new OrganizationTestBuilder();
    }

    @Override
    public void setFields(Organization organization) {
        setField(organization, "name", name);
        setField(organization, "address", address);
        setField(organization, "phoneNumber", phoneNumber);
        setField(organization, "emailAddress", emailAddress);
        setField(organization, "image", image);
        setField(organization, "logo", logo);
        setField(organization, "users", users);
        setField(organization, "roles", roles);
    }

    public OrganizationTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public OrganizationTestBuilder withAddress(Address address) {
        this.address = address;
        return this;
    }

    public OrganizationTestBuilder withPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public OrganizationTestBuilder withEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
        return this;
    }

    public OrganizationTestBuilder withImage(String image) {
        this.image = image;
        return this;
    }

    public OrganizationTestBuilder withLogo(String logo) {
        this.logo = logo;
        return this;
    }

    public OrganizationTestBuilder withUsers(User... users) {
        this.users.addAll(newArrayList(users));
        return this;
    }

    public OrganizationTestBuilder withRoles(Role... roles) {
        this.roles.addAll(newArrayList(roles));
        return this;
    }
}
