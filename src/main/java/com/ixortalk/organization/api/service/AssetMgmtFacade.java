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
package com.ixortalk.organization.api.service;

import com.ixortalk.organization.api.asset.Asset;
import com.ixortalk.organization.api.asset.AssetMgmt;
import com.ixortalk.organization.api.config.IxorTalkConfigProperties;
import com.ixortalk.organization.api.domain.Organization;
import com.ixortalk.organization.api.domain.OrganizationId;
import com.ixortalk.organization.api.rest.OrganizationRestResource;
import com.ixortalk.organization.api.rest.dto.DeviceInOrganizationDTO;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.ixortalk.organization.api.asset.RemoveAssetFromOrganizationDTO.removeAssetFromOrganizationDTO;
import static java.util.Optional.ofNullable;

public class AssetMgmtFacade {

    private AssetMgmt assetMgmt;

    private OrganizationRestResource organizationRestResource;
    private IxorTalkConfigProperties ixorTalkConfigProperties;

    public AssetMgmtFacade(AssetMgmt assetMgmt, OrganizationRestResource organizationRestResource, IxorTalkConfigProperties ixorTalkConfigProperties) {
        this.assetMgmt = assetMgmt;
        this.organizationRestResource = organizationRestResource;
        this.ixorTalkConfigProperties = ixorTalkConfigProperties;
    }

    public Stream<Asset> getDevicesFromAssetMgmt(Long organizationId) {
        return getDevicesFromAssetMgmt(organizationRestResource.findById(organizationId).orElseThrow(ResourceNotFoundException::new));
    }

    public Stream<Asset> getDevicesFromAssetMgmt(Organization organization) {
        return assetMgmt.getAssets(organization.getOrganizationId()).stream();
    }

    public Optional<Asset> getOwnedDevice(DeviceInOrganizationDTO deviceInOrganizationDTO) {
        assertOrganizationOwnership(deviceInOrganizationDTO.getOrganizationId());

        return ofNullable(assetMgmt.getAssetByDeviceId(deviceInOrganizationDTO.getDeviceId())).filter(asset -> asset.belongsToOrganization(deviceInOrganizationDTO.getOrganizationId()));
    }

    public Optional<Asset> getAvailableDevice(DeviceInOrganizationDTO deviceInOrganizationDTO) {
        assertOrganizationOwnership(deviceInOrganizationDTO.getOrganizationId());

        return ofNullable(assetMgmt.getAssetByDeviceId(deviceInOrganizationDTO.getDeviceId())).filter(asset -> !asset.getOrganizationId().isPresent());
    }

    private void assertOrganizationOwnership(OrganizationId organizationId) {
        organizationRestResource.findById(organizationId.longValue()).orElseThrow(ResourceNotFoundException::new);
    }

    public Asset saveAssetProperties(Asset asset, Object properties) {
        assetMgmt.saveProperties(asset.getAssetId().stringValue(), properties);
        return asset;
    }

    public void removeFromOrganization(Asset asset) {

        Map<String, Object> fieldsToClear =
                ixorTalkConfigProperties
                        .getOrganization()
                        .getAssetmgmt()
                        .getAllowedSaveCalls()
                        .values()
                        .stream()
                        .flatMap(Collection::stream)
                        .collect(HashMap::new, (map, property) -> map.put(property, null), HashMap::putAll);

        saveAssetProperties(asset, removeAssetFromOrganizationDTO(fieldsToClear));
    }
}
