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

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.math.BigInteger;
import java.util.List;

/**
 *
 * @author Vlad Kolotov
 */
@XStreamAlias("Field")
public class Field {

    @XStreamAsAttribute
    private String name;
    @XStreamAlias("InformativeText")
    private String informativeText;
    @XStreamImplicit(itemFieldName = "Requirement")
    private List<String> requirements;
    @XStreamAlias("Reference")
    private String reference;
    @XStreamAlias("Format")
    private String format;
    @XStreamAlias("BitField")
    private BitField bitField;
    @XStreamAlias("DecimalExponent")
    private Integer decimalExponent;
    @XStreamAlias("BinaryExponent")
    private Integer binaryExponent;
    @XStreamAlias("Multiplier")
    private Integer multiplier;
    @XStreamAlias("Unit")
    private String unit;
    @XStreamAlias("Minimum")
    private Double minimum;
    @XStreamAlias("Maximum")
    private Double maximum;
    @XStreamAlias("Offset")
    private Double offset;
    @XStreamAlias("Enumerations")
    private Enumerations enumerations;

    // extensions
    @XStreamAsAttribute
    private boolean unknown;
    @XStreamAsAttribute
    private boolean system;
    // Repeated (array) field, from GSS types like uint16[n]. null/absent = single occurrence;
    // 0 = repeat to end of data; N > 0 = exactly N occurrences.
    @XStreamAsAttribute
    private Integer repeats;

    public String getName() {
        return name != null ? name.trim() : null;
    }

    public String getInformativeText() {
        return informativeText;
    }

    public List<String> getRequirements() {
        return requirements;
    }

    public FieldFormat getFormat() {
        return FieldFormat.valueOf(format);
    }

    public BitField getBitField() {
        return bitField;
    }

    public Integer getDecimalExponent() {
        return decimalExponent;
    }

    public Integer getBinaryExponent() {
        return binaryExponent;
    }

    public Integer getMultiplier() {
        return multiplier;
    }

    public String getUnit() {
        return unit;
    }

    public Double getMinimum() {
        return minimum;
    }

    public Double getMaximum() {
        return maximum;
    }

    public Double getOffset() {
        return offset;
    }

    public Enumerations getEnumerations() {
        return enumerations;
    }

    public String getReference() {
        return reference;
    }

    /**
     * Returns the repeat specification for an array field (GSS types like {@code uint16[n]}).
     * {@code null} means a single (non-repeated) field; {@code 0} means repeat until the end of
     * the data; a positive value means exactly that many occurrences.
     *
     * @return the repeat count, or null when the field is not repeated
     */
    public Integer getRepeats() {
        return repeats;
    }

    public boolean isRepeated() {
        return repeats != null;
    }

    public boolean isUnknown() {
        return unknown;
    }

    public boolean isSystem() {
        return system;
    }

    public boolean isFlagField() {
        return FlagUtils.isFlagsField(this);
    }

    public boolean isOpCodesField() {
        return FlagUtils.isOpCodesField(this);
    }

    public boolean hasEnumerations() {
        return enumerations != null && enumerations.getEnumerations() != null
                && !enumerations.getEnumerations().isEmpty();
    }

    public Enumeration getEnumeration(BigInteger key) {
        return FlagUtils.getEnumeration(this, key).orElse(null);
    }

    public List<Enumeration> getEnumerations(String value) {
        return FlagUtils.getEnumerations(this, value);
    }

}
