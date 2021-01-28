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
import com.ixortalk.organization.api.domain.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.matching.RequestPattern.everything;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.organization.api.TestConstants.ORGANIZATION_PRE_DELETE_CHECK_CALLBACK_PATH;
import static com.ixortalk.organization.api.config.TestConstants.ADMIN_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN;
import static com.ixortalk.organization.api.domain.UserTestBuilder.aUser;
import static io.restassured.RestAssured.given;
import static java.lang.String.valueOf;
import static java.util.Optional.of;
import static org.apache.http.HttpStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

@TestPropertySource(properties = {"ixortalk.server.assetmgmt.url=false"})
public class OrganizationRestResource_Delete_WithoutAssetMgmt_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final ParameterDescriptor ORGANIZATION_ID_PATH_PARAMETER = parameterWithName("id").description("The id of the organization to delete.");

    private static final String ORGANIZATION_ADMIN_TOKEN = "organizationAdminToken";

    private Organization organizationToDelete;

    @Before
    public void setupEmptyOrganization() throws JsonProcessingException {
        organizationToDelete = organizationRestResource.save(OrganizationTestBuilder.anOrganization().build());

        when(jwtDecoder.decode(ORGANIZATION_ADMIN_TOKEN))
                .thenReturn(
                        buildJwtTokenWithEmailCustomClaim(
                                ORGANIZATION_ADMIN_TOKEN,
                                of("admin@organization-to-delete.com"),
                                "orgManager"));

        organizationCallbackApiWireMockRule.stubFor(get(urlPathEqualTo("/org-callback-api" + ORGANIZATION_PRE_DELETE_CHECK_CALLBACK_PATH.configValue()))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .withQueryParam("organizationId", equalTo(valueOf(organizationToDelete.getId())))
                .willReturn(ok()));
    }

    @Test
    public void asAdmin() {

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .delete("/organizations/{id}", organizationToDelete.getId())
                .then()
                .statusCode(SC_NO_CONTENT);

        assertThat(organizationRestResource.findById(organizationToDelete.getId())).isNotPresent();
        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }

    @Test
    public void asOrganizationAdmin() {
        adminInOrganizationX = aUser().withLogin("admin@organization-to-delete.com").withStatus(Status.ACCEPTED).withIsAdmin(true).build();
        organizationToDelete.getUsers().add(adminInOrganizationX);
        organizationRestResource.save(organizationToDelete);

        given()
                .auth()
                .preemptive()
                .oauth2(ORGANIZATION_ADMIN_TOKEN)
                .filter(
                        document("organizations/delete/as-organization-admin",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(
                                        ORGANIZATION_ID_PATH_PARAMETER
                                ))
                )
                .when()
                .delete("/organizations/{id}", organizationToDelete.getId())
                .then()
                .statusCode(SC_NO_CONTENT);

        assertThat(organizationRestResource.findById(organizationToDelete.getId())).isNotPresent();
        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }

    @Test
    public void asOrganizationAdminForOtherOrganization() {

        given()
                .auth()
                .preemptive()
                .oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .filter(
                        document("organizations/delete/no-access",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(
                                        ORGANIZATION_ID_PATH_PARAMETER
                                ))
                )
                .when()
                .delete("/organizations/{id}", organizationToDelete.getId())
                .then()
                .statusCode(SC_FORBIDDEN);

        assertThat(organizationRestResource.findById(organizationToDelete.getId())).isPresent();
        verifyZeroInteractions(auth0Roles);
        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }

    @Test
    public void userExists() {

        organizationToDelete.getUsers().add(UserTestBuilder.aUser().build());
        organizationRestResource.save(organizationToDelete);

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .filter(
                        document("organizations/delete/organization-not-empty",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(
                                        ORGANIZATION_ID_PATH_PARAMETER
                                ))
                )
                .when()
                .delete("/organizations/{id}", organizationToDelete.getId())
                .then()
                .statusCode(SC_BAD_REQUEST);

        assertThat(organizationRestResource.findById(organizationToDelete.getId())).isPresent();
        verifyZeroInteractions(auth0Roles);
        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }

    @Test
    public void userNextToAdminExists() {
        adminInOrganizationX = aUser().withLogin("admin@organization-to-delete.com").withStatus(Status.ACCEPTED).withIsAdmin(true).build();
        organizationToDelete.getUsers().add(adminInOrganizationX);
        organizationToDelete.getUsers().add(UserTestBuilder.aUser().build());
        organizationRestResource.save(organizationToDelete);

        given()
                .auth()
                .preemptive()
                .oauth2(ORGANIZATION_ADMIN_TOKEN)
                .filter(
                        document("organizations/delete/as-organization-admin-and-other-user",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(
                                        ORGANIZATION_ID_PATH_PARAMETER
                                ))
                )
                .when()
                .delete("/organizations/{id}", organizationToDelete.getId())
                .then()
                .statusCode(SC_BAD_REQUEST);

        assertThat(organizationRestResource.findById(organizationToDelete.getId())).isPresent();
        verifyZeroInteractions(auth0Roles);
        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }

    @Test
    public void roleExists() {

        organizationToDelete.getRoles().add(RoleTestBuilder.aRole().build());
        organizationRestResource.save(organizationToDelete);

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .delete("/organizations/{id}", organizationToDelete.getId())
                .then()
                .statusCode(SC_BAD_REQUEST);

        assertThat(organizationRestResource.findById(organizationToDelete.getId())).isPresent();
        verifyZeroInteractions(auth0Roles);
        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }

    @Test
    public void preDeleteCheckFails() {

        organizationCallbackApiWireMockRule.stubFor(get(urlPathEqualTo("/org-callback-api" + ORGANIZATION_PRE_DELETE_CHECK_CALLBACK_PATH.configValue()))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .withQueryParam("organizationId", equalTo(valueOf(organizationToDelete.getId())))
                .willReturn(badRequest()));

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .delete("/organizations/{id}", organizationToDelete.getId())
                .then()
                .statusCode(SC_BAD_REQUEST);

        assertThat(organizationRestResource.findById(organizationToDelete.getId())).isPresent();
        verifyZeroInteractions(auth0Roles);
        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }

    @Test
    public void preDeleteCheckNotFound() {

        organizationCallbackApiWireMockRule.stubFor(get(urlPathEqualTo("/org-callback-api" + ORGANIZATION_PRE_DELETE_CHECK_CALLBACK_PATH.configValue()))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .withQueryParam("organizationId", equalTo(valueOf(organizationToDelete.getId())))
                .willReturn(notFound()));

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .delete("/organizations/{id}", organizationToDelete.getId())
                .then()
                .statusCode(SC_NOT_FOUND);

        assertThat(organizationRestResource.findById(organizationToDelete.getId())).isPresent();
        verifyZeroInteractions(auth0Roles);
        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }

    @Test
    public void deviceExists() throws JsonProcessingException {

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .delete("/organizations/{id}", organizationToDelete.getId())
                .then()
                .statusCode(SC_NO_CONTENT);

        assertThat(organizationRestResource.findById(organizationToDelete.getId())).isNotPresent();
        assertThat(assetMgmtWireMockRule.countRequestsMatching(everything()).getCount()).isZero();
    }
}
