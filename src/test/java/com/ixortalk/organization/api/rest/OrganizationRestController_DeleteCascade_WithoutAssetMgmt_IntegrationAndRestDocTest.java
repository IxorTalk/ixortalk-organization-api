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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ixortalk.organization.api.AbstractSpringIntegrationTest;
import com.ixortalk.organization.api.config.TestConstants;
import com.ixortalk.organization.api.domain.Role;
import com.ixortalk.organization.api.domain.User;
import org.junit.Before;
import org.junit.Test;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.matching.RequestPattern.everything;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.allRequests;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.organization.api.TestConstants.ORGANIZATION_REMOVED_CALLBACK_PATH;
import static com.ixortalk.organization.api.config.TestConstants.ADMIN_JWT_TOKEN;
import static io.restassured.RestAssured.given;
import static java.lang.Long.MAX_VALUE;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toSet;
import static org.apache.http.HttpStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;


@TestPropertySource(properties = {"ixortalk.server.assetmgmt.url=false"})
public class OrganizationRestController_DeleteCascade_WithoutAssetMgmt_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final ParameterDescriptor ORGANIZATION_ID_PATH_PARAMETER = parameterWithName("id").description("The id of the organization to delete.");

    private Set<Long> existingOrganizationXUserIds;
    private Set<Long> existingOrganizationXRoleIds;
    private Set<String> existingOrganizationXRoleNames;

    @Before
    public void before() throws JsonProcessingException {
        existingOrganizationXUserIds = organizationX.getUsers().stream().map(User::getId).collect(toSet());
        existingOrganizationXRoleIds = organizationX.getRoles().stream().map(Role::getId).collect(toSet());
        existingOrganizationXRoleNames = organizationX.getRoles().stream().map(Role::getRole).collect(toSet());

        organizationCallbackApiWireMockRule.stubFor(post(urlPathEqualTo("/org-callback-api" + ORGANIZATION_REMOVED_CALLBACK_PATH.configValue()))
                .withQueryParam("organizationId", equalTo(valueOf(organizationX.getId())))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(ok()));
    }

    @Test
    public void usersDeleted() {

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .delete("/organizations/{id}/cascade", organizationX.getId())
                .then()
                .statusCode(SC_NO_CONTENT);

        assertThat(userRestResource.findAllById(existingOrganizationXUserIds)).isEmpty();
        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }

    @Test
    public void rolesDeleted() {

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .delete("/organizations/{id}/cascade", organizationX.getId())
                .then()
                .statusCode(SC_NO_CONTENT);

        assertThat(roleRestResource.findAllById(existingOrganizationXRoleIds)).isEmpty();
        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }

    @Test
    public void organizationDeleted() {

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .delete("/organizations/{id}/cascade", organizationX.getId())
                .then()
                .statusCode(SC_NO_CONTENT);

        assertThat(organizationRestResource.findById(organizationX.getId())).isNotPresent();
        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }

    @Test
    public void rolesDeletedFromAuth0() {

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .delete("/organizations/{id}/cascade", organizationX.getId())
                .then()
                .statusCode(SC_NO_CONTENT);

        existingOrganizationXRoleNames.forEach(role -> verify(auth0Roles).deleteRole(role));
        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }

    @Test
    public void devicesNotRemovedFromOrganization() throws JsonProcessingException {

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .delete("/organizations/{id}/cascade", organizationX.getId())
                .then()
                .statusCode(SC_NO_CONTENT);

        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }

    @Test
    public void organizationCallback() {

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .delete("/organizations/{id}/cascade", organizationX.getId())
                .then()
                .statusCode(SC_NO_CONTENT);

        organizationCallbackApiWireMockRule.verify(1,
                postRequestedFor(urlPathEqualTo("/org-callback-api" + ORGANIZATION_REMOVED_CALLBACK_PATH.configValue()))
                        .withQueryParam("organizationId", equalTo(valueOf(organizationX.getId())))
                        .andMatching(retrievedAdminTokenAuthorizationHeader()));
        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }

    @Test
    public void asOrganizationAdmin() {

        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .filter(
                        document("organizations/delete-cascade/as-organization-admin",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(
                                        ORGANIZATION_ID_PATH_PARAMETER
                                ))
                )
                .when()
                .delete("/organizations/{id}/cascade", organizationX.getId())
                .then()
                .statusCode(SC_NO_CONTENT);

        assertThat(organizationRestResource.findById(organizationX.getId())).isNotPresent();
        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }

    @Test
    public void organizationNotFound() {

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .delete("/organizations/{id}/cascade", MAX_VALUE)
                .then()
                .statusCode(SC_NOT_FOUND);

        verifyZeroInteractions(auth0Roles);
        organizationCallbackApiWireMockRule.verify(0, allRequests());
        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }

    @Test
    public void organizationAdminFromDifferentOrganization() {

        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.USER_IN_ORGANIZATION_Y_ADMIN_JWT_TOKEN)
                .filter(
                        document("organizations/delete-cascade/no-access",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(
                                        ORGANIZATION_ID_PATH_PARAMETER
                                ))
                )
                .when()
                .delete("/organizations/{id}/cascade", organizationX.getId())
                .then()
                .statusCode(SC_FORBIDDEN);

        assertThat(organizationRestResource.findById(organizationX.getId())).isPresent();
        verifyZeroInteractions(auth0Roles);
        organizationCallbackApiWireMockRule.verify(0, allRequests());
        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }

    @Test
    public void deviceNotRemovedWhenOrganizationCallbackFails() {
        organizationCallbackApiWireMockRule.stubFor(post(urlPathEqualTo("/org-callback-api" + ORGANIZATION_REMOVED_CALLBACK_PATH.configValue()))
                .withQueryParam("organizationId", equalTo(valueOf(organizationX.getId())))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(badRequest()));

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .delete("/organizations/{id}/cascade", organizationX.getId())
                .then()
                .statusCode(SC_BAD_REQUEST);

        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }

    @Test
    public void organizationCallbackNotFound() {
        organizationCallbackApiWireMockRule.stubFor(post(urlPathEqualTo("/org-callback-api" + ORGANIZATION_REMOVED_CALLBACK_PATH.configValue()))
                .withQueryParam("organizationId", equalTo(valueOf(organizationX.getId())))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(notFound()));

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .delete("/organizations/{id}/cascade", organizationX.getId())
                .then()
                .statusCode(SC_NOT_FOUND);

        assertThat(organizationRestResource.findById(organizationX.getId())).isPresent();
        verifyZeroInteractions(auth0Roles);
        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }
}
