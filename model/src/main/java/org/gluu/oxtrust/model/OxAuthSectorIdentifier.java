package org.gluu.oxtrust.model;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.logging.log4j.core.util.UuidUtil;
import org.gluu.persist.model.base.Entry;
import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * Sector Identifier
 *
 * @author Javier Rojas Blum
 * @version January 15, 2016
 */
@LdapEntry(sortBy = { "description" })
@LdapObjectClass(values = { "top", "oxSectorIdentifier" })
public class OxAuthSectorIdentifier extends Entry implements Serializable {

	private static final long serialVersionUID = -2812480357430436514L;

	private transient boolean selected;

	@LdapAttribute(name = "oxId", ignoreDuringUpdate = true)
	private String id;
	@NotNull
    @Size(min = 0, max = 250, message = "Length of the Description should not exceed 250")
	@LdapAttribute(name = "description")
	private String description;

	@LdapAttribute(name = "oxAuthRedirectURI")
	private List<String> redirectUris;

	@LdapAttribute(name = "oxAuthClientId")
	private List<String> clientIds;

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<String> getRedirectUris() {
		return redirectUris;
	}

	public void setRedirectUris(List<String> redirectUris) {
		this.redirectUris = redirectUris;
	}

	public List<String> getClientIds() {
		return clientIds;
	}

	public void setClientIds(List<String> clientIds) {
		this.clientIds = clientIds;
	}

	public String getLoginUri() {
		return null;
	}

	public String getDescription() {
		if (description == null) {
			description = "Default description " + UuidUtil.getTimeBasedUuid().toString().substring(0, 5);
		}
		return description;
	}

	public void setDescription(String des) {
		this.description = des;
	}

	@Override
	public String toString() {
		return String.format("OxAuthSectorIdentifier [id=%s, toString()=%s]", id, super.toString());
	}
}
