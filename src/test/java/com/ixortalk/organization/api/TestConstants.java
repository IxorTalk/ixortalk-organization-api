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
package com.ixortalk.organization.api;

public enum TestConstants {

    AUTH_0_DOMAIN("integration-test.auth0.com"),

    MAPPED_DEVICE_INFO_FIELD("deviceId"),
    UNMAPPED_DEVICE_INFO_FIELD("deviceName"),

    INVITE_MAIL_SUBJECT_KEY("theInviteMailSubjectKey"),
    INVITE_MAIL_TEMPLATE("theInviteMailTemplate"),

    VERIFY_MAIL_SUBJECT_KEY("theVerifyMailSubjectKey"),
    VERIFY_MAIL_TEMPLATE("theVerifyMailTemplate"),
    VERIFY_EMAIL_LANDING_PAGE_PATH("/onboarding/complete-registration"),

    USER_ACCEPTED_CALLBACK_PATH("/user-accepted"),
    USER_REMOVED_CALLBACK_PATH("/user-removed"),
    DEVICE_REMOVED_CALLBACK_PATH("/device-removed"),
    ORGANIZATION_REMOVED_CALLBACK_PATH("/organization-removed"),
    ORGANIZATION_PRE_DELETE_CHECK_CALLBACK_PATH("/organization-pre-delete-check"),

    CUSTOM_CLAIMS_NAMESPACE("https://test-namespace.ixortalk.com/"),

    DEFAULT_MAIL_LANGUAGE_TAG("nl"),

    LOADBALANCER_EXTERNAL_URL("https://integration-test.ixortalk.com"),
    IMAGE_SERVICE_CONTEXT_PATH("/image"),
    IMAGE_SERVICE_DOWNLOAD_PATH("/download"),
    ;

    private String configValue;

    TestConstants(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return this.configValue;
    }

    public enum JsonTestConstants {

        ASSET_WITHOUT_ORGANIZATION_ID_DEVICE_ID("c1"),
        ASSET_WITHOUT_ORGANIZATION_ID_DEVICE_NAME("assetWithoutOrganizationIdDeviceName"),
        ASSET_WITHOUT_ORGANIZATION_ID_IMAGE("https://my-organization/image.png"),
        ASSET_WITHOUT_ORGANIZATION_FIRST_ACTION_S_NAME("testing"),


        ASSET_WITH_ORGANIZATION_ID_ORGANIZATION_ID("1234"),
        ASSET_WITH_ORGANIZATION_ID_DEVICE_ID("c2"),
        ASSET_WITH_ORGANIZATION_ID_DEVICE_NAME("assetWithOrganizationIdDeviceName");

        private String configValue;

        JsonTestConstants(String configValue) {
            this.configValue = configValue;
        }

        public String configValue() {
            return this.configValue;
        }

        public Long configValueAsLong() {
            return Long.valueOf(this.configValue);
        }
    }
}
