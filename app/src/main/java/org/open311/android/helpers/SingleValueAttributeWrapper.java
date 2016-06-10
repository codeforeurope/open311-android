package org.open311.android.helpers;

import org.codeforamerica.open311.facade.data.SingleValueAttribute;

public class SingleValueAttributeWrapper extends SingleValueAttribute {

    private String code;

    public SingleValueAttributeWrapper(String code, String value) {
        super(code, value);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean hasCode(String code) {
        return this.code.equals(code);
    }
}
