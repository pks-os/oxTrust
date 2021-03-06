package org.gluu.oxtrust.service;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xdi.config.oxtrust.AppConfiguration;
import org.xdi.model.ApplicationType;
import org.xdi.service.CacheService;
import org.xdi.service.cache.CacheProvider;
import org.xdi.service.cache.NativePersistenceCacheProvider;
import org.xdi.service.cdi.async.Asynchronous;
import org.xdi.service.cdi.event.CleanerEvent;
import org.xdi.service.cdi.event.Scheduled;
import org.xdi.service.timer.event.TimerEvent;
import org.xdi.service.timer.schedule.TimerSchedule;

/**
 * Cleaner service
 * 
 * @author Yuriy Movchan Date: 09/01/2018
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class CleanerTimer {

	public final static int BATCH_SIZE = 100;
	private final static int DEFAULT_INTERVAL = 600; // 10 minutes

	@Inject
	private Logger log;

	@Inject
	private PasswordResetService passwordResetService;

	@Inject
	private CacheProvider cacheProvider;

	@Inject
	private MetricService metricService;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private Event<TimerEvent> cleanerEvent;

	@Inject
	private CleanUpLogger cleanUpLogger;

	private AtomicBoolean isActive;

	public void initTimer() {
		log.debug("Initializing Cleaner Timer");
		cleanUpLogger.addNewLogLine("Initializing Cleaner Timer at:" + new Date());
		this.isActive = new AtomicBoolean(false);
		int interval = appConfiguration.getCleanServiceInterval();
		if (interval <= 0) {
			interval = DEFAULT_INTERVAL;
		}
		cleanerEvent.fire(
				new TimerEvent(new TimerSchedule(interval, interval), new CleanerEvent(), Scheduled.Literal.INSTANCE));
		cleanUpLogger.addNewLogLine("Initialization Done at :" + new Date());
	}

	@Asynchronous
	public void process(@Observes @Scheduled CleanerEvent cleanerEvent) {
		cleanUpLogger.addNewLogLine("++++Starting processing clean up services at:" + new Date());
		if (this.isActive.get()) {
			return;
		}
		if (!this.isActive.compareAndSet(false, true)) {
			return;
		}
		try {
			Date now = new Date();
			processCache(now);
			processPasswordReset();
			processMetricEntries();
		} finally {
			this.isActive.set(false);
		}
		cleanUpLogger.addNewLogLine("+Processing clean up services done at:" + new Date());
	}

	protected void processPasswordReset() {
		cleanUpLogger.addNewLogLine("-Starting processing PasswordReset clean up at:" + new Date());
		Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		calendar.add(Calendar.SECOND, -appConfiguration.getPasswordResetRequestExpirationTime());
		final Date expirationDate = calendar.getTime();
		cleanUpLogger.addNewLogLine("-Running password reset clean up with expiration date :" + expirationDate);
		passwordResetService.cleanup(expirationDate);
		cleanUpLogger.addNewLogLine("-Processing PasswordReset clean up at:" + new Date());
	}

	private void processCache(Date now) {
		cleanUpLogger.addNewLogLine("~Starting processing cache at:" + now);
		try {
            cacheProvider.cleanup(now);
		} catch (Exception e) {
			log.error("Failed to clean up cache.", e);
			cleanUpLogger.addNewLogLineAsError("~Error occurs while processing cache");
			cleanUpLogger.addNewLogLineAsError("~Error message: " + e.getMessage());
		}
		cleanUpLogger.addNewLogLine("~Processing cache done at:" + new Date());
	}

	private void processMetricEntries() {
		cleanUpLogger.addNewLogLine("#Starting processing Metric entries at:" + new Date());
		log.debug("Start metric entries clean up");
		int keepDataDays = appConfiguration.getMetricReporterKeepDataDays();
		Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		calendar.add(Calendar.DATE, -keepDataDays);
		Date expirationDate = calendar.getTime();
		cleanUpLogger.addNewLogLine(String.format(
				"#Ready to remove expired entries with parameters batch size: %s, expiration date: %s and appliance inum %s",
				BATCH_SIZE, expirationDate, metricService.applianceInum()));
		metricService.removeExpiredMetricEntries(expirationDate, ApplicationType.OX_TRUST, metricService.applianceInum(),
				0, BATCH_SIZE);
		log.debug("End metric entries clean up");
		cleanUpLogger.addNewLogLine("#Processing Metric entries done at:" + new Date());
	}
}
