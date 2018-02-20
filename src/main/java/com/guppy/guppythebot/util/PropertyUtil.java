package com.guppy.guppythebot.util;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.guppy.guppythebot.Bootstrap;

public class PropertyUtil
{
	private static final Logger LOG = LogManager.getLogger(PropertyUtil.class);
	public static Properties properties = Bootstrap.getDbUtil().lookupProperties();
	
	public static String getProperty(String propertyKey) {
		if (properties.containsKey(propertyKey) && !StringUtils.isBlank(propertyKey)) {
			return properties.getProperty(propertyKey);
		}
		else {
			LOG.error("Could not find property " + propertyKey);
			return null;
		}
	}
	
}
