/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sts.common;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.util.CommandResult;
import com.android.tradefed.util.CommandStatus;

/** Various system-related helper functions */
public class SystemUtil {
    private SystemUtil() {}

    /**
     * Set the value of a property until return is closed
     *
     * @param name the name of the property to set
     * @param value the value that the property should be set to
     * @param device the device to use
     * @return If enabling malloc debug succeeded: an object that will disable malloc debug when
     *     closed. If enabling malloc debug did not succeed: null.
     */
    public static AutoCloseable withProperty(
            final String name, final String value, final ITestDevice device)
            throws DeviceNotAvailableException {
        final String oldValue = device.getProperty(name);
        assumeTrue(
                "Could not set property: '" + name + "' to '" + value + "'",
                setProperty(device, name, value));
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                assumeTrue(
                        "Could not reset property: '" + name + "' to '" + oldValue + "'",
                        setProperty(device, name, oldValue));
            }
        };
    }

    /** Reimplement setProperty because property values are not properly escaped in Q and R */
    private static boolean setProperty(ITestDevice device, String name, String value)
            throws DeviceNotAvailableException {
        assertNotNull("Property name cannot be null", name);
        String setValue = (value == null) ? "" : value;
        CommandResult res =
                device.executeShellV2Command(String.format("setprop '%s' '%s'", name, setValue));
        return res.getStatus() == CommandStatus.SUCCESS;
    }
}
