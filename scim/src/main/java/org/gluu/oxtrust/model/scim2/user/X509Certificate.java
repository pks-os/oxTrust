package org.gluu.oxtrust.model.scim2.user;

import org.gluu.oxtrust.model.scim2.AttributeDefinition;
import org.gluu.oxtrust.model.scim2.annotations.Attribute;
import org.gluu.oxtrust.model.scim2.Validations;
import org.gluu.oxtrust.model.scim2.annotations.Validator;

/**
 * Created by jgomer on 2017-09-12.
 *
 * This class represents a x509Certificate attribute.
 */
public class X509Certificate {

    @Attribute(description = "The value of a X509 certificate.",
            isRequired = true,  //specs says false but it doesn't make sense
            type = AttributeDefinition.Type.BINARY)
    @Validator(value = Validations.X509)
    private String value;

    @Attribute(description = "A human readable name, primarily used for display purposes.")
    private String display;

    @Attribute(description = "A label indicating the attribute's function.")
    private String type;

    @Attribute(description = "A Boolean value indicating the 'primary' or preferred attribute value for this attribute. " +
            "The primary attribute value 'true' MUST appear no more than once.",
            type = AttributeDefinition.Type.BOOLEAN)
    private Boolean primary;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getPrimary() {
        return primary;
    }

    public void setPrimary(Boolean primary) {
        this.primary = primary;
    }
}
