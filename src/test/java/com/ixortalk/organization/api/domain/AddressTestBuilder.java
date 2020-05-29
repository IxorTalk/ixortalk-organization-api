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
package com.ixortalk.organization.api.domain;

import com.ixortalk.test.builder.ReflectionInstanceTestBuilder;

import static org.springframework.test.util.ReflectionTestUtils.setField;

public class AddressTestBuilder extends ReflectionInstanceTestBuilder<Address> {

    private String streetAndNumber = "Schuttersvest 75";
    private String postalCode = "2800";
    private String city = "Mechelen";
    private String country = "Belgium";

    private AddressTestBuilder() {}

    @Override
    public void setFields(Address address) {
        setField(address, "streetAndNumber", streetAndNumber);
        setField(address, "postalCode", postalCode);
        setField(address, "city", city);
        setField(address, "country", country);
    }

    public static final AddressTestBuilder anAddress() {
        return new AddressTestBuilder();
    }

    public AddressTestBuilder withStreetAndNumber(String street) {
        this.streetAndNumber = street;
        return this;
    }

    public AddressTestBuilder withPostalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    public AddressTestBuilder withCity(String city) {
        this.city = city;
        return this;
    }

    public AddressTestBuilder withCountry(String country) {
        this.country = country;
        return this;
    }
}
