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
package org.jboss.soa.esb.actions.splitter;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.jboss.soa.esb.Service;
import org.jboss.soa.esb.actions.AbstractActionPipelineProcessor;
import org.jboss.soa.esb.actions.ActionProcessingException;
import org.jboss.soa.esb.addressing.EPR;
import org.jboss.soa.esb.addressing.eprs.LogicalEPR;
import org.jboss.soa.esb.client.ServiceInvoker;
import org.jboss.soa.esb.helpers.ConfigTree;
import org.jboss.soa.esb.message.Message;
import org.jboss.soa.esb.message.MessagePayloadProxy;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@SuppressWarnings("unchecked")
public class SplitterAction extends AbstractActionPipelineProcessor {

	private static class NamespaceContext implements
			javax.xml.namespace.NamespaceContext {
		private Map<String, String> nsMap;

		private NamespaceContext(Map<String, String> nsMap) {
			this.nsMap = nsMap;
		}

		@Override
		public String getNamespaceURI(String prefix) {

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
		public String getPrefix(String uri) {
			Iterator prefixes = getPrefixes(uri);

			if (!prefixes.hasNext())
				return null;

			return (String) prefixes.next();
		}

		@Override
		public Iterator getPrefixes(String uri) {

			if (uri == null)
				throw new IllegalArgumentException("The URI can't be null");

			List<String> result = new ArrayList<String>();

			if (XMLConstants.NULL_NS_URI.equals(uri)) {
				result.add(XMLConstants.DEFAULT_NS_PREFIX);
			} else if (XMLConstants.XML_NS_URI.equals(uri)) {
				result.add(XMLConstants.XML_NS_PREFIX);
			} else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(uri)) {
				result.add(XMLConstants.XMLNS_ATTRIBUTE);
			} else {

				for (Map.Entry<String, String> entry : nsMap.entrySet()) {

					if (entry.getValue().equals(uri))
						result.add(entry.getKey());

				}

			}

			return result.iterator();
		}

	}

	private static String ATTR_NAMESPACES = "namespaces";
	private static String ATTR_ROUTE_TO = "route-to";
	private static String ATTR_SERVICE_CATEGORY = "service-category";
	private static String ATTR_SERVICE_NAME = "service-name";
	private static String ATTR_XPATH = "xpath";

	private static DocumentBuilderFactory BUILDER_FACTORY = DocumentBuilderFactory
			.newInstance();

	private static Pattern PATTERN_NAMESPACES = Pattern
			.compile("([-._:A-Za-z0-9]*)=([^,]*),?");

	private static TransformerFactory TRANSFORMER_FACTORY = TransformerFactory
			.newInstance();

	private static XPathFactory XPATH_FACTORY = XPathFactory
			.newInstance();

	private DocumentBuilder builder;

	private EPR dlq;

	private List<Service> dstList = new ArrayList<Service>();

	private String expr;

	private String namespaces;

	private MessagePayloadProxy proxy;

	private Transformer transformer;

	public SplitterAction(ConfigTree conf) throws Exception {
		builder = BUILDER_FACTORY.newDocumentBuilder();

		dlq = new LogicalEPR(ServiceInvoker.INTERNAL_SERVICE_CATEGORY,
				ServiceInvoker.DEAD_LETTER_SERVICE_NAME);

		ConfigTree[] dstArr = conf.getChildren(ATTR_ROUTE_TO);

		for (int i = 0; i < dstArr.length; i++)
			dstList.add(new Service(dstArr[i]
					.getRequiredAttribute(ATTR_SERVICE_CATEGORY), dstArr[i]
					.getRequiredAttribute(ATTR_SERVICE_NAME)));

		namespaces = conf.getAttribute(ATTR_NAMESPACES);

		proxy = new MessagePayloadProxy(conf);

		transformer = TRANSFORMER_FACTORY.newTransformer();

		expr = conf.getRequiredAttribute(ATTR_XPATH);
	}

	public Message process(Message origMsg)
			throws ActionProcessingException {

		try {

			if (expr != null) {
				XPath xpath = XPATH_FACTORY.newXPath();

				if (namespaces != null) {
					Map<String, String> nsMap = new HashMap<String, String>();

					/* Configure the namespaces */
					Matcher matcher = PATTERN_NAMESPACES
							.matcher(namespaces);

					while (matcher.find())
						nsMap.put(matcher.group(1), matcher.group(2));

					xpath.setNamespaceContext(new NamespaceContext(nsMap));
				}

				String payload = (String) proxy.getPayload(origMsg);

				NodeList resultList = (NodeList) xpath.evaluate(expr,
						new InputSource(new StringReader(payload)),
						XPathConstants.NODESET);

				for (Service dst : dstList) {
					ServiceInvoker invoker = new ServiceInvoker(dst
							.getCategory(), dst.getName());

					for (int i = 0; i < resultList.getLength(); i++) {
						Message newMsg = origMsg.copy();

						newMsg.getHeader().getCall().setReplyTo(dlq);

						Document doc = builder.newDocument();

						doc.appendChild(doc
								.importNode(resultList.item(i), true));

						Writer writer = new StringWriter();

						transformer.transform(new DOMSource(doc),
								new StreamResult(writer));

						proxy.setPayload(newMsg, writer.toString());

						invoker.deliverAsync(newMsg);
					}

				}

			}

		} catch (Exception e) {
			throw new ActionProcessingException("Can't process message", e);
		}

		return origMsg;
	}

}