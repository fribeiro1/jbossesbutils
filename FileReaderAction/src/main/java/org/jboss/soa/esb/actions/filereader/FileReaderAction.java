/*
 * Copyright 2011 Fernando Ribeiro
 * 
 * This file is part of JBoss ESB Utils.
 *
 * JBoss ESB Utils is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * JBoss ESB Utils is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with JBoss ESB Utils. If not, see <http://www.gnu.org/licenses/>.
 */
package org.jboss.soa.esb.actions.filereader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;

import org.jboss.soa.esb.ConfigurationException;
import org.jboss.soa.esb.actions.AbstractActionPipelineProcessor;
import org.jboss.soa.esb.actions.ActionProcessingException;
import org.jboss.soa.esb.helpers.ConfigTree;
import org.jboss.soa.esb.message.Message;
import org.jboss.soa.esb.message.MessagePayloadProxy;

public class FileReaderAction extends AbstractActionPipelineProcessor {
	private static String ATTR_FILE = "file";
	private static String ATTR_SOURCE = "source";

	private static String SOURCE_CLASSPATH = "classpath";
	private static String SOURCE_FILESYSTEM = "filesystem";

	private String file;

	private MessagePayloadProxy proxy;

	private String source;

	public FileReaderAction(ConfigTree conf)
			throws ConfigurationException {
		file = conf.getRequiredAttribute(ATTR_FILE);

		proxy = new MessagePayloadProxy(conf);

		source = conf.getRequiredAttribute(ATTR_SOURCE);
	}

	public Message process(Message msg) throws ActionProcessingException {

		try {
			StringBuffer buf = new StringBuffer();

			Reader reader = null;

			if (SOURCE_CLASSPATH.equals(source))
				reader = new BufferedReader(new InputStreamReader(Thread
						.currentThread().getContextClassLoader()
						.getResourceAsStream(file)));
			else if (SOURCE_FILESYSTEM.equals(source))
				reader = new BufferedReader(new FileReader(file));

			int i = reader.read();

			while (i != -1) {
				buf.append((char) i);

				i = reader.read();
			}

			reader.close();

			proxy.setPayload(msg, buf.toString());
		} catch (Exception e) {
			throw new ActionProcessingException("Can't process message", e);
		}

		return msg;
	}

}