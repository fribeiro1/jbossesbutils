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
package org.jboss.soa.esb.actions.xslt;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jboss.soa.esb.ConfigurationException;
import org.jboss.soa.esb.actions.AbstractActionPipelineProcessor;
import org.jboss.soa.esb.actions.ActionProcessingException;
import org.jboss.soa.esb.helpers.ConfigTree;
import org.jboss.soa.esb.message.Message;
import org.jboss.soa.esb.message.MessagePayloadProxy;
import org.jboss.soa.esb.message.Properties;

public class XsltAction extends AbstractActionPipelineProcessor {
	private static String ATTR_STYLESHEET = "stylesheet";

	private static TransformerFactory FACTORY = TransformerFactory
			.newInstance();

	private MessagePayloadProxy proxy;

	private String stylesheet;

	public XsltAction(ConfigTree conf) throws ConfigurationException {
		proxy = new MessagePayloadProxy(conf);

		stylesheet = conf.getRequiredAttribute(ATTR_STYLESHEET);
	}

	@Override
	public Message process(Message msg) throws ActionProcessingException {

		try {
			Transformer transformer = FACTORY
					.newTransformer(new StreamSource(Thread.currentThread()
							.getContextClassLoader().getResourceAsStream(
									stylesheet)));

			/* Pass the properties to the transformer */
			Properties props = msg.getProperties();

			String[] names = props.getNames();

			for (int i = 0; i < names.length; i++)
				transformer.setParameter(names[i], props.getProperty(names[i]));

			StringWriter writer = new StringWriter();

			transformer.transform(new StreamSource(new StringReader(
					(String) proxy.getPayload(msg))), new StreamResult(writer));

			proxy.setPayload(msg, writer.toString());

			return msg;
		} catch (Exception e) {
			throw new ActionProcessingException("Can't process message", e);
		}

	}

}