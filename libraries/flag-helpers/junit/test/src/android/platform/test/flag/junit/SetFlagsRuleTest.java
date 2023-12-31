/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.platform.test.flag.junit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.platform.test.flag.util.FlagSetException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@code ResetFlagsRule}. */
@RunWith(JUnit4.class)
public final class SetFlagsRuleTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Test
    public void setFlagValues() {
        mSetFlagsRule.enableFlags("android.platform.test.flag.junit.flagName3");
        mSetFlagsRule.disableFlags("android.platform.test.flag.junit.flagName4");
        assertTrue(Flags.flagName3());
        assertFalse(Flags.flagName4());
    }

    @Test
    public void flagsShouldResetAfterOneTest() {
        assertFalse(Flags.flagName3());
        assertTrue(Flags.flagName4());
    }

    @Test
    public void setFlagsAfterOneTest() {
        mSetFlagsRule.enableFlags(
                "android.platform.test.flag.junit.flagName3",
                "android.platform.test.flag.junit.flagName4");
        assertTrue(Flags.flagName3());
        assertTrue(Flags.flagName4());

        mSetFlagsRule.disableFlags(
                "android.platform.test.flag.junit.flagName3",
                "android.platform.test.flag.junit.flagName4");
        assertFalse(Flags.flagName3());
        assertFalse(Flags.flagName4());
    }

    @Test
    public void invalidFlagName_throwException() {
        assertThrows(
                FlagSetException.class,
                () -> {
                    mSetFlagsRule.enableFlags("flagName3");
                });
    }
}
