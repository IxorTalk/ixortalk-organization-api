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
package com.ixortalk.organization.api.asset;

import com.ixortalk.organization.api.AbstractSpringIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static com.ixortalk.organization.api.TestConstants.JsonTestConstants.*;
import static com.ixortalk.organization.api.TestConstants.UNMAPPED_DEVICE_INFO_FIELD;
import static com.ixortalk.organization.api.asset.DeviceId.deviceId;
import static com.ixortalk.organization.api.asset.Properties.MappedField.IMAGE;
import static com.ixortalk.organization.api.domain.OrganizationId.organizationId;
import static com.ixortalk.test.util.FileUtil.jsonFile;
import static org.assertj.core.api.Assertions.assertThat;

public class Asset_JsonMapping_IntegrationTest extends AbstractSpringIntegrationTest {

    private Asset assetWithoutOrganizationId;
    private Asset assetWithOrganizationId;

    @Before
    public void before() throws IOException {
        assetWithoutOrganizationId = objectMapper.readValue(jsonFile("assetWithoutOrganizationId.json"), Asset.class);
        assetWithOrganizationId = objectMapper.readValue(jsonFile("assetWithOrganizationId.json"), Asset.class);
    }

    @Test
    public void getMappedField() {
        assertThat(assetWithOrganizationId.getOrganizationId()).isPresent().hasValue(organizationId(ASSET_WITH_ORGANIZATION_ID_ORGANIZATION_ID.configValueAsLong()));
        assertThat(assetWithOrganizationId.getDeviceId()).isEqualTo(deviceId(ASSET_WITH_ORGANIZATION_ID_DEVICE_ID.configValue()));

        assertThat(assetWithoutOrganizationId.getOrganizationId()).isNotPresent();
        assertThat(assetWithoutOrganizationId.getActions()).asList().first().isInstanceOfSatisfying(Map.class, map -> assertThat(map.get("name")).isEqualTo(ASSET_WITHOUT_ORGANIZATION_FIRST_ACTION_S_NAME.configValue()));
    }

    @Test
    public void getUnmappedProperty() {
        assertThat(assetWithoutOrganizationId.getAssetProperty(UNMAPPED_DEVICE_INFO_FIELD.configValue())).isEqualTo(ASSET_WITHOUT_ORGANIZATION_ID_DEVICE_NAME.configValue());
    }

    @Test
    public void getMappedFieldAsProperty() {
        assertThat(assetWithOrganizationId.getAssetProperty("organizationId")).isEqualTo(ASSET_WITH_ORGANIZATION_ID_ORGANIZATION_ID.configValueAsLong());
        assertThat(assetWithOrganizationId.getAssetProperty("deviceId")).isEqualTo(ASSET_WITH_ORGANIZATION_ID_DEVICE_ID.configValue());

        assertThat(assetWithoutOrganizationId.getAssetProperty("actions")).asList().first().isInstanceOfSatisfying(Map.class, map -> assertThat(map.get("name")).isEqualTo(ASSET_WITHOUT_ORGANIZATION_FIRST_ACTION_S_NAME.configValue()));
    }

    @Test
    public void getImage() {
        assertThat(assetWithoutOrganizationId.getAssetProperty(IMAGE.getPropertyName())).isEqualTo("https://my-organization/image.png");

    }
}