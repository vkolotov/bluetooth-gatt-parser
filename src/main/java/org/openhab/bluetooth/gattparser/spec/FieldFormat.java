package org.openhab.bluetooth.gattparser.spec;

/*-
 * Copyright (C) 2017 Sputnik Dev
 * %%
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
 * #L%
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Vlad Kolotov
 */
public class FieldFormat {

    public static final int FULL_SIZE = -1;

    private static final Map<String, FieldFormat> PREDEFINED = Collections.unmodifiableMap(
            new HashMap<String, FieldFormat>() {{
                put("boolean", new FieldFormat("boolean", FieldType.BOOLEAN, 1));
                put("nibble", new FieldFormat("nibble", FieldType.UINT, 4));
                put("float32", new FieldFormat("float32", FieldType.FLOAT_IEE754, 32));
                put("float64", new FieldFormat("float64", FieldType.FLOAT_IEE754, 64));
                put("sfloat", new FieldFormat("SFLOAT", FieldType.FLOAT_IEE11073, 16));
                put("float", new FieldFormat("FLOAT", FieldType.FLOAT_IEE11073, 32));
                // GSS (Bluetooth Specification Supplement) names for the IEEE-11073 floats.
                put("medfloat16", new FieldFormat("medfloat16", FieldType.FLOAT_IEE11073, 16));
                put("medfloat32", new FieldFormat("medfloat32", FieldType.FLOAT_IEE11073, 32));
                //put("duint16", new FieldFormat("duint16", FieldType.UINT, 16));
                put("utf8s", new FieldFormat("utf8s", FieldType.UTF8S, FULL_SIZE));
                put("utf16s", new FieldFormat("utf16s", FieldType.UTF16S, FULL_SIZE));
                put("struct", new FieldFormat("struct", FieldType.STRUCT, FULL_SIZE));
                put("reg-cert-data-list", new FieldFormat("struct", FieldType.STRUCT, FULL_SIZE));
    }});

    private final String name;
    private final FieldType type;
    private final int size;

    FieldFormat(String name, FieldType type, int size) {
        this.name = name;
        this.type = type;
        this.size = size;
    }

    public static FieldFormat valueOf(String name) {
        if (name == null) {
            return null;
        }
        String fieldName = name.toLowerCase();
        if (PREDEFINED.containsKey(fieldName)) {
            return PREDEFINED.get(fieldName);
        } else if (fieldName.startsWith("uint") || fieldName.endsWith("bit")) {
            return new FieldFormat(fieldName, FieldType.UINT, parseSize(fieldName));
        } else if (fieldName.startsWith("sint")) {
            return new FieldFormat(fieldName, FieldType.SINT, parseSize(fieldName));
        } else {
            return null;
        }
    }

    public String getName() {
        return name;
    }

    public FieldType getType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    public boolean isReal() {
        return type == FieldType.UINT || type == FieldType.SINT;
    }

    public boolean isDecimal() {
        return type == FieldType.FLOAT_IEE754 || type == FieldType.FLOAT_IEE11073;
    }

    public boolean isBoolean() {
        return type == FieldType.BOOLEAN;
    }

    public boolean isString() {
        return type == FieldType.UTF8S || type == FieldType.UTF16S;
    }

    public boolean isStruct() {
        return type == FieldType.STRUCT;
    }

    public boolean isNumber() {
        return isReal() || isDecimal();
    }

    private static int parseSize(String name) {
        try {
            return Integer.parseInt(name.replace("uint", "").replace("sint", "").replace("bit", ""));
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
