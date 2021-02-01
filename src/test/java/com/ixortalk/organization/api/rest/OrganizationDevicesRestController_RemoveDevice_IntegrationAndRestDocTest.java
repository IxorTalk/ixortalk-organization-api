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
import com.ixortalk.organization.api.config.TestConstants;
import com.ixortalk.organization.api.rest.dto.DeviceInOrganizationDTO;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.organization.api.TestConstants.DEVICE_REMOVED_CALLBACK_PATH;
import static com.ixortalk.organization.api.asset.DeviceId.deviceId;
import static com.ixortalk.organization.api.asset.Properties.MappedField.IMAGE;
import static com.ixortalk.organization.api.util.ExpectedValueObjectSerializer.serializedDeviceId;
import static com.ixortalk.test.util.Randomizer.nextString;
import static io.restassured.RestAssured.given;
import static javax.servlet.http.HttpServletResponse.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class OrganizationDevicesRestController_RemoveDevice_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final String REMOVE_DEVICE_SCENARIO = "removeDeviceScenario";
    private static final String ASSET_PROPERTIES_CLEARED = "assetPropertiesCleared";
    private static final DeviceId DEVICE_TO_REMOVE = deviceId(nextString("deviceToRemove"));

    private Asset asset;

    private DeviceInOrganizationDTO deviceInOrganizationDTO;

    @Before
    public void before() throws IOException {

        asset =
                AssetTestBuilder.anAsset()
                        .withOrganizationId(organizationX.getOrganizationId())
                        .withDeviceId(DEVICE_TO_REMOVE)
                        .withOtherProperty(IMAGE.getPropertyName(), "https://organization_x.com/image.png")
                        .withActions("existing actions within organization X")
                        .withOtherProperty("deviceName", "The device's name within Organization X")
                        .withOtherProperty("deviceInformation", "The device information within Organization X")
                        .build();

        deviceInOrganizationDTO = new DeviceInOrganizationDTO(organizationX.getId(), DEVICE_TO_REMOVE);

        assetMgmtWireMockRule.stubFor(
                post(urlEqualTo("/assetmgmt/assets/find/property"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(serializedDeviceId(DEVICE_TO_REMOVE)))
                        .willReturn(okJson(objectMapper.writeValueAsString(asset))));

        assetMgmtWireMockRule.stubFor(
                put(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(assetMgmtFieldsToBeCleared())))
                        .willReturn(ok()));

        organizationCallbackApiWireMockRule.stubFor(post(urlEqualTo("/org-callback-api" + DEVICE_REMOVED_CALLBACK_PATH.configValue()))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(ok()));
    }

    @Test
    public void asAdmin() throws JsonProcessingException {

        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.ADMIN_JWT_TOKEN)
                .delete("/organizations/{id}/devices/{deviceId}", organizationX.getId(), DEVICE_TO_REMOVE.stringValue())
                .then()
                .statusCode(SC_NO_CONTENT);

        assetMgmtWireMockRule.verify(
                putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(assetMgmtFieldsToBeCleared()))));

        organizationCallbackApiWireMockRule.verify(1,
                postRequestedFor(urlEqualTo("/org-callback-api" + DEVICE_REMOVED_CALLBACK_PATH.configValue()))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(feignObjectMapper.writeValueAsString(deviceInOrganizationDTO))));
    }

    @Test
    public void asOrganizationAdmin() throws JsonProcessingException {

        given()
                .auth().preemptive()
                .oauth2(TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .filter(
                        document("organizations/remove-device/ok",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS
                        )
                )
                .delete("/organizations/{id}/devices/{deviceId}", organizationX.getId(), DEVICE_TO_REMOVE.stringValue())
                .then()
                .statusCode(SC_NO_CONTENT);

        assetMgmtWireMockRule.verify(
                putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(assetMgmtFieldsToBeCleared()))));

        organizationCallbackApiWireMockRule.verify(1,
                postRequestedFor(urlEqualTo("/org-callback-api" + DEVICE_REMOVED_CALLBACK_PATH.configValue()))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(feignObjectMapper.writeValueAsString(deviceInOrganizationDTO))));
    }

    @Test
    public void asUser() {

        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.USER_JWT_TOKEN)
                .delete("/organizations/{id}/devices/{deviceId}", organizationX.getId(), DEVICE_TO_REMOVE.stringValue())
                .then()
                .statusCode(SC_FORBIDDEN);

        assetMgmtWireMockRule.verify(0, postRequestedFor(urlEqualTo("/assetmgmt/assets/find/property")));
        assetMgmtWireMockRule.verify(0, putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
        organizationCallbackApiWireMockRule.verify(0, postRequestedFor(urlEqualTo("/org-callback-api" + DEVICE_REMOVED_CALLBACK_PATH.configValue())));
    }

    @Test
    public void asOrganizationAdminFromDifferentOrganization() {

        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.USER_IN_ORGANIZATION_Y_ADMIN_JWT_TOKEN)
                .filter(
                        document("organizations/remove-device/no-access",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS
                        )
                )
                .delete("/organizations/{id}/devices/{deviceId}", organizationX.getId(), DEVICE_TO_REMOVE.stringValue())
                .then()
                .statusCode(SC_FORBIDDEN);

        assetMgmtWireMockRule.verify(0, postRequestedFor(urlEqualTo("/assetmgmt/assets/find/property")));
        assetMgmtWireMockRule.verify(0, putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
        organizationCallbackApiWireMockRule.verify(0, postRequestedFor(urlEqualTo("/org-callback-api" + DEVICE_REMOVED_CALLBACK_PATH.configValue())));
    }

    @Test
    public void organizationDoesNotExist() {

        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.ADMIN_JWT_TOKEN)
                .filter(
                        document("organizations/remove-device/organization-doesnt-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS
                        )
                )
                .delete("/organizations/{id}/devices/{deviceId}", Long.MAX_VALUE, DEVICE_TO_REMOVE.stringValue())
                .then()
                .statusCode(SC_NOT_FOUND);

        assetMgmtWireMockRule.verify(0, postRequestedFor(urlEqualTo("/assetmgmt/assets/find/property")));
        assetMgmtWireMockRule.verify(0, putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
        organizationCallbackApiWireMockRule.verify(0, postRequestedFor(urlEqualTo("/org-callback-api" + DEVICE_REMOVED_CALLBACK_PATH.configValue())));
    }

    @Test
    public void deviceIdDoesNotExist() {

        assetMgmtWireMockRule.stubFor(
                post(urlEqualTo("/assetmgmt/assets/find/property"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(serializedDeviceId(DEVICE_TO_REMOVE)))
                        .willReturn(notFound()));

        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.ADMIN_JWT_TOKEN)
                .filter(
                        document("organizations/remove-device/device-doesnt-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS
                        )
                )
                .delete("/organizations/{id}/devices/{deviceId}", organizationX.getId(), DEVICE_TO_REMOVE.stringValue())
                .then()
                .statusCode(SC_NOT_FOUND);

        assetMgmtWireMockRule.verify(0, putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
        organizationCallbackApiWireMockRule.verify(0, postRequestedFor(urlEqualTo("/org-callback-api" + DEVICE_REMOVED_CALLBACK_PATH.configValue())));
    }

    @Test
    public void assetMgmtFails() {

        assetMgmtWireMockRule.stubFor(
                put(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .inScenario(REMOVE_DEVICE_SCENARIO)
                        .whenScenarioStateIs(STARTED)
                        .willSetStateTo(ASSET_PROPERTIES_CLEARED)
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .willReturn(serviceUnavailable()));

        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.ADMIN_JWT_TOKEN)
                .delete("/organizations/{id}/devices/{deviceId}", organizationX.getId(), DEVICE_TO_REMOVE.stringValue())
                .then()
                .statusCode(SC_SERVICE_UNAVAILABLE);

        assetMgmtWireMockRule.verify(1, putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
    }

    @Test
    public void organizationCallbackNotFound() {

        organizationCallbackApiWireMockRule.stubFor(post(urlEqualTo("/org-callback-api" + DEVICE_REMOVED_CALLBACK_PATH.configValue()))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(notFound()));

        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.ADMIN_JWT_TOKEN)
                .delete("/organizations/{id}/devices/{deviceId}", organizationX.getId(), DEVICE_TO_REMOVE.stringValue())
                .then()
                .statusCode(SC_NOT_FOUND);

        assetMgmtWireMockRule.verify(0, putRequestedFor(urlEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
    }
}
