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

import com.ixortalk.organization.api.asset.ActionsDTO;
import com.ixortalk.organization.api.asset.Asset;
import com.ixortalk.organization.api.asset.DeviceId;
import com.ixortalk.organization.api.asset.DeviceInformationDTO;
import com.ixortalk.organization.api.callback.api.OrganizationCallbackAPI;
import com.ixortalk.organization.api.domain.EnhancedUserProjection;
import com.ixortalk.organization.api.domain.Organization;
import com.ixortalk.organization.api.domain.Status;
import com.ixortalk.organization.api.domain.User;
import com.ixortalk.organization.api.events.OrganizationEventHandler;
import com.ixortalk.organization.api.events.RoleEventHandler;
import com.ixortalk.organization.api.rest.dto.DeviceInOrganizationDTO;
import com.ixortalk.organization.api.service.AssetMgmtFacade;
import com.ixortalk.organization.api.service.EnhanceUserService;
import com.ixortalk.organization.api.service.ImageMethodsService;
import com.ixortalk.organization.api.service.UserEmailProvider;
import com.ixortalk.organization.api.config.IxorTalkConfigProperties;
import com.ixortalk.organization.api.image.ImageService;
import com.ixortalk.organization.api.mail.InviteUserService;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.ixortalk.organization.api.asset.Properties.MappedField.IMAGE;
import static java.util.Collections.singletonMap;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.springframework.hateoas.CollectionModel.wrap;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.*;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

@Transactional
@RestController
@RequestMapping("/organizations")
public class OrganizationRestController {

    static final String DEVICE_STATE_FIELD_NAME = "deviceState";

    @Inject
    private OrganizationRestResource organizationRestResource;

    @Inject
    private AssetMgmtFacade assetMgmtFacade;

    @Inject
    private IxorTalkConfigProperties ixorTalkConfigProperties;

    @Inject
    private InviteUserService inviteUserService;

    @Inject
    private EnhanceUserService enhanceUserService;

    @Inject
    private ProjectionFactory projectionFactory;

    @Inject
    private ImageService imageService;

    @Inject
    private ImageMethodsService imageMethodsService;

    @Inject
    private OrganizationCallbackAPI organizationCallbackAPI;

    @Inject
    private UserEmailProvider userEmailProvider;

    @Inject
    private OrganizationEventHandler organizationEventHandler;

    @Inject
    private RoleEventHandler roleEventHandler;

    @DeleteMapping(path = "/{organizationId}/cascade")
    public ResponseEntity<?> deleteCascade(@PathVariable("organizationId") Long organizationId) {
        Organization organization = organizationRestResource.findById(organizationId).orElseThrow(ResourceNotFoundException::new);
        organizationRestResource.delete(organization);
        organizationCallbackAPI.organizationRemoved(organizationId);
        organizationEventHandler.handleAfterDelete(organization);
        organization.getRoles().forEach(role -> roleEventHandler.handleAfterDelete(role));
        assetMgmtFacade.getDevicesFromAssetMgmt(organization).forEach(asset -> assetMgmtFacade.removeFromOrganization(asset));
        return noContent().build();
    }

    @GetMapping(path = "/{organizationId}/adminUsers", produces = APPLICATION_JSON_VALUE)
    public CollectionModel<EntityModel<EnhancedUserProjection>> getAdminUsers(@PathVariable("organizationId") Long organizationId) {
        return organizationRestResource.findById(organizationId)
                .map(organization ->
                        wrap(
                                organization
                                        .getUsers()
                                        .stream()
                                        .filter(user -> enhanceUserService.isAdmin(user))
                                        .map(user -> projectionFactory.createProjection(EnhancedUserProjection.class, user))
                                        .sorted(comparing(EnhancedUserProjection::getLogin))
                                        .collect(toList())))
                .orElseThrow(ResourceNotFoundException::new);
    }

    @GetMapping(path = "/{organizationId}/devices")
    public List<Map<String, Object>> getDevices(@PathVariable("organizationId") Long organizationId) {
        return assetMgmtFacade.getDevicesFromAssetMgmt(organizationId)
                .map(this::constructAssetInformation)
                .collect(toList());
    }

    @GetMapping(path = "/{organizationId}/deviceIds")
    public List<String> getDeviceIds(@PathVariable("organizationId") Long organizationId) {
        return assetMgmtFacade.getDevicesFromAssetMgmt(organizationId)
                .map(Asset::getDeviceId)
                .map(DeviceId::stringValue)
                .collect(toList());
    }

    @PostMapping(path = "/{organizationId}/devices/{deviceId}/save-actions")
    public void saveActions(DeviceInOrganizationDTO deviceInOrganizationDTO, @RequestBody ActionsDTO actions) {
        assetMgmtFacade.getOwnedDevice(deviceInOrganizationDTO).ifPresent(asset -> assetMgmtFacade.saveAssetProperties(asset, actions));
    }

    @PostMapping(path = "/{organizationId}/devices/{deviceId}/save-info")
    public void saveDeviceInformation(DeviceInOrganizationDTO deviceInOrganizationDTO, @RequestBody DeviceInformationDTO deviceInformationDTO) {
        assetMgmtFacade.getOwnedDevice(deviceInOrganizationDTO).ifPresent(asset -> assetMgmtFacade.saveAssetProperties(asset, deviceInformationDTO));
    }

    @PostMapping(path = "/{organizationId}/devices/{deviceId}")
    public ResponseEntity<?> addDevice(DeviceInOrganizationDTO deviceInOrganizationDTO) {
        return assetMgmtFacade.getAvailableDevice(deviceInOrganizationDTO)
                .map(asset -> assetMgmtFacade.saveAssetProperties(asset, deviceInOrganizationDTO.getOrganizationId()))
                .map(asset -> ok().build())
                .orElse(badRequest().build());
    }


    @DeleteMapping(path = "/{organizationId}/devices/{deviceId}")
    public ResponseEntity<?> deleteDevice(DeviceInOrganizationDTO deviceInOrganizationDTO) {
        Asset asset = assetMgmtFacade.getOwnedDevice(deviceInOrganizationDTO).orElseThrow(ResourceNotFoundException::new);
        organizationCallbackAPI.deviceRemoved(deviceInOrganizationDTO);
        assetMgmtFacade.removeFromOrganization(asset);
        return noContent().build();
    }

    @PostMapping(path = "/{organizationId}/devices/{deviceId}/image")
    public ResponseEntity<?> uploadDeviceImage(DeviceInOrganizationDTO deviceInOrganizationDTO, @RequestPart("file") MultipartFile multipartFile) {
        return assetMgmtFacade.getOwnedDevice(deviceInOrganizationDTO)
                .flatMap(asset ->
                        ofNullable(imageService.uploadImage(multipartFile, "organizations/" + deviceInOrganizationDTO.getOrganizationId().longValue() + "/" + deviceInOrganizationDTO.getDeviceId().stringValue() + "/image"))
                                .map(response -> {
                                            String imageUrl = response.headers().get(LOCATION).iterator().next();
                                            assetMgmtFacade.saveAssetProperties(asset, singletonMap(IMAGE.getPropertyName(), imageUrl));
                                            return created(fromUriString(imageUrl).build().toUri()).build();
                                        }
                                ))
                .orElse(notFound().build());
    }

    @PostMapping(path = "/{organizationId}/users/used")
    public void usersUsed(
            @PathVariable("organizationId") Long organizationId,
            @RequestBody List<String> logins) {

        Organization organization = organizationRestResource.findById(organizationId).orElseThrow(ResourceNotFoundException::new);
        organization
                .getUsers()
                .stream()
                .filter(User::isCreated)
                .filter(user -> logins.contains(user.getLogin()))
                .forEach(user -> inviteUserService.inviteUser(user, organization));
    }

    @GetMapping(path = "/search/findAcceptedOrganizationIds")
    public List<Long> findAcceptedOrganizationIds() {
        return userEmailProvider.getCurrentUsersEmail()
                .map(this::findAcceptedOrganizationIds)
                .orElse(newArrayList());
    }

    @GetMapping(path = "/search/findAcceptedOrganizationIds", params = {"userId"})
    @PreAuthorize("hasRole('ROLE_ADMIN') or @securityService.isCurrentUser(#userId)")
    public List<Long> findAcceptedOrganizationIds(@RequestParam(name = "userId") String userId) {
        return organizationRestResource.findByUsersLoginAndUsersStatus(userId.toLowerCase(), Status.ACCEPTED).stream().map(Organization::getId).collect(toList());
    }

    private Map<String, Object> constructAssetInformation(Asset asset) {
        Map<String, Object> assetInfo = newHashMap();
        assetInfo.put(
                DEVICE_STATE_FIELD_NAME,
                ixorTalkConfigProperties.getLoadbalancer().getExternal().getUrlWithoutStandardPorts() + ixorTalkConfigProperties.getMicroservice("assetstate").getContextPath() + "/states/" + asset.getDeviceId());
        ixorTalkConfigProperties
                .getOrganization()
                .getApi()
                .getDeviceInfoFields()
                .forEach(fieldName ->
                        assetInfo.put(fieldName, (fieldName.equals(IMAGE.getPropertyName())) ?
                                imageMethodsService.constructImageLink((String) asset.getAssetProperty(fieldName)) :
                                asset.getAssetProperty(fieldName)));
        return assetInfo;
    }
}
