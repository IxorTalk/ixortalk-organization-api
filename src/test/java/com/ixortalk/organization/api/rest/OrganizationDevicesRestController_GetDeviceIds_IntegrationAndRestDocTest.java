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
import com.ixortalk.organization.api.asset.AssetTestBuilder;
import com.ixortalk.organization.api.asset.DeviceId;
import com.ixortalk.organization.api.config.TestConstants;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.google.common.collect.Lists.newArrayList;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.organization.api.asset.DeviceId.deviceId;
import static com.ixortalk.organization.api.config.TestConstants.*;
import static com.ixortalk.organization.api.rest.docs.RestDocDescriptors.PathParameters.ORGANIZATION_ID_PATH_PARAMETER;
import static com.ixortalk.test.util.Randomizer.nextString;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class OrganizationDevicesRestController_GetDeviceIds_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final DeviceId OTHER_DEVICE_ID = deviceId(nextString("otherDeviceId"));

    @Before
    public void before() throws IOException {

        assetMgmtWireMockRule.stubFor(
                post(urlEqualTo("/assetmgmt/assets/search/property"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(organizationX.getOrganizationId())))
                        .willReturn(okJson(objectMapper.writeValueAsString(
                                newArrayList(
                                        AssetTestBuilder.anAsset().withDeviceId(TEST_DEVICE).build(),
                                        AssetTestBuilder.anAsset().withDeviceId(OTHER_DEVICE_ID).build())))));
    }

    @Test
    public void asAdmin() {

        DeviceId[] deviceIds =
                given()
                        .auth()
                        .preemptive()
                        .oauth2(ADMIN_JWT_TOKEN)
                        .contentType(JSON)
                        .get("/organizations/{id}/deviceIds", organizationX.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().as(DeviceId[].class);

        assertThat(deviceIds).containsOnly(TEST_DEVICE, OTHER_DEVICE_ID);

        assetMgmtWireMockRule.verify(postRequestedFor(urlEqualTo("/assetmgmt/assets/search/property")).andMatching(retrievedAdminTokenAuthorizationHeader()));
    }

    @Test
    public void asOrganizationAdmin() {

        DeviceId[] deviceIds =
                given()
                        .auth()
                        .preemptive()
                        .oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .contentType(JSON)
                        .filter(
                                document("organizations/get-device-ids/ok",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()),
                                        pathParameters(
                                                ORGANIZATION_ID_PATH_PARAMETER
                                        )
                                ))
                        .get("/organizations/{id}/deviceIds", organizationX.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().as(DeviceId[].class);

        assertThat(deviceIds).containsOnly(TEST_DEVICE, OTHER_DEVICE_ID);

        assetMgmtWireMockRule.verify(postRequestedFor(urlEqualTo("/assetmgmt/assets/search/property")).andMatching(retrievedAdminTokenAuthorizationHeader()));
    }

    @Test
    public void asOrganizationAdminFromDifferentOrganization() {

        given()
                .auth()
                .preemptive()
                .oauth2(USER_IN_ORGANIZATION_Y_ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/get-device-ids/different-admin",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(
                                        ORGANIZATION_ID_PATH_PARAMETER
                                )
                        ))
                .get("/organizations/{id}/deviceIds", organizationX.getId())
                .then()
                .statusCode(SC_FORBIDDEN);

        assetMgmtWireMockRule.verify(0, postRequestedFor(urlEqualTo("/assetmgmt/assets/search/property")).andMatching(retrievedAdminTokenAuthorizationHeader()));
    }

    @Test
    public void asUser() {

        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.USER_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/get-device-ids/as-user",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(
                                        ORGANIZATION_ID_PATH_PARAMETER
                                )
                        ))
                .get("/organizations/{id}/deviceIds", organizationX.getId())
                .then()
                .statusCode(SC_FORBIDDEN);

        assetMgmtWireMockRule.verify(0, postRequestedFor(urlEqualTo("/assetmgmt/assets/search/property")).andMatching(retrievedAdminTokenAuthorizationHeader()));
    }

    @Test
    public void whenOrganizationDoesNotExist() {

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/get-device-ids/organization-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(
                                        ORGANIZATION_ID_PATH_PARAMETER
                                )
                        ))
                .get("/organizations/{id}/deviceIds", Long.MAX_VALUE)
                .then()
                .statusCode(SC_NOT_FOUND);

        assetMgmtWireMockRule.verify(0, postRequestedFor(urlEqualTo("/assetmgmt/assets/search/property")).andMatching(retrievedAdminTokenAuthorizationHeader()));
    }

    @Test
    public void whenAssetMgmtDoesNotReturnOK() throws JsonProcessingException {

        assetMgmtWireMockRule.stubFor(
                post(urlEqualTo("/assetmgmt/assets/search/property"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(organizationX.getOrganizationId())))
                        .willReturn(aResponse().withStatus(SC_UNAUTHORIZED)));

        given()
                .auth()
                .preemptive()
                .oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .get("/organizations/{id}/deviceIds", organizationX.getId())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
