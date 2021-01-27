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
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.google.common.collect.Lists.newArrayList;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.organization.api.TestConstants.ORGANIZATION_REMOVED_CALLBACK_PATH;
import static com.ixortalk.organization.api.asset.AssetId.assetId;
import static com.ixortalk.organization.api.config.TestConstants.ADMIN_JWT_TOKEN;
import static com.ixortalk.test.util.Randomizer.nextString;
import static io.restassured.RestAssured.given;
import static java.lang.String.valueOf;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@TestPropertySource(properties = {"ixortalk.server.assetmgmt.url=false"})
public class OrganizationRestController_DeleteCascade_WithoutAssetMgmt_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    public static final AssetId FIRST_DEVICE_ASSET_ID = assetId(nextString("firstDeviceAssetId"));
    public static final AssetId SECOND_DEVICE_ASSET_ID = assetId(nextString("secondDeviceAssetId"));

    @Before
    public void before() throws JsonProcessingException {

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
    public void assetMgmtNotCalledOnCascadedDelete() throws JsonProcessingException {

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .delete("/organizations/{id}/cascade", organizationX.getId())
                .then()
                .statusCode(SC_NO_CONTENT);

        assetMgmtWireMockRule.verify(0,
                putRequestedFor(urlEqualTo("/assetmgmt/assets/" + FIRST_DEVICE_ASSET_ID.stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(assetMgmtFieldsToBeCleared()))));
        assetMgmtWireMockRule.verify(0,
                putRequestedFor(urlEqualTo("/assetmgmt/assets/" + SECOND_DEVICE_ASSET_ID.stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(assetMgmtFieldsToBeCleared()))));
    }
}
