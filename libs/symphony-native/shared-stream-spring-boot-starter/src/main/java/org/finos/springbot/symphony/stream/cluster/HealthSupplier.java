package org.finos.springbot.symphony.stream.cluster;

import java.util.function.BooleanSupplier;

/**
 * Returns true/false status for the bot's health.
 * 
 * @author rob@kite9.com
 *
 */
public interface HealthSupplier extends BooleanSupplier {

}
