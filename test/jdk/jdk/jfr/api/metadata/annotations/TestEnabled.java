/*
 * Copyright (c) 2015, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.jfr.api.metadata.annotations;

import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Recording;
import jdk.test.lib.Asserts;

/**
 * @test
 * @requires vm.flagless
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.annotations.TestEnabled
 */
public class TestEnabled {

    @Enabled(true)
    static class EnabledTrueEvent extends Event {
    }

    @Enabled(false)
    static class EnabledFalseEvent extends Event {
    }

    public static void main(String[] args) throws Exception {
        EventType trueEvent = EventType.getEventType(EnabledTrueEvent.class);
        EventType falseEvent = EventType.getEventType(EnabledFalseEvent.class);

        Recording r = new Recording();

        Asserts.assertFalse(trueEvent.isEnabled(), "@Enabled(true) event should be diabled before recording start");
        Asserts.assertFalse(falseEvent.isEnabled(), "@Enabled(false) event should be diabled before recording start");

        r.start();

        Asserts.assertTrue(trueEvent.isEnabled(), "@Enabled(true) event should to be enabled during recording");
        Asserts.assertFalse(falseEvent.isEnabled(), "@Enabled(true) event should to be disabled during recording");

        r.stop();

        Asserts.assertFalse(trueEvent.isEnabled(), "@Enabled(true) event should be diabled after recording stop");
        Asserts.assertFalse(falseEvent.isEnabled(), "@Enabled(false) event should to be diabled after recording stop");

        r.close();
    }
}
