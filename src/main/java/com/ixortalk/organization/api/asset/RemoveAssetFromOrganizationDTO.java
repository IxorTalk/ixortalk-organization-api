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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.ixortalk.organization.api.domain.OrganizationId;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class RemoveAssetFromOrganizationDTO {

    private OrganizationId organizationId;
    private String image;

    private RemoveAssetFromOrganizationDTO(OrganizationId organizationId, String image) {
        this.organizationId = organizationId;
        this.image = image;
    }

    public static RemoveAssetFromOrganizationDTO removeAssetFromOrganizationDTO() {
        return new RemoveAssetFromOrganizationDTO(OrganizationId.noOrganizationId(), "");
    }

    @JsonUnwrapped
    public OrganizationId getOrganizationId() {
        return organizationId;
    }

    public String getImage() {
        return image;
    }

    // TODO #37: Below properties should not be hard-coded but "erase" all properties allowed to be saved
    @JsonProperty
    public List<?> getActions() {
        return newArrayList();
    }

    @JsonProperty
    public String getDeviceName() {
        return "";
    }

    @JsonProperty
    public String getDeviceInformation() {
        return "";
    }
}
