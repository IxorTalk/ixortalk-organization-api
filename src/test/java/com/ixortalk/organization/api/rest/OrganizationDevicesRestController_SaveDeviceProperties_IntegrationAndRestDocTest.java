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
import com.ixortalk.organization.api.asset.DeviceId;
import com.ixortalk.organization.api.util.ExpectedValueObjectSerializer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.restdocs.payload.RequestFieldsSnippet;

import java.io.IOException;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.organization.api.TestConstants.SAVE_DEVICE_PROPERTIES_PATH;
import static com.ixortalk.organization.api.TestConstants.SAVE_DEVICE_PROPERTY_ALLOWED_PROPERTY;
import static com.ixortalk.organization.api.TestConstants.SAVE_DEVICE_PROPERTY_OTHER_ALLOWED_PROPERTY;
import static com.ixortalk.organization.api.asset.AssetTestBuilder.anAsset;
import static com.ixortalk.organization.api.asset.DeviceId.deviceId;
import static com.ixortalk.organization.api.config.TestConstants.ADMIN_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_Y_ADMIN_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_JWT_TOKEN;
import static com.ixortalk.organization.api.rest.docs.RestDocDescriptors.TokenHeaderDescriptors.TOKEN_WITH_ORGANIZATION_ADMIN_PRIVILEGES;
import static com.ixortalk.organization.api.rest.docs.RestDocDescriptors.TokenHeaderDescriptors.TOKEN_WITH_USER_PRIVILEGES;
import static com.ixortalk.test.util.Randomizer.nextString;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;
import static wiremock.com.google.common.collect.Maps.newHashMap;

public class OrganizationDevicesRestController_SaveDeviceProperties_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final DeviceId TEST_DEVICE = deviceId(nextString("testDevice"));

    private Asset asset;

    private static final RequestFieldsSnippet REQUEST_FIELDS_SNIPPET = requestFields(
            fieldWithPath(SAVE_DEVICE_PROPERTY_ALLOWED_PROPERTY.configValue()).type(STRING).description("The value for `" + SAVE_DEVICE_PROPERTY_ALLOWED_PROPERTY.configValue() + "` to be saved."),
            fieldWithPath(SAVE_DEVICE_PROPERTY_OTHER_ALLOWED_PROPERTY.configValue()).type(STRING).description("The value for `" + SAVE_DEVICE_PROPERTY_OTHER_ALLOWED_PROPERTY.configValue() + "` to be saved.")
    );

    private Map<String, String> deviceProperties;

    @Before
    public void before() throws IOException {

        asset =
                anAsset()
                        .withOrganizationId(organizationX.getOrganizationId())
                        .withDeviceId(TEST_DEVICE)
                        .build();

        deviceProperties = newHashMap();
        deviceProperties.put(SAVE_DEVICE_PROPERTY_ALLOWED_PROPERTY.configValue(), nextString("allowedPropertyValue"));
        deviceProperties.put(SAVE_DEVICE_PROPERTY_OTHER_ALLOWED_PROPERTY.configValue(), nextString("otherAllowedPropertyValue"));

        assetMgmtWireMockRule.stubFor(
                post(urlEqualTo("/assetmgmt/assets/find/property"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(ExpectedValueObjectSerializer.serializedDeviceId(TEST_DEVICE)))
                        .willReturn(okJson(objectMapper.writeValueAsString(asset))));

        assetMgmtWireMockRule.stubFor(
                put(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(deviceProperties)))
                        .willReturn(ok()));
    }

    @Test
    public void asAdmin() throws JsonProcessingException {

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(deviceProperties))
                .post("/organizations/{id}/devices/{deviceId}/save-" + SAVE_DEVICE_PROPERTIES_PATH.configValue(), organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_OK);

        assetMgmtWireMockRule.verify(
                putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(deviceProperties))));
    }

    @Test
    public void asOrganizationAdmin() throws JsonProcessingException {

        given()
            .auth()
            .preemptive()
            .oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
            .contentType(JSON)
            .filter(
                    document("organizations/save-device-properties/ok",
                            preprocessRequest(staticUris(), prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestHeaders(TOKEN_WITH_ORGANIZATION_ADMIN_PRIVILEGES),
                            DEVICE_IN_ORGANIZATION_PATH_PARAMETERS,
                            REQUEST_FIELDS_SNIPPET
                    ))
            .body(objectMapper.writeValueAsString(deviceProperties))
                .post("/organizations/{id}/devices/{deviceId}/save-" + SAVE_DEVICE_PROPERTIES_PATH.configValue(), organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
            .statusCode(SC_OK);

        assetMgmtWireMockRule.verify(
                putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(deviceProperties))));
    }

    @Test
    public void asUser() throws JsonProcessingException {

        given()
                .auth()
                .preemptive()
                .oauth2(USER_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/save-device-properties/as-user",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(TOKEN_WITH_USER_PRIVILEGES),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS,
                                REQUEST_FIELDS_SNIPPET
                        ))
                .body(objectMapper.writeValueAsString(deviceProperties))
                .post("/organizations/{id}/devices/{deviceId}/save-" + SAVE_DEVICE_PROPERTIES_PATH.configValue(), organizationX.getId(), TEST_DEVICE.stringValue())
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
                        document("organizations/save-device-properties/different-admin",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(TOKEN_WITH_ORGANIZATION_ADMIN_PRIVILEGES),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS,
                                REQUEST_FIELDS_SNIPPET
                        ))
                .body(objectMapper.writeValueAsString(deviceProperties))
                .post("/organizations/{id}/devices/{deviceId}/save-" + SAVE_DEVICE_PROPERTIES_PATH.configValue(), organizationX.getId(), TEST_DEVICE.stringValue())
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
                        document("organizations/save-device-properties/organization-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(TOKEN_WITH_ORGANIZATION_ADMIN_PRIVILEGES),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS,
                                REQUEST_FIELDS_SNIPPET
                        ))
                .body(objectMapper.writeValueAsString(deviceProperties))
                .post("/organizations/{id}/devices/{deviceId}/save-" + SAVE_DEVICE_PROPERTIES_PATH.configValue(), Long.MAX_VALUE, TEST_DEVICE.stringValue())
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
                        document("organizations/save-device-properties/deviceid-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(TOKEN_WITH_ORGANIZATION_ADMIN_PRIVILEGES),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS,
                                REQUEST_FIELDS_SNIPPET
                        ))
                .body(objectMapper.writeValueAsString(deviceProperties))
                .post("/organizations/{id}/devices/{deviceId}/save-" + SAVE_DEVICE_PROPERTIES_PATH.configValue(), organizationX.getId(), TEST_DEVICE.stringValue())
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
                .body(objectMapper.writeValueAsString(deviceProperties))
                .post("/organizations/{id}/devices/{deviceId}/save-" + SAVE_DEVICE_PROPERTIES_PATH.configValue(), organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void unallowedProperty() throws IOException {

        deviceProperties.put("anUnallowedProperty", "someValue");

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/save-device-properties/unallowed-property",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(TOKEN_WITH_ORGANIZATION_ADMIN_PRIVILEGES),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS
                        ))
                .body(objectMapper.writeValueAsString(deviceProperties))
                .post("/organizations/{id}/devices/{deviceId}/save-" + SAVE_DEVICE_PROPERTIES_PATH.configValue(), organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_FORBIDDEN);

        assetMgmtWireMockRule.verify(0, putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
    }

    @Test
    public void unallowedPath() throws JsonProcessingException {
        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/save-device-properties/unallowed-path",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(TOKEN_WITH_ORGANIZATION_ADMIN_PRIVILEGES),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS,
                                REQUEST_FIELDS_SNIPPET
                        ))
                .body(objectMapper.writeValueAsString(deviceProperties))
                .post("/organizations/{id}/devices/{deviceId}/save-unallowed-path", organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_NOT_FOUND);

        assetMgmtWireMockRule.verify(0, putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
    }
}
