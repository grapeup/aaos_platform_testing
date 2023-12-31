/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.host.test.longevity;

import android.host.test.composer.Iterate;
import android.host.test.composer.Shuffle;
import android.host.test.longevity.listener.ErrorTerminator;
import android.host.test.longevity.listener.TimeoutTerminator;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Using the {@code LongevitySuite} as a runner allows you to run test sequences repeatedly and with
 * shuffling in order to simulate longevity conditions and repeated stress or exercise. For examples
 * look at the bundled samples package.
 */
public class LongevitySuite extends Suite {
    static final String QUITTER_OPTION = "quitter";
    private static final boolean QUITTER_DEFAULT = false; // don't quit

    // If this option is true, a test interruption is reported as a test run failure, as opposed to
    // a successful test run end with a truncated set of tests that were actually run.
    static final String INVALIDATE_OPTION = "invalidate-if-early";
    private static final boolean INVALIDATE_DEFAULT = false;

    protected Map<String, String> mArguments;

    /**
     * Called reflectively on classes annotated with {@code @RunWith(LongevitySuite.class)}
     */
    public LongevitySuite(Class<?> klass, RunnerBuilder builder)
            throws InitializationError {
        this(
                klass,
                builder,
                System.getProperties()
                        .entrySet()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        i -> String.valueOf(i.getKey()),
                                        i -> String.valueOf(i.getValue()))));
    }

    /**
     * Called explicitly to pass in configurable arguments without affecting expected formats.
     */
    public LongevitySuite(Class<?> klass, RunnerBuilder builder, Map<String, String> args)
            throws InitializationError {
        this(klass, constructClassRunners(klass, builder, args), args);
    }

    /**
     * Called by this class once the suite class and runners have been determined.
     */
    protected LongevitySuite(Class<?> klass, List<Runner> runners, Map<String, String> args)
            throws InitializationError {
        super(klass, runners);
        mArguments = args;
    }

    /**
     * Constructs the sequence of {@link Runner}s that produce the full longevity test.
     */
    private static List<Runner> constructClassRunners(
                Class<?> suite, RunnerBuilder builder, Map<String, String> args)
            throws InitializationError {
        // Note: until b/118340229 is resolved, keep the platform class method in sync as necessary.
        // Retrieve annotated suite classes.
        SuiteClasses annotation = suite.getAnnotation(SuiteClasses.class);
        if (annotation == null) {
            throw new InitializationError(String.format(
                    "Longevity suite, '%s', must have a SuiteClasses annotation", suite.getName()));
        }
        // Construct and store custom runners for the full suite.
        BiFunction<Map<String, String>, List<Runner>, List<Runner>> modifier =
                new Iterate<Runner>().andThen(new Shuffle<Runner>());
        return modifier.apply(args, builder.runners(suite, annotation.value()));
    }

    @Override
    public void run(final RunNotifier notifier) {
        // Add action terminators for custom runner logic.
        if (mArguments.containsKey(QUITTER_OPTION)
                ? Boolean.parseBoolean(mArguments.get(QUITTER_OPTION))
                : QUITTER_DEFAULT) {
            notifier.addListener(getErrorTerminator(notifier));
        }
        notifier.addListener(getTimeoutTerminator(notifier));
        // Invoke tests to run through super call.
        try {
            super.run(notifier);
        } catch (StoppedByUserException e) {
            // Invalidate the test run if terminated early and the option is set.
            if (mArguments.containsKey(INVALIDATE_OPTION)
                    ? Boolean.parseBoolean(mArguments.get(INVALIDATE_OPTION))
                    : INVALIDATE_DEFAULT) {
                throw e;
            } else {
                return;
            }
        }
    }

    /**
     * Returns the {@link ErrorTerminator} to register with the {@link RunNotifier}.
     * <p>
     * Note: exposed for overriding with a platform-specific {@link ErrorTerminator}.
     */
    public ErrorTerminator getErrorTerminator(final RunNotifier notifier) {
        return new ErrorTerminator(notifier);
    }

    /**
     * Returns the {@link TimeoutTerminator} to register with the {@link RunNotifier}.
     * <p>
     * Note: exposed for overriding with a platform-specific {@link TimeoutTerminator}.
     */
    public TimeoutTerminator getTimeoutTerminator(final RunNotifier notifier) {
        return new TimeoutTerminator(notifier, mArguments);
    }

    /**
     * Returns the {@link List} of {@link Runner}s children for explicit modification by another
     * class.
     * <p>
     * Note: using this method is highly discouraged unless explicitly needed.
     */
    public List<Runner> getRunners() {
        return getChildren();
    }
}
