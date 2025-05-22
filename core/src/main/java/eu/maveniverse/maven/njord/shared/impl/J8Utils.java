/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.shared.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class J8Utils {
    private J8Utils() {}

    public static Supplier<IllegalStateException> OET = () -> new IllegalStateException("No value present");

    public static Map<String, String> zipFsCreate(boolean create) {
        HashMap<String, String> map = new HashMap<>();
        map.put("create", Boolean.toString(create));
        return map;
    }

    private static final int DEFAULT_BUFFER_SIZE = 16384;

    public static long transferTo(InputStream in, OutputStream out) throws IOException {
        Objects.requireNonNull(out, "out");
        long transferred = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
            out.write(buffer, 0, read);
            if (transferred < Long.MAX_VALUE) {
                try {
                    transferred = Math.addExact(transferred, read);
                } catch (ArithmeticException ignore) {
                    transferred = Long.MAX_VALUE;
                }
            }
        }
        return transferred;
    }

    public static <K, V> Map<K, V> copyOf(Map<K, V> map) {
        return Collections.unmodifiableMap(new HashMap<>(map));
    }

    public static <E1, E2 extends E1> List<E1> copyOf(Collection<E2> list) {
        return Collections.unmodifiableList(new ArrayList<>(list));
    }
}
