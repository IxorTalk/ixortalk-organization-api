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
package com.ixortalk.organization.api.config;

import static com.ixortalk.test.util.Randomizer.nextString;

public class TestConstants {

    public static final String ADMIN_JWT_TOKEN = nextString("adminJwtToken");
    public static final String USER_JWT_TOKEN = nextString("userJwtToken");
    public static final String OTHER_USER_JWT_TOKEN = nextString("otherUserJwtToken");
    public static final String USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN = nextString("userInOrganizationXAdminJwtToken");
    public static final String USER_IN_ORGANIZATION_Y_ADMIN_JWT_TOKEN = nextString("userInOrganizationYAdminJwtToken");
    public static final String USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN = nextString("userInOrganizationXInvitedJwtToken");
    public static final String USER_IN_ORGANIZATION_X_ACCEPTED_JWT_TOKEN = nextString("userInOrganizationXAcceptedJwtToken");
    public static final String USER_WITHOUT_ROLES_JWT_TOKEN = nextString("userWithoutRolesJwtToken");

    public static final String USER_IN_ORGANIZATION_X_ADMIN_ID = "userInOrganizationXAdminId";
    public static final String USER_IN_ORGANIZATION_X_ADMIN_EMAIL = "admin@organization-x.com";

    public static final String USER_IN_ORGANIZATION_Y_ADMIN_ID = "userInOrganizationYAdminId";
    public static final String USER_IN_ORGANIZATION_Y_ADMIN_EMAIL = "admin@organization-y.com";

    public static final String USER_IN_ORGANIZATION_X_INVITED_ID = "user-in-organization-x-invited-id";
    public static final String USER_IN_ORGANIZATION_X_INVITED_EMAIL = "user-invited@organization-x.com";

    public static final String USER_IN_ORGANIZATION_X_ACCEPTED_ID = "user-in-organization-x-accepted-id";
    public static final String USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL = "user-accepted@organization-x.com";

    public static final String USER_WITHOUT_ROLES_ID = "userWithoutRoles";
    public static final String USER_WITHOUT_ROLES_EMAIL = "no-roles@ixortalk.com";

    public static final String USER_EMAIL = "user@ixortalk.com";
    public static final String OTHER_USER_EMAIL = "other-user@ixortalk.com";

    public enum Role {
        ORGANIZATION_X_ADMIN,
        ORGANIZATION_Y_ADMIN;

        public String roleName() {
            return "ROLE_" + name();
        }
    }
}
