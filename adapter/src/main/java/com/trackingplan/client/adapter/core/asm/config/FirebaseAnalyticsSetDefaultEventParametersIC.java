// MIT License
//
// Copyright (c) 2021 Trackingplan
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package com.trackingplan.client.adapter.core.asm.config;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.transformations.FirebaseAnalyticsLogEventTransformation;
import com.trackingplan.client.adapter.core.asm.transformations.FirebaseAnalyticsSetDefaultEventParametersTransformation;

public class FirebaseAnalyticsSetDefaultEventParametersIC extends MethodVisitorTransformationConfig {

    private static final String CLASS_NAME;
    private static final String METHOD_NAME;
    private static final String METHOD_DESC;

    static {
        try {
            // TODO: Add FirebaseAnalytics dependency to get data
            CLASS_NAME = "com/google/firebase/analytics/FirebaseAnalytics";
            METHOD_NAME = "setDefaultEventParameters";
            METHOD_DESC = "(Landroid/os/Bundle;)V";
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public FirebaseAnalyticsSetDefaultEventParametersIC() {
        super(new FirebaseAnalyticsSetDefaultEventParametersTransformation.Factory(), CLASS_NAME, METHOD_NAME, METHOD_DESC);
    }
}
