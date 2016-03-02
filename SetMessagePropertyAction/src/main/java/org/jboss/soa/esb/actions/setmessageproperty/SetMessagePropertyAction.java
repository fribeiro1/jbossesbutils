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
package org.jboss.soa.esb.actions.setmessageproperty;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.jboss.soa.esb.ConfigurationException;
import org.jboss.soa.esb.actions.AbstractActionPipelineProcessor;
import org.jboss.soa.esb.actions.ActionProcessingException;
import org.jboss.soa.esb.helpers.ConfigTree;
import org.jboss.soa.esb.message.Message;
import org.jboss.soa.esb.message.MessagePayloadProxy;
import org.jboss.soa.esb.message.Properties;
import org.xml.sax.InputSource;

public final class SetMessagePropertyAction extends
		AbstractActionPipelineProcessor {

	@SuppressWarnings("unchecked")
	private static final class NamespaceContext implements
			javax.xml.namespace.NamespaceContext {
		private Map<String, String> nsMap;

		private NamespaceContext(final Map<String, String> nsMap) {
			this.nsMap = nsMap;
		}

		@Override
		public String getNamespaceURI(final String prefix) {

			if (prefix == null)
				throw new IllegalArgumentException("The prefix can't be null");

			if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix))
				return XMLConstants.NULL_NS_URI;

			if (XMLConstants.XML_NS_URI.equals(prefix))
				return XMLConstants.XML_NS_URI;

			if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(prefix))
				return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;

			if (!(nsMap.containsKey(prefix)))
				return XMLConstants.NULL_NS_URI;

			return nsMap.get(prefix);
		}

		@Override
		public String getPrefix(final String uri) {
			final Iterator prefixes = getPrefixes(uri);

			if (!prefixes.hasNext())
				return null;

			return (String) prefixes.next();
		}

		@Override
		public Iterator getPrefixes(final String uri) {

			if (uri == null)
				throw new IllegalArgumentException("The URI can't be null");

			final List<String> result = new ArrayList<String>();

			if (XMLConstants.NULL_NS_URI.equals(uri)) {
				result.add(XMLConstants.DEFAULT_NS_PREFIX);
			} else if (XMLConstants.XML_NS_URI.equals(uri)) {
				result.add(XMLConstants.XML_NS_PREFIX);
			} else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(uri)) {
				result.add(XMLConstants.XMLNS_ATTRIBUTE);
			} else {

				for (final Map.Entry<String, String> entry : nsMap.entrySet())

					if (entry.getValue().equals(uri))
						result.add(entry.getKey());

			}

			return result.iterator();
		}

	}

	private static final String ATTR_CONSTANT = "constant";
	private static final String ATTR_MODE = "mode";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_NAMESPACES = "namespaces";
	private static final String ATTR_XPATH = "xpath";

	private static final XPathFactory FACTORY = XPathFactory.newInstance();

	private static final String MODE_CONSTANT = "Constant";
	private static final String MODE_XPATH = "XPath";

	private static final Pattern PATTERN_NAMESPACES = Pattern
			.compile("([-._:A-Za-z0-9]*)=([^,]*),?");

	private String constant;

	private String expr;

	private String mode;

	private String name;

	private String namespaces;

	private MessagePayloadProxy proxy;

	public SetMessagePropertyAction(final ConfigTree conf)
			throws ConfigurationException {
		constant = conf.getAttribute(ATTR_CONSTANT);

		mode = conf.getRequiredAttribute(ATTR_MODE);

		name = conf.getRequiredAttribute(ATTR_NAME);

		namespaces = conf.getAttribute(ATTR_NAMESPACES);

		proxy = new MessagePayloadProxy(conf);

		expr = conf.getAttribute(ATTR_XPATH);
	}

	public Message process(final Message msg) throws ActionProcessingException {

		try {
			final Properties props = msg.getProperties();

			if (MODE_CONSTANT.equals(mode)) {

				if (constant != null)
					props.setProperty(name, constant);

			} else if (MODE_XPATH.equals(mode)) {

				if (expr != null) {
					final XPath xpath = FACTORY.newXPath();

					if (namespaces != null) {
						final Map<String, String> nsMap = new HashMap<String, String>();

						/* Configure the namespaces */
						final Matcher matcher = PATTERN_NAMESPACES
								.matcher(namespaces);

						while (matcher.find())
							nsMap.put(matcher.group(1), matcher.group(2));

						xpath.setNamespaceContext(new NamespaceContext(nsMap));
					}

					final String payload = (String) proxy.getPayload(msg);

					props.setProperty(name, xpath.evaluate(expr,
							new InputSource(new StringReader(payload))));
				}

			}

		} catch (final Exception e) {
			throw new ActionProcessingException("Can't process message", e);
		}

		return msg;
	}

}