/*
 *  Copyright 2019 Information and Computational Sciences,
 *  The James Hutton Institute.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jhi.pickamix.server.util;

import jhi.pickamix.server.database.Database;
import org.apache.commons.io.monitor.*;
import org.jooq.tools.StringUtils;

import java.io.*;
import java.net.URL;
import java.util.Properties;

/**
 * {@link PropertyWatcher} is a wrapper around {@link Properties} to readAll properties.
 *
 * @author Sebastian Raubach
 */
public class PropertyWatcher
{
	public static final String PROP_EXT_DATA = "data.directory.external";

	public static final String PROP_REMOTE_SPREADSHEET_URL = "remote.spreadsheet.url";

	public static final String PROP_DATABASE_SERVER   = "database.server";
	public static final String PROP_DATABASE_NAME     = "database.name";
	public static final String PROP_DATABASE_PORT     = "database.port";
	public static final String PROP_DATABASE_USERNAME = "database.username";
	public static final String PROP_DATABASE_PASSWORD = "database.password";


	/** The name of the properties file */
	private static final String PROPERTIES_FILE = "config.properties";

	private static Properties properties = new Properties();

	private static FileAlterationMonitor monitor;
	private static File                  config = null;

	/**
	 * Attempts to reads the properties file and then checks the required properties.
	 */
	public static void initialize()
	{
		/* Start to listen for file changes */
		try
		{
			// We have to load the internal one initially to figure out where the external data directory is...
			URL resource = PropertyWatcher.class.getClassLoader().getResource(PROPERTIES_FILE);
			if (resource != null)
			{
				config = new File(resource.toURI());
				loadProperties(false);

				// Then check if there's another version in the external data directory
				String path = get(PROP_EXT_DATA);
				if (path != null)
				{
					File folder = new File(path);
					if (folder.exists() && folder.isDirectory())
					{
						File potential = new File(folder, PROPERTIES_FILE);

						if (potential.exists() && potential.isFile())
						{
							// Use it
							config = potential;
						}
					}
				}

				// Finally, load it properly. This is either the original file or the external file.
				loadProperties(true);

				// Then watch whichever file exists for changes
				FileAlterationObserver observer = new FileAlterationObserver(config.getParentFile());
				monitor = new FileAlterationMonitor(1000L);
				observer.addListener(new FileAlterationListenerAdaptor()
				{
					@Override
					public void onFileChange(File file)
					{
						if (file.equals(config))
						{
							loadProperties(true);
						}
					}
				});
				monitor.addObserver(observer);
				monitor.start();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private static void loadProperties(boolean checkAndInit)
	{
		try (FileInputStream stream = new FileInputStream(config))
		{
			properties.load(stream);
		}
		catch (IOException | NullPointerException e)
		{
			throw new RuntimeException(e);
		}

		if (checkAndInit)
		{
			Database.init(get(PROP_DATABASE_SERVER), get(PROP_DATABASE_NAME), get(PROP_DATABASE_PORT), get(PROP_DATABASE_USERNAME), get(PROP_DATABASE_PASSWORD), true);
		}
	}

	public static void stopFileWatcher()
	{
		try
		{
			if (monitor != null)
				monitor.stop();

			monitor = null;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reads a property from the .properties file
	 *
	 * @param property The property to readAll
	 * @return The property or <code>null</code> if the property is not found
	 */
	public static String get(String property)
	{
		String value = properties.getProperty(property);

		return StringUtils.isBlank(value) ? null : value.strip();
	}
}
