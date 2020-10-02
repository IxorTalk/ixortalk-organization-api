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

import com.ixortalk.organization.api.AbstractSpringIntegrationTest;
import com.ixortalk.organization.api.asset.Asset;
import com.ixortalk.organization.api.config.TestConstants;
import com.ixortalk.organization.api.util.ExpectedValueObjectSerializer;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.test.util.FileUtil.jsonFile;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class OrganizationRestController_AddDevice_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private Asset asset, asset2;

    @Before
    public void before() throws IOException {

        asset = objectMapper.readValue(jsonFile("assetWithoutOrganizationId.json"), Asset.class);
        asset2 = objectMapper.readValue(jsonFile("assetWithOrganizationId.json"), Asset.class);

        assetMgmtWireMockRule.stubFor(
                post(urlEqualTo("/assetmgmt/assets/find/property"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(ExpectedValueObjectSerializer.serializedDeviceId(asset.getDeviceId())))
                        .willReturn(okJson(jsonFile("assetWithoutOrganizationId.json"))));

        assetMgmtWireMockRule.stubFor(
                put(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .willReturn(ok()));

        assetMgmtWireMockRule.stubFor(
                post(urlEqualTo("/assetmgmt/assets/find/property"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(asset2.getDeviceId())))
                        .willReturn(aResponse()
                                .withStatus(SC_OK)
                                .withBody(
                                        jsonFile("assetWithOrganizationId.json")
                                )));
    }

    @Test
    public void asAdmin() throws JSONException {

        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .post("/organizations/{id}/devices/{deviceId}", organizationX.getId(), asset.getDeviceId().stringValue())
                .then()
                .statusCode(SC_OK);

        assetMgmtWireMockRule.verify(
                putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(new JSONObject().put("organizationId", organizationX.getId()).toString())));
    }

    @Test
    public void asOrganizationAdmin() throws JSONException {

        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/add-device/ok",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS
                        )
                )
                .post("/organizations/{id}/devices/{deviceId}", organizationX.getId(), asset.getDeviceId().stringValue())
                .then()
                .statusCode(SC_OK);

        assetMgmtWireMockRule.verify(
                putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(new JSONObject().put("organizationId", organizationX.getId()).toString())));
    }

    @Test
    public void asOrganizationAdminFromDifferentOrganization() {

        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.USER_IN_ORGANIZATION_Y_ADMIN_ROLE_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/add-device/different-admin",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS
                        )
                )
                .post("/organizations/{id}/devices/{deviceId}", organizationX.getId(), asset.getDeviceId().stringValue())
                .then()
                .statusCode(SC_FORBIDDEN);

        assetMgmtWireMockRule.verify(0, postRequestedFor(urlEqualTo("/assetmgmt/assets/find/property")));
        assetMgmtWireMockRule.verify(0, putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
    }

    @Test
    public void asUser() {

        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.USER_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/add-device/as-user",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS
                        )
                )
                .post("/organizations/{id}/devices/{deviceId}", organizationX.getId(), asset.getDeviceId().stringValue())
                .then()
                .statusCode(SC_FORBIDDEN);

        assetMgmtWireMockRule.verify(0, postRequestedFor(urlEqualTo("/assetmgmt/assets/find/property")));
        assetMgmtWireMockRule.verify(0, putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
    }

    @Test
    public void whenOtherOrganizationIdAlreadyPresentOnAsset() {

        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/add-device/role-present",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS
                        )
                )
                .post("/organizations/{id}/devices/{deviceId}", organizationX.getId(), asset2.getDeviceId().stringValue())
                .then()
                .statusCode(SC_BAD_REQUEST);

        assetMgmtWireMockRule.verify(0, putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset2.getAssetId().stringValue() + "/properties")));
    }

    @Test
    public void whenAssetDoesNotExist() {

        assetMgmtWireMockRule.stubFor(
                post(urlEqualTo("/assetmgmt/assets/find/property"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(ExpectedValueObjectSerializer.serializedDeviceId(asset.getDeviceId())))
                        .willReturn(aResponse()
                                .withStatus(SC_NOT_FOUND)
                        ));
        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/add-device/asset-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS
                        )
                )
                .post("/organizations/{id}/devices/{deviceId}", organizationX.getId(), asset.getDeviceId().stringValue())
                .then()
                .statusCode(SC_BAD_REQUEST);

        assetMgmtWireMockRule.verify(0, putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
    }

    @Test
    public void whenOrganizationDoesNotExist() {

        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/add-device/organization-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS
                        )
                )
                .post("/organizations/{id}/devices/{deviceId}", Long.MAX_VALUE, asset.getDeviceId().stringValue())
                .then()
                .statusCode(SC_NOT_FOUND);

        assetMgmtWireMockRule.verify(0, postRequestedFor(urlEqualTo("/assetmgmt/assets/find/property")));
        assetMgmtWireMockRule.verify(0, putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
    }

    @Test
    public void whenAssetMgmtDoesReturnOK() {

        assetMgmtWireMockRule.stubFor(
                post(urlEqualTo("/assetmgmt/assets/find/property"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(ExpectedValueObjectSerializer.serializedDeviceId(asset.getDeviceId())))
                        .willReturn(aResponse()
                                .withStatus(SC_UNAUTHORIZED)
                        ));
        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .post("/organizations/{id}/devices/{deviceId}", organizationX.getId(), asset.getDeviceId().stringValue())
                .then()
                .statusCode(SC_UNAUTHORIZED);

        assetMgmtWireMockRule.verify(0, putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
    }
}
