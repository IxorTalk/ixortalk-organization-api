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
import com.ixortalk.organization.api.asset.Asset;
import com.ixortalk.organization.api.asset.AssetTestBuilder;
import com.ixortalk.organization.api.asset.DeviceId;
import com.ixortalk.organization.api.asset.DeviceInformationDTO;
import com.ixortalk.organization.api.util.ExpectedValueObjectSerializer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.restdocs.payload.RequestFieldsSnippet;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.organization.api.asset.DeviceId.deviceId;
import static com.ixortalk.organization.api.config.TestConstants.*;
import static com.ixortalk.test.util.FileUtil.jsonFile;
import static com.ixortalk.test.util.Randomizer.nextString;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.servlet.http.HttpServletResponse.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class OrganizationDevicesRestController_SaveDeviceInformation_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final DeviceId TEST_DEVICE = deviceId(nextString("testDevice"));

    private Asset asset;

    private DeviceInformationDTO deviceInformation;

    private static final RequestFieldsSnippet REQUEST_FIELDS_SNIPPET = requestFields(
            fieldWithPath("deviceName").type(STRING).description("The name of the device."),
            fieldWithPath("deviceInformation").type(STRING).description("Information of the device.")
    );

    @Before
    public void before() throws IOException {

        asset =
                AssetTestBuilder.anAsset()
                        .withOrganizationId(organizationX.getOrganizationId())
                        .withDeviceId(TEST_DEVICE)
                        .build();

        deviceInformation = objectMapper.readValue(jsonFile("deviceInformation.json"), DeviceInformationDTO.class);

        assetMgmtWireMockRule.stubFor(
                post(urlEqualTo("/assetmgmt/assets/find/property"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(ExpectedValueObjectSerializer.serializedDeviceId(TEST_DEVICE)))
                        .willReturn(okJson(objectMapper.writeValueAsString(asset))));

        assetMgmtWireMockRule.stubFor(
                put(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(deviceInformation)))
                        .willReturn(ok()));
    }

    @Test
    public void asAdmin() throws JsonProcessingException {

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(deviceInformation))
                .post("/organizations/{id}/devices/{deviceId}/save-info", organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_OK);

        assetMgmtWireMockRule.verify(
                putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(deviceInformation))));
    }

    @Test
    public void asOrganizationAdmin() throws JsonProcessingException {

        given()
            .auth()
            .preemptive()
            .oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
            .contentType(JSON)
            .filter(
                    document("organizations/save-info/ok",
                            preprocessRequest(staticUris(), prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(describeAuthorizationTokenHeader()),
                            DEVICE_IN_ORGANIZATION_PATH_PARAMETERS,
                            REQUEST_FIELDS_SNIPPET
                    ))
            .body(objectMapper.writeValueAsString(deviceInformation))
                .post("/organizations/{id}/devices/{deviceId}/save-info", organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
            .statusCode(SC_OK);

        assetMgmtWireMockRule.verify(
                putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(deviceInformation))));
    }

    @Test
    public void asUser() throws JsonProcessingException {

        given()
                .auth()
                .preemptive()
                .oauth2(USER_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/save-info/as-user",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS,
                                REQUEST_FIELDS_SNIPPET
                        ))
                .body(objectMapper.writeValueAsString(deviceInformation))
                .post("/organizations/{id}/devices/{deviceId}/save-info", organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_FORBIDDEN);

        assetMgmtWireMockRule.verify(0, postRequestedFor(urlEqualTo("/assetmgmt/assets/find/property")));
        assetMgmtWireMockRule.verify(0, putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
    }

    @Test
    public void asOrganizationAdminFromDifferentOrganization() throws JsonProcessingException {

        given()
                .auth()
                .preemptive()
                .oauth2(USER_IN_ORGANIZATION_Y_ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/save-info/different-admin",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS,
                                REQUEST_FIELDS_SNIPPET
                        ))
                .body(objectMapper.writeValueAsString(deviceInformation))
                .post("/organizations/{id}/devices/{deviceId}/save-info", organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_FORBIDDEN);

        assetMgmtWireMockRule.verify(0, postRequestedFor(urlEqualTo("/assetmgmt/assets/find/property")));
        assetMgmtWireMockRule.verify(0, putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
    }

    @Test
    public void organizationDoesNotExist() throws JsonProcessingException {

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/save-info/organization-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS,
                                REQUEST_FIELDS_SNIPPET
                        ))
                .body(objectMapper.writeValueAsString(deviceInformation))
                .post("/organizations/{id}/devices/{deviceId}/save-info", Long.MAX_VALUE, TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_NOT_FOUND);

        assetMgmtWireMockRule.verify(0, postRequestedFor(urlEqualTo("/assetmgmt/assets/find/property")));
        assetMgmtWireMockRule.verify(0, putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
    }

    @Test
    public void deviceIdDoesNotExist() throws JsonProcessingException {

        assetMgmtWireMockRule.stubFor(
                put(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .willReturn(aResponse()
                                .withStatus(SC_NOT_FOUND)));

        given()
                .auth()
                .preemptive()
                .oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/save-info/deviceid-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS,
                                REQUEST_FIELDS_SNIPPET
                        ))
                .body(objectMapper.writeValueAsString(deviceInformation))
                .post("/organizations/{id}/devices/{deviceId}/save-info", organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void assetMgmtFails() throws JsonProcessingException {

        assetMgmtWireMockRule.stubFor(
                put(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .willReturn(aResponse()
                                .withStatus(SC_INTERNAL_SERVER_ERROR)));

        given()
                .auth()
                .preemptive()
                .oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(deviceInformation))
                .post("/organizations/{id}/devices/{deviceId}/save-info", organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void otherAssetFieldsShouldNotBeUpdated() throws IOException {

        Object deviceInformation = objectMapper.readValue(jsonFile("deviceInformation.json"), Object.class);

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(deviceInformation))
                .post("/organizations/{id}/devices/{deviceId}/save-info", organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_OK);

        assetMgmtWireMockRule.verify(
                putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(this.deviceInformation))));
    }
}
