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
import com.ixortalk.organization.api.asset.AssetId;
import com.ixortalk.organization.api.asset.AssetTestBuilder;
import com.ixortalk.organization.api.config.TestConstants;
import com.ixortalk.organization.api.domain.Role;
import com.ixortalk.organization.api.domain.User;
import org.junit.Before;
import org.junit.Test;
import org.springframework.restdocs.request.ParameterDescriptor;

import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.allRequests;
import static com.google.common.collect.Lists.newArrayList;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.organization.api.TestConstants.ORGANIZATION_REMOVED_CALLBACK_PATH;
import static com.ixortalk.organization.api.asset.AssetId.assetId;
import static com.ixortalk.organization.api.config.TestConstants.ADMIN_JWT_TOKEN;
import static com.ixortalk.test.util.Randomizer.nextString;
import static io.restassured.RestAssured.given;
import static java.lang.Long.MAX_VALUE;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toSet;
import static org.apache.http.HttpStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;


public class OrganizationRestController_DeleteCascade_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final ParameterDescriptor ORGANIZATION_ID_PATH_PARAMETER = parameterWithName("id").description("The id of the organization to delete.");

    public static final AssetId FIRST_DEVICE_ASSET_ID = assetId(nextString("firstDeviceAssetId"));
    public static final AssetId SECOND_DEVICE_ASSET_ID = assetId(nextString("secondDeviceAssetId"));

    private Set<Long> existingOrganizationXUserIds;
    private Set<Long> existingOrganizationXRoleIds;
    private Set<String> existingOrganizationXRoleNames;

    @Before
    public void before() throws JsonProcessingException {
        existingOrganizationXUserIds = organizationX.getUsers().stream().map(User::getId).collect(toSet());
        existingOrganizationXRoleIds = organizationX.getRoles().stream().map(Role::getId).collect(toSet());
        existingOrganizationXRoleNames = organizationX.getRoles().stream().map(Role::getRole).collect(toSet());

        assetMgmtWireMockRule.stubFor(
                post(urlEqualTo("/assetmgmt/assets/search/property"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(organizationX.getOrganizationId())))
                        .willReturn(okJson(objectMapper.writeValueAsString(newArrayList(
                                AssetTestBuilder.anAsset().withAssetId(FIRST_DEVICE_ASSET_ID).build(),
                                AssetTestBuilder.anAsset().withAssetId(SECOND_DEVICE_ASSET_ID).build()
                        )))));

        assetMgmtWireMockRule.stubFor(
                put(urlEqualTo("/assetmgmt/assets/" + FIRST_DEVICE_ASSET_ID.stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(assetMgmtFieldsToBeCleared())))
                        .willReturn(ok()));
        assetMgmtWireMockRule.stubFor(
                put(urlEqualTo("/assetmgmt/assets/" + SECOND_DEVICE_ASSET_ID.stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(assetMgmtFieldsToBeCleared())))
                        .willReturn(ok()));

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
    }

    @Test
    public void devicesRemovedFromOrganization() throws JsonProcessingException {

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .delete("/organizations/{id}/cascade", organizationX.getId())
                .then()
                .statusCode(SC_NO_CONTENT);

        assetMgmtWireMockRule.verify(
                putRequestedFor(urlEqualTo("/assetmgmt/assets/" + FIRST_DEVICE_ASSET_ID.stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(assetMgmtFieldsToBeCleared()))));
        assetMgmtWireMockRule.verify(
                putRequestedFor(urlEqualTo("/assetmgmt/assets/" + SECOND_DEVICE_ASSET_ID.stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(assetMgmtFieldsToBeCleared()))));
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
        assetMgmtWireMockRule.verify(0, allRequests());
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
        assetMgmtWireMockRule.verify(0, allRequests());
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

        assetMgmtWireMockRule.verify(0, allRequests());
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
        assetMgmtWireMockRule.verify(0, allRequests());
    }
}
