package com.ixortalk.organization.api.domain;

import com.google.common.base.Strings;
import org.junit.Test;

import static com.ixortalk.organization.api.domain.OrganizationTestBuilder.anOrganization;
import static com.ixortalk.organization.api.domain.RoleTestBuilder.aRole;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class RoleTest {
    @Test
    public void generateRoleName() {
        Organization organization = anOrganization().withName("My Organization").build();
        Role role = aRole().withName("test").build();
        setField(role, "id", 100L);
        role.assignRoleName(organization);

        assertThat(role.getRole()).isEqualTo("ROLE_MY_ORGANIZATION_100");
    }

   @Test
    public void generateRoleName_weirdCharacters() {
        Organization organization = anOrganization().withName("My %#€Organization").build();
        Role role = aRole().withName("test").build();
        setField(role, "id", 100L);
        role.assignRoleName(organization);

        assertThat(role.getRole()).isEqualTo("ROLE_MY_ORGANIZATION_100");
    }

    @Test
    public void generateRoleName_trimming() {
        Organization organization = anOrganization().withName("           My Organization        ").build();
        Role role = aRole().withName("test").build();
        setField(role, "id", 100L);
        role.assignRoleName(organization);

        assertThat(role.getRole()).isEqualTo("ROLE_MY_ORGANIZATION_100");
    }

    @Test
    public void generateRoleName_lineBreak() {
        Organization organization = anOrganization().withName("           My \nOrganization        ").build();
        Role role = aRole().withName("test").build();
        setField(role, "id", 100L);
        role.assignRoleName(organization);

        assertThat(role.getRole()).isEqualTo("ROLE_MY_ORGANIZATION_100");
    }

    @Test
    public void generateRoleName_chopped() {

        Organization organization = anOrganization().withName("My Organization" + Strings.repeat("n", 300)).build();
        Role role = aRole().withName("test").build();
        setField(role, "id", 100L);
        role.assignRoleName(organization);

        assertThat(role.getRole()).hasSize(194);
    }
}
