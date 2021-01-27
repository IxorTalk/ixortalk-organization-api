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
import com.ixortalk.organization.api.domain.Organization;
import com.ixortalk.organization.api.domain.OrganizationTestBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.google.common.collect.Lists.newArrayList;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.organization.api.TestConstants.ORGANIZATION_PRE_DELETE_CHECK_CALLBACK_PATH;
import static com.ixortalk.organization.api.config.TestConstants.ADMIN_JWT_TOKEN;
import static io.restassured.RestAssured.given;
import static java.lang.String.valueOf;
import static java.util.Optional.of;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@TestPropertySource(properties = {"ixortalk.server.assetmgmt.url=false"})
public class OrganizationRestResource_Delete_WithoutAssetMgmt_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

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

        assetMgmtWireMockRule.stubFor(
                post(urlEqualTo("/assetmgmt/assets/search/property"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(organizationToDelete.getOrganizationId())))
                        .willReturn(okJson(objectMapper.writeValueAsString(newArrayList()))));
    }

    @Test
    public void assetMgmtNotCalledOnDelete() throws JsonProcessingException {

        assetMgmtWireMockRule.stubFor(
                post(urlEqualTo("/assetmgmt/assets/search/property"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(organizationToDelete.getOrganizationId())))
                        .willReturn(okJson(objectMapper.writeValueAsString(newArrayList(AssetTestBuilder.anAsset().build())))));

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .delete("/organizations/{id}", organizationToDelete.getId())
                .then()
                .statusCode(SC_NO_CONTENT);

        assertThat(organizationRestResource.findById(organizationToDelete.getId())).isNotPresent();
    }
}
