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
import org.junit.Before;
import org.junit.Test;
import org.springframework.restdocs.request.RequestPartsSnippet;

import javax.servlet.http.HttpServletResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.organization.api.asset.AssetTestBuilder.anAsset;
import static com.ixortalk.organization.api.asset.Properties.MappedField.IMAGE;
import static com.ixortalk.organization.api.config.TestConstants.*;
import static com.ixortalk.organization.api.util.ExpectedValueObjectSerializer.serializedDeviceId;
import static io.restassured.RestAssured.given;
import static java.util.Collections.singletonMap;
import static org.apache.http.HttpHeaders.LOCATION;
import static org.apache.http.HttpStatus.*;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.MULTIPART_FORM_DATA;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class OrganizationRestController_UploadDeviceImage_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final String ORIGINAL_IMAGE_CONTENT_TYPE = IMAGE_PNG_VALUE;
    private static final String FILE_REQUEST_PART_NAME = "file";
    private static final String ORIGINAL_IMAGE_FILE_NAME = "original.png";
    private static final String IMAGE_LOCATION = "the/key/51aa812d-69fa-47d9-a880-30c9413a4ec9/original";

    private static final RequestPartsSnippet REQUEST_PARTS = requestParts(
            partWithName(FILE_REQUEST_PART_NAME).description("The (required) actual image file part.")
    );

    private Asset asset;

    @Before
    public void before() throws JsonProcessingException {

        asset =
                anAsset()
                        .withOrganizationId(organizationX.getOrganizationId())
                        .withDeviceId(TEST_DEVICE)
                        .build();

        assetMgmtWireMockRule.stubFor(
                post(urlPathEqualTo("/assetmgmt/assets/find/property"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(serializedDeviceId(TEST_DEVICE)))
                        .willReturn(okJson(objectMapper.writeValueAsString(asset))));

        assetMgmtWireMockRule.stubFor(
                put(urlPathEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .willReturn(ok()));

        imageServiceWireMockRule.stubFor(
                post(urlPathEqualTo("/image/upload"))
                        .withMultipartRequestBody(aMultipart().withName("key").withBody(equalTo("organizations/" + organizationX.getId() + "/" + TEST_DEVICE.stringValue() + "/image")))
                        .withMultipartRequestBody(aMultipart().withName("file").withBody(binaryEqualTo(originalImageBytes)))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .willReturn(ok().withHeader(LOCATION, IMAGE_LOCATION)));
    }

    @Test
    public void asAdmin() throws JsonProcessingException {

        given()
                .auth().preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .contentType(MULTIPART_FORM_DATA)
                .multiPart(FILE_REQUEST_PART_NAME, ORIGINAL_IMAGE_FILE_NAME, originalImageBytes, ORIGINAL_IMAGE_CONTENT_TYPE)
                .post("/organizations/{id}/devices/{deviceId}/image", organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_CREATED)
                .header(LOCATION, IMAGE_LOCATION);

        imageServiceWireMockRule.verify(1, postRequestedFor(urlPathEqualTo("/image/upload")));
        assetMgmtWireMockRule.verify(1,
                putRequestedFor(urlPathEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .withRequestBody(equalToJson(objectMapper.writeValueAsString(singletonMap(IMAGE.getPropertyName(), IMAGE_LOCATION))))
        );
    }

    @Test
    public void asOrganizationAdmin() throws JsonProcessingException {

        given()
                .auth().preemptive()
                .oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .when()
                .filter(
                        document("organizations/upload-device-image/ok",
                                preprocessRequest(staticUris(), prettyPrint(), removeBinaryContent()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS,
                                REQUEST_PARTS,
                                responseHeaders(headerWithName(LOCATION).description("Contains the actual path to the stored original image."))
                        )
                )
                .contentType(MULTIPART_FORM_DATA)
                .multiPart(FILE_REQUEST_PART_NAME, ORIGINAL_IMAGE_FILE_NAME, originalImageBytes, ORIGINAL_IMAGE_CONTENT_TYPE)
                .post("/organizations/{id}/devices/{deviceId}/image", organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_CREATED)
                .header(LOCATION, IMAGE_LOCATION);

        imageServiceWireMockRule.verify(1, postRequestedFor(urlPathEqualTo("/image/upload")));
        assetMgmtWireMockRule.verify(1,
                putRequestedFor(urlPathEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties"))
                        .withRequestBody(equalTo(objectMapper.writeValueAsString(singletonMap(IMAGE.getPropertyName(), IMAGE_LOCATION))))
        );
    }

    @Test
    public void asDifferentOrganizationAdmin() {

        given()
                .auth().preemptive()
                .oauth2(USER_IN_ORGANIZATION_Y_ADMIN_JWT_TOKEN)
                .when()
                .filter(
                        document("organizations/upload-device-image/different-organization-admin",
                                preprocessRequest(staticUris(), prettyPrint(), removeBinaryContent()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS
                        )
                )
                .contentType(MULTIPART_FORM_DATA)
                .multiPart(FILE_REQUEST_PART_NAME, ORIGINAL_IMAGE_FILE_NAME, originalImageBytes, ORIGINAL_IMAGE_CONTENT_TYPE)
                .post("/organizations/{id}/devices/{deviceId}/image", organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_FORBIDDEN);

        assetMgmtWireMockRule.verify(0, postRequestedFor(urlEqualTo("/assetmgmt/assets/find/property")));
        assetMgmtWireMockRule.verify(0, putRequestedFor(urlPathEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
        imageServiceWireMockRule.verify(0, postRequestedFor(urlPathEqualTo("/image/upload")));
    }

    @Test
    public void asUser() {

        given()
                .auth().preemptive()
                .oauth2(USER_JWT_TOKEN)
                .when()
                .filter(
                        document("organizations/upload-device-image/as-user",
                                preprocessRequest(staticUris(), prettyPrint(), removeBinaryContent()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS
                        )
                )
                .contentType(MULTIPART_FORM_DATA)
                .multiPart(FILE_REQUEST_PART_NAME, ORIGINAL_IMAGE_FILE_NAME, originalImageBytes, ORIGINAL_IMAGE_CONTENT_TYPE)
                .post("/organizations/{id}/devices/{deviceId}/image", organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_FORBIDDEN);

        assetMgmtWireMockRule.verify(0, postRequestedFor(urlEqualTo("/assetmgmt/assets/find/property")));
        assetMgmtWireMockRule.verify(0, putRequestedFor(urlPathEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
        imageServiceWireMockRule.verify(0, postRequestedFor(urlPathEqualTo("/image/upload")));
    }

    @Test
    public void organizationDoesNotExist() {

        given()
                .auth().preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .filter(
                        document("organizations/upload-device-image/does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint(), removeBinaryContent()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS
                        )
                )
                .contentType(MULTIPART_FORM_DATA)
                .multiPart(FILE_REQUEST_PART_NAME, ORIGINAL_IMAGE_FILE_NAME, originalImageBytes, ORIGINAL_IMAGE_CONTENT_TYPE)
                .post("/organizations/{id}/devices/{deviceId}/image", Long.MAX_VALUE, TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_NOT_FOUND);

        assetMgmtWireMockRule.verify(0, postRequestedFor(urlEqualTo("/assetmgmt/assets/find/property")));
        assetMgmtWireMockRule.verify(0, putRequestedFor(urlPathEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
        imageServiceWireMockRule.verify(0, postRequestedFor(urlPathEqualTo("/image/upload")));
    }

    @Test
    public void deviceDoesNotExist() {

        assetMgmtWireMockRule.stubFor(
                post(urlPathEqualTo("/assetmgmt/assets/find/property"))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .willReturn(aResponse()
                                .withStatus(HttpServletResponse.SC_NOT_FOUND)
                        ));

        given()
                .auth().preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .filter(
                        document("organizations/upload-device-image/device-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint(), removeBinaryContent()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS
                        )
                )
                .contentType(MULTIPART_FORM_DATA)
                .multiPart(FILE_REQUEST_PART_NAME, ORIGINAL_IMAGE_FILE_NAME, originalImageBytes, ORIGINAL_IMAGE_CONTENT_TYPE)
                .post("/organizations/{id}/devices/{deviceId}/image", organizationX.getId(), Long.MAX_VALUE)
                .then()
                .statusCode(SC_NOT_FOUND);

        assetMgmtWireMockRule.verify(0, putRequestedFor(urlPathEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
        imageServiceWireMockRule.verify(0, postRequestedFor(urlPathEqualTo("/image/upload")));
    }

    @Test
    public void fileRequestPartMissing() {

        given()
                .auth().preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .filter(
                        document("organizations/upload-device-image/file-part-missing",
                                preprocessRequest(staticUris(), prettyPrint(), removeBinaryContent()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS
                        )
                )
                .contentType(MULTIPART_FORM_DATA)
                .multiPart("", ORIGINAL_IMAGE_FILE_NAME, originalImageBytes, ORIGINAL_IMAGE_CONTENT_TYPE)
                .post("/organizations/{id}/devices/{deviceId}/image", organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_BAD_REQUEST);

        assetMgmtWireMockRule.verify(0, postRequestedFor(urlEqualTo("/assetmgmt/assets/find/property")));
        assetMgmtWireMockRule.verify(0, putRequestedFor(urlPathEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
        imageServiceWireMockRule.verify(0, postRequestedFor(urlPathEqualTo("/image/upload")));
    }

    @Test
    public void fileNamePartMissing() {

        given()
                .auth().preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .filter(
                        document("organizations/upload-device-image/filename-missing",
                                preprocessRequest(staticUris(), prettyPrint(), removeBinaryContent()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                DEVICE_IN_ORGANIZATION_PATH_PARAMETERS
                        )
                )
                .contentType(MULTIPART_FORM_DATA)
                .multiPart(FILE_REQUEST_PART_NAME, null, originalImageBytes, ORIGINAL_IMAGE_CONTENT_TYPE)
                .post("/organizations/{id}/devices/{deviceId}/image", organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_BAD_REQUEST);

        assetMgmtWireMockRule.verify(0, postRequestedFor(urlEqualTo("/assetmgmt/assets/find/property")));
        assetMgmtWireMockRule.verify(0, putRequestedFor(urlPathEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
        imageServiceWireMockRule.verify(0, postRequestedFor(urlPathEqualTo("/image/upload")));
    }

    @Test
    public void imageServiceDoesNotReturnOK() {

        imageServiceWireMockRule.stubFor(
                post(urlPathEqualTo("/image/upload"))
                        .willReturn(aResponse()
                                .withStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                        ));

        given()
                .auth().preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .contentType(MULTIPART_FORM_DATA)
                .multiPart(FILE_REQUEST_PART_NAME, ORIGINAL_IMAGE_FILE_NAME, originalImageBytes, ORIGINAL_IMAGE_CONTENT_TYPE)
                .post("/organizations/{id}/devices/{deviceId}/image", organizationX.getId(), TEST_DEVICE.stringValue())
                .then()
                .statusCode(SC_INTERNAL_SERVER_ERROR);

        assetMgmtWireMockRule.verify(0, putRequestedFor(urlPathEqualTo("/assetmgmt/assets/" + asset.getAssetId().stringValue() + "/properties")));
    }
}
