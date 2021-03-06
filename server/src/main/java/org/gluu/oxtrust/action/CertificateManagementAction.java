/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */
package org.gluu.oxtrust.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Size;

import org.apache.commons.io.IOUtils;
import org.gluu.jsf2.message.FacesMessages;
import org.gluu.oxtrust.ldap.service.ApplianceService;
import org.gluu.oxtrust.ldap.service.SSLService;
import org.gluu.oxtrust.ldap.service.SvnSyncTimer;
import org.gluu.oxtrust.model.GluuAppliance;
import org.gluu.oxtrust.model.cert.TrustStoreCertificate;
import org.gluu.oxtrust.service.asimba.AsimbaXMLConfigurationService;
import org.gluu.oxtrust.util.KeystoreWrapper;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.gluu.oxtrust.util.X509CertificateShortInfo;
import org.slf4j.Logger;
import org.xdi.service.security.Secure;

/**
 * Action class for security certificate management.
 * 
 * @author Dmitry Ognyannikov
 */
@Named
@SessionScoped
@Secure("#{permissionService.hasPermission('configuration', 'access')}")
public class CertificateManagementAction implements Serializable {

	private static final long serialVersionUID = -1938167091985945238L;
	private static final String OPENLDAP_CERTIFICATE_FILE = "/etc/certs/openldap.crt";
	private static final String OPENDJ_CERTIFICATE_FILE = "/etc/certs/opendj.crt";
	private static final String HTTPD_CERTIFICATE_FILE = "/etc/certs/httpd.crt";
	private static final String IDP_SIGNING_CERTIFICATE_FILE = "/etc/certs/idp-signing.crt";
	private static final String IDP_ENCRYPT_CERTIFICATE_FILE = "/etc/certs/idp-encryption.crt";

	@Inject
	private Logger log;

	@Inject
	private FacesMessages facesMessages;

	@Inject
	private SvnSyncTimer svnSyncTimer;

	@Inject
	private AsimbaXMLConfigurationService asimbaXMLConfigurationService;

	@Inject
	private ApplianceService applianceService;

	private KeystoreWrapper asimbaKeystore;

	private List<X509CertificateShortInfo> asimbaCertificates = new ArrayList<X509CertificateShortInfo>();

	private List<X509CertificateShortInfo> trustStoreCertificates;

	private List<X509CertificateShortInfo> internalCertificates;

	@Size(min = 0, max = 30, message = "Length of search string should be less than 30")
	private String searchPattern = "";

	private boolean searchObsoleteWarning = false;

	@PostConstruct
	public void init() {
		log.info("init() CertificateManagement call");
		refresh();
	}

	public void refresh() {
		log.info("refresh() CertificateManagement call");
		try {
			if (asimbaXMLConfigurationService.isReady()) {
				asimbaKeystore = asimbaXMLConfigurationService.getKeystore();
				List<X509CertificateShortInfo> listCertificates = asimbaKeystore.listCertificates();
				if (listCertificates != null) {
					asimbaCertificates.addAll(listCertificates);
				}
			}
		} catch (Exception e) {
			log.error("Load Asimba keystore configuration exception", e);
		}

		updateTableView();
	}

	public String add() {
		log.info("add");
		// save
		synchronized (svnSyncTimer) {
			// TODO
		}
		refresh();
		return OxTrustConstants.RESULT_SUCCESS;
	}

	public String cancel() {
		log.info("cancel CertificateManagement");

		facesMessages.add(FacesMessage.SEVERITY_INFO, "Certificates not updated");
		return OxTrustConstants.RESULT_SUCCESS;
	}

	public String search() {
		log.info("search() CertificateManagement searchPattern:", searchPattern);

		// TODO

		return OxTrustConstants.RESULT_SUCCESS;
	}

	/**
	 * Load and process certificate lists.
	 * 
	 * Set highlight for obsolete certificates. Apply search pattern.
	 */
	private void updateTableView() {
		try {
			asimbaCertificates.stream().forEach(cert -> cert.updateViewStyle());
		} catch (Exception e) {
			log.error("Load Asimba keystore configuration exception", e);
		}

		try {
			// load trustStoreCertificates
			trustStoreCertificates = new ArrayList<X509CertificateShortInfo>();

			GluuAppliance appliance = applianceService.getAppliance();

			List<TrustStoreCertificate> trustStoreCertificatesList = appliance.getTrustStoreCertificates();

			if (trustStoreCertificatesList != null) {
				for (TrustStoreCertificate trustStoreCertificate : trustStoreCertificatesList) {
					try {
						X509Certificate certs[] = SSLService
								.loadCertificates(trustStoreCertificate.getCertificate().getBytes());

						for (X509Certificate cert : certs) {
							X509CertificateShortInfo entry = new X509CertificateShortInfo(
									trustStoreCertificate.getName(), cert);
							trustStoreCertificates.add(entry);
						}
					} catch (Exception e) {
						log.error("Certificate load exception", e);
					}
				}
			}
		} catch (Exception e) {
			log.error("Load trustStoreCertificates configuration exception", e);
		}

		try {
			// load internalCertificates
			internalCertificates = new ArrayList<X509CertificateShortInfo>();
			try {
				X509Certificate openDJCerts[] = SSLService
						.loadCertificates(new FileInputStream(OPENDJ_CERTIFICATE_FILE));
				for (X509Certificate openDJCert : openDJCerts) {
					internalCertificates
							.add(new X509CertificateShortInfo(OPENDJ_CERTIFICATE_FILE, "OpenDJ SSL", openDJCert));
				}
			} catch (Exception e) {
				log.warn("OPENDJ certificate load exception");
			}
			try {
				X509Certificate httpdCerts[] = SSLService.loadCertificates(new FileInputStream(HTTPD_CERTIFICATE_FILE));
				for (X509Certificate httpdCert : httpdCerts) {
					internalCertificates
							.add(new X509CertificateShortInfo(HTTPD_CERTIFICATE_FILE, "HTTPD SSL", httpdCert));
				}
			} catch (Exception e) {
				log.warn("HTTPD Certificate load exception");
			}
			try {
				X509Certificate idpSigingCerts[] = SSLService
						.loadCertificates(new FileInputStream(IDP_SIGNING_CERTIFICATE_FILE));
				for (X509Certificate idpSigingCert : idpSigingCerts) {
					internalCertificates.add(
							new X509CertificateShortInfo(IDP_SIGNING_CERTIFICATE_FILE, "IDP SIGNING", idpSigingCert));
				}
			} catch (Exception e) {
				log.warn("IDP SIGNING certificate load exception");
			}
			try {
				X509Certificate idpEncryptionCerts[] = SSLService
						.loadCertificates(new FileInputStream(IDP_ENCRYPT_CERTIFICATE_FILE));
				for (X509Certificate idpEncryptionCert : idpEncryptionCerts) {
					internalCertificates.add(new X509CertificateShortInfo(IDP_ENCRYPT_CERTIFICATE_FILE,
							"IDP ENCRYPTION", idpEncryptionCert));
				}
			} catch (Exception e) {
				log.warn("IDP ENCRYPTION certificate load exception");
			}

			try {
				X509Certificate idpEncryptionCerts[] = SSLService
						.loadCertificates(new FileInputStream(OPENLDAP_CERTIFICATE_FILE));
				for (X509Certificate idpEncryptionCert : idpEncryptionCerts) {
					internalCertificates.add(new X509CertificateShortInfo(OPENLDAP_CERTIFICATE_FILE,
							"OpenLDAP ENCRYPTION", idpEncryptionCert));
				}
			} catch (Exception e) {
				log.error("OpenLDAP certificate load exception");
			}

		} catch (

		Exception e) {
			log.error("Load internalCertificates configuration exception", e);
		}

		try {
			// check for warning and search pattern
			final String searchPatternLC = this.searchPattern != null ? this.searchPattern.toLowerCase() : null;

			Iterator<X509CertificateShortInfo> certsIterator = asimbaCertificates.iterator();
			while (certsIterator.hasNext()) {
				X509CertificateShortInfo cert = certsIterator.next();
				// apply warning flag
				if (searchObsoleteWarning && !cert.isWarning())
					certsIterator.remove();
				// apply search pattern
				if (searchPatternLC != null && !searchPatternLC.isEmpty() && cert.getAlias() != null
						&& cert.getIssuer() != null) {
					if (!cert.getAlias().toLowerCase().contains(searchPatternLC)
							&& !cert.getIssuer().toLowerCase().contains(searchPatternLC))
						certsIterator.remove();
				}
			}

			certsIterator = trustStoreCertificates.iterator();
			while (certsIterator.hasNext()) {
				X509CertificateShortInfo cert = certsIterator.next();
				// apply warning flag
				if (searchObsoleteWarning && !cert.isWarning())
					certsIterator.remove();
				// apply search pattern
				if (searchPatternLC != null && !searchPatternLC.isEmpty() && cert.getAlias() != null
						&& cert.getIssuer() != null) {
					if (!cert.getAlias().toLowerCase().contains(searchPatternLC)
							&& !cert.getIssuer().toLowerCase().contains(searchPatternLC))
						certsIterator.remove();
				}
			}

			certsIterator = internalCertificates.iterator();
			while (certsIterator.hasNext()) {
				X509CertificateShortInfo cert = certsIterator.next();
				// apply warning flag
				if (searchObsoleteWarning && !cert.isWarning())
					certsIterator.remove();
				// apply search pattern
				if (searchPatternLC != null && !searchPatternLC.isEmpty() && cert.getAlias() != null
						&& cert.getIssuer() != null) {
					if (!cert.getAlias().toLowerCase().contains(searchPatternLC)
							&& !cert.getIssuer().toLowerCase().contains(searchPatternLC))
						certsIterator.remove();
				}
			}
		} catch (Exception e) {
			log.error("Update certificates status view exception", e);
		}
	}

	/**
	 * @return the asimbaCertificates
	 */
	public List<X509CertificateShortInfo> getAsimbaCertificates() {
		return asimbaCertificates;
	}

	/**
	 * @param asimbaCertificates
	 *            the asimbaCertificates to set
	 */
	public void setAsimbaCertificates(List<X509CertificateShortInfo> asimbaCertificates) {
		this.asimbaCertificates = asimbaCertificates;
	}

	/**
	 * @return the searchPattern
	 */
	public String getSearchPattern() {
		return searchPattern;
	}

	/**
	 * @param searchPattern
	 *            the searchPattern to set
	 */
	public void setSearchPattern(String searchPattern) {
		this.searchPattern = searchPattern;
	}

	/**
	 * @return the trustStoreCertificates
	 */
	public List<X509CertificateShortInfo> getTrustStoreCertificates() {
		return trustStoreCertificates;
	}

	/**
	 * @param trustStoreCertificates
	 *            the trustStoreCertificates to set
	 */
	public void setTrustStoreCertificates(List<X509CertificateShortInfo> trustStoreCertificates) {
		this.trustStoreCertificates = trustStoreCertificates;
	}

	/**
	 * @return the internalCertificates
	 */
	public List<X509CertificateShortInfo> getInternalCertificates() {
		return internalCertificates;
	}

	/**
	 * @param internalCertificates
	 *            the internalCertificates to set
	 */
	public void setInternalCertificates(List<X509CertificateShortInfo> internalCertificates) {
		this.internalCertificates = internalCertificates;
	}

	/**
	 * @return the searchObsoleteWarning
	 */
	public boolean isSearchObsoleteWarning() {
		return searchObsoleteWarning;
	}

	/**
	 * @param searchObsoleteWarning
	 *            the searchObsoleteWarning to set
	 */
	public void setSearchObsoleteWarning(boolean searchObsoleteWarning) {
		this.searchObsoleteWarning = searchObsoleteWarning;
	}

	public void download(X509CertificateShortInfo cert) {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
		response.setContentType("text/plain");
		String header = "attachment; filename=\"" + cert.getName() + "\"";
		response.addHeader("Content-disposition", header);
		try (ServletOutputStream os = response.getOutputStream()){
			File file = new File(cert.getPath());
			InputStream in = Files.newInputStream(file.toPath());
			IOUtils.copy(in, os);
			os.flush();
			facesContext.responseComplete();
		} catch (Exception e) {
			log.error("", e);
		}
	}
}
