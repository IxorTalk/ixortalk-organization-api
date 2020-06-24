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

import com.google.common.base.Strings;
import org.junit.Test;

import static com.ixortalk.organization.api.domain.OrganizationTestBuilder.anOrganization;
import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationTest {

    @Test
    public void generateRoleName() {
        Organization organization = anOrganization().withName("My Organization").withRole(null).build();

        organization.generateRoleName();

        assertThat(organization.getRole()).isEqualTo("ROLE_MY_ORGANIZATION_ADMIN");
    }

    @Test
    public void generateRoleName_weirdCharacters() {
        Organization organization = anOrganization().withName("My %#€Organization").withRole(null).build();

        organization.generateRoleName();

        assertThat(organization.getRole()).isEqualTo("ROLE_MY_ORGANIZATION_ADMIN");
    }

    @Test
    public void generateRoleName_trimming() {
        Organization organization = anOrganization().withName("           My Organization        ").withRole(null).build();

        organization.generateRoleName();

        assertThat(organization.getRole()).isEqualTo("ROLE_MY_ORGANIZATION_ADMIN");
    }

    @Test
    public void generateRoleName_lineBreak() {
        Organization organization = anOrganization().withName("           My \nOrganization        ").withRole(null).build();

        organization.generateRoleName();

        assertThat(organization.getRole()).isEqualTo("ROLE_MY_ORGANIZATION_ADMIN");
    }

    @Test
    public void generateRoleName_chopped() {
        Organization organization = anOrganization().withName("My Organization" + Strings.repeat("n", 300)).withRole(null).build();

        organization.generateRoleName();

        assertThat(organization.getRole()).hasSize(200);
    }

}