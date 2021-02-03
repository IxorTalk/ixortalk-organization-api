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
import com.ixortalk.organization.api.config.IxorTalkConfigProperties;
import io.restassured.path.json.JsonPath;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.restdocs.request.PathParametersSnippet;

import javax.inject.Inject;
import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.google.common.collect.Lists.newArrayList;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.organization.api.TestConstants.MAPPED_DEVICE_INFO_FIELD;
import static com.ixortalk.organization.api.TestConstants.UNMAPPED_DEVICE_INFO_FIELD;
import static com.ixortalk.organization.api.asset.AssetTestBuilder.anAsset;
import static com.ixortalk.organization.api.asset.DeviceId.deviceId;
import static com.ixortalk.organization.api.config.TestConstants.ADMIN_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_Y_ADMIN_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_JWT_TOKEN;
import static com.ixortalk.organization.api.rest.OrganizationDevicesRestController.DEVICE_STATE_FIELD_NAME;
import static com.ixortalk.test.util.Randomizer.nextString;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class OrganizationDevicesRestController_GetDevices_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    public static final String ASSET_UNMAPPED_DEVICE_INFO_FIELD_VALUE = "assetUnmappedDeviceInfoFieldValue";
    public static final String ASSET_2_UNMAPPED_DEVICE_INFO_FIELD_VALUE = "asset2UnmappedDeviceInfoFieldValue";

    private Asset asset, asset2;

    @Inject
    private IxorTalkConfigProperties ixorTalkConfigProperties;

    private static final DeviceId OTHER_DEVICE_ID = deviceId(nextString("otherDeviceId"));

    private static final PathParametersSnippet PATH_PARAMETERS_SNIPPET = pathParameters(
            parameterWithName("id").description("The id of the organization.")
    );

    private static final ResponseFieldsSnippet RESPONSE_FIELDS_SNIPPET =
            responseFields(
                    fieldWithPath("[].deviceId").type(STRING).description("The `deviceId` of the asset."),
                    fieldWithPath("[].deviceState").type(STRING).description("The link to the state of the device."),
                    fieldWithPath("[].deviceName").type(STRING).description("Name of the device.")
            );


    @Before
    public void before() throws IOException {

        asset = anAsset().withDeviceId(TEST_DEVICE).withOtherProperty(UNMAPPED_DEVICE_INFO_FIELD.configValue(), ASSET_UNMAPPED_DEVICE_INFO_FIELD_VALUE).build();
        asset2 = anAsset().withDeviceId(OTHER_DEVICE_ID).withOtherProperty(UNMAPPED_DEVICE_INFO_FIELD.configValue(), ASSET_2_UNMAPPED_DEVICE_INFO_FIELD_VALUE).build();

        assetMgmtWireMockRule.stubFor(
                post(urlEqualTo("/assetmgmt/assets/search/property"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(organizationX.getOrganizationId())))
                        .willReturn(okJson(objectMapper.writeValueAsString(newArrayList(asset, asset2)))));
    }

    @Test
    public void asAdmin() {

        JsonPath devices =
                given()
                        .auth()
                        .preemptive()
                        .oauth2(ADMIN_JWT_TOKEN)
                        .contentType(JSON)
                        .get("/organizations/{id}/devices", organizationX.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(devices.getString(MAPPED_DEVICE_INFO_FIELD.configValue())).contains(TEST_DEVICE.stringValue(), OTHER_DEVICE_ID.stringValue());
        assertThat(devices.getString(UNMAPPED_DEVICE_INFO_FIELD.configValue())).contains(ASSET_UNMAPPED_DEVICE_INFO_FIELD_VALUE, ASSET_2_UNMAPPED_DEVICE_INFO_FIELD_VALUE);

        assetMgmtWireMockRule.verify(
                postRequestedFor(urlEqualTo("/assetmgmt/assets/search/property"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader()));
    }

    @Test
    public void asOrganizationAdmin() {

        JsonPath devices = given()
                .auth()
                .preemptive()
                .oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/get-devices/ok",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET,
                                RESPONSE_FIELDS_SNIPPET
                        ))
                .get("/organizations/{id}/devices", organizationX.getId())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath();


        assertThat(devices.getString(MAPPED_DEVICE_INFO_FIELD.configValue())).contains(TEST_DEVICE.stringValue(), OTHER_DEVICE_ID.stringValue());
        assertThat(devices.getString(UNMAPPED_DEVICE_INFO_FIELD.configValue())).contains(ASSET_UNMAPPED_DEVICE_INFO_FIELD_VALUE, ASSET_2_UNMAPPED_DEVICE_INFO_FIELD_VALUE);

        assetMgmtWireMockRule.verify(
                postRequestedFor(urlEqualTo("/assetmgmt/assets/search/property"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader()));
    }

    @Test
    public void asOrganizationAdminFromDifferentOrganization() {

        given()
                .auth()
                .preemptive()
                .oauth2(USER_IN_ORGANIZATION_Y_ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/get-devices/different-admin",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        ))
                .get("/organizations/{id}/devices", organizationX.getId())
                .then()
                .statusCode(SC_FORBIDDEN);

        assetMgmtWireMockRule.verify(0, postRequestedFor(urlEqualTo("/assetmgmt/assets/search/property")).andMatching(retrievedAdminTokenAuthorizationHeader()));
    }

    @Test
    public void asUser() {

        given()
                .auth()
                .preemptive()
                .oauth2(USER_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/get-devices/as-user",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        ))
                .get("/organizations/{id}/devices", organizationX.getId())
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
                        document("organizations/get-devices/organization-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        ))
                .get("/organizations/{id}/devices", Long.MAX_VALUE)
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
                .get("/organizations/{id}/devices", organizationX.getId())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void getAssetStateLink() {
        JsonPath jsonPath =
                given()
                        .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .when()
                        .get("/organizations/{id}/devices", organizationX.getId())
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getString(DEVICE_STATE_FIELD_NAME))
                .contains(
                        ixorTalkConfigProperties.getLoadbalancer().getExternal().getUrlWithoutStandardPorts() + ixorTalkConfigProperties.getMicroservice("assetstate").getContextPath() + "/states/" + asset.getDeviceId().toString(),
                        ixorTalkConfigProperties.getLoadbalancer().getExternal().getUrlWithoutStandardPorts() + ixorTalkConfigProperties.getMicroservice("assetstate").getContextPath() + "/states/" + asset2.getDeviceId().toString());
    }
}
