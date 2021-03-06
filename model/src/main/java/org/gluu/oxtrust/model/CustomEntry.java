/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.gluu.persist.model.base.Entry;
import org.gluu.site.ldap.persistence.annotation.LdapCustomObjectClass;
import org.xdi.util.StringHelper;

/**
 * Entry with custom attributes and custom object classes lists
 * 
 * @author Yuriy Movchan Date: 07.05.2011
 */
public abstract class CustomEntry extends Entry implements Serializable, Cloneable {

	private static final long serialVersionUID = 5079582184398161111L;

	@LdapCustomObjectClass
	private String[] customObjectClasses;

	public abstract List<GluuCustomAttribute> getCustomAttributes();

	public abstract void setCustomAttributes(List<GluuCustomAttribute> customAttributes);

	public String[] getCustomObjectClasses() {
		return customObjectClasses;
	}

	public void setCustomObjectClasses(String[] customObjectClasses) {
		this.customObjectClasses = customObjectClasses;
	}

	public String[] getAttributes(String attributeName) {
		if (StringHelper.isEmpty(attributeName)) {
			return null;
		}

		String[] values = null;
		for (GluuCustomAttribute attribute : getCustomAttributes()) {
			if (StringHelper.equalsIgnoreCase(attribute.getName(), attributeName)) {
				values = attribute.getValues();
				break;
			}
		}
		return values;
	}

	public String getAttribute(String attributeName) {
		if (StringHelper.isEmpty(attributeName)) {
			return null;
		}

		String value = null;
		for (GluuCustomAttribute attribute : getCustomAttributes()) {
			if (StringHelper.equalsIgnoreCase(attribute.getName(), attributeName)) {
				value = attribute.getValue();
				break;
			}
		}
		return value;
	}

	public String getAttribute(String attributeName, String defaultValue) {
		String result = getAttribute(attributeName);
		if (StringHelper.isEmpty(result)) {
			result = defaultValue;
		}

		return result;
	}

	public void setAttribute(String attributeName, String attributeValue) {
		setAttribute(new GluuCustomAttribute(attributeName, attributeValue));
	}

	public void setAttribute(String attributeName, String[] attributeValue) {
		setAttribute(new GluuCustomAttribute(attributeName, attributeValue));
	}

	public void setAttribute(GluuCustomAttribute attribute) {
		List<GluuCustomAttribute> customAttributes = getCustomAttributes();
		customAttributes.remove(attribute);
		customAttributes.add(attribute);
	}

	@Override
	public String toString() {
		return String.format("CustomEntry [customAttributes=%s, customObjectClasses=%s, toString()=%s]", getCustomAttributes(),
				Arrays.toString(customObjectClasses), super.toString());
	}

}
