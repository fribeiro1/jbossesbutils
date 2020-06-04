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
package org.jboss.soa.esb.actions.http;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.jboss.soa.esb.ConfigurationException;
import org.jboss.soa.esb.actions.AbstractActionPipelineProcessor;
import org.jboss.soa.esb.actions.ActionProcessingException;
import org.jboss.soa.esb.helpers.ConfigTree;
import org.jboss.soa.esb.message.Message;
import org.jboss.soa.esb.message.MessagePayloadProxy;
import org.jboss.soa.esb.message.Properties;

public class HttpAction extends AbstractActionPipelineProcessor {
	private static String ATTR_METHOD = "method";
	private static String ATTR_URI = "uri";

	private static String METHOD_DELETE = "DELETE";
	private static String METHOD_GET = "GET";
	private static String METHOD_POST = "POST";
	private static String METHOD_PUT = "PUT";

	private static Pattern PATTERN_HEADER = Pattern
			.compile("org\\.jboss\\.soa\\.esb\\.actions\\.http\\.header\\.(.*)");
	private static Pattern PATTERN_PARAM = Pattern
			.compile("org\\.jboss\\.soa\\.esb\\.actions\\.http\\.param\\.(.*)");

	private static String PREFIX_HEADER = "org.jboss.soa.esb.actions.http.header.";

	private String method;

	private MessagePayloadProxy proxy;

	private String uri;

	public HttpAction(ConfigTree conf) throws ConfigurationException {
		method = conf.getRequiredAttribute(ATTR_METHOD);

		proxy = new MessagePayloadProxy(conf);

		uri = conf.getRequiredAttribute(ATTR_URI);
	}

	public Message process(Message msg) throws ActionProcessingException {

		try {
			HttpClient client = new HttpClient();

			Properties props = msg.getProperties();

			String[] names = props.getNames();

			if (METHOD_DELETE.equals(method)) {
				HttpMethodBase req = new DeleteMethod(uri);

				for (int i = 0; i < names.length; i++) {
					Matcher headerMatcher = PATTERN_HEADER
							.matcher(names[i]);

					if (headerMatcher.find())
						req.addRequestHeader(headerMatcher.group(1),
								(String) props.getProperty(headerMatcher
										.group()));

				}

				client.executeMethod(req);

				Header[] headers = req.getResponseHeaders();

				for (int i = 0; i < headers.length; i++)
					props.setProperty(PREFIX_HEADER + headers[i].getName(),
							headers[i].getValue());

				proxy.setPayload(msg, req.getResponseBodyAsString());
			} else if (METHOD_GET.equals(method)) {
				HttpMethodBase req = new GetMethod(uri);

				List<NameValuePair> paramList = new ArrayList<NameValuePair>();

				for (int i = 0; i < names.length; i++) {
					Matcher headerMatcher = PATTERN_HEADER
							.matcher(names[i]);

					if (headerMatcher.find()) {
						req.addRequestHeader(headerMatcher.group(1),
								(String) props.getProperty(headerMatcher
										.group()));

						continue;
					}

					Matcher paramMatcher = PATTERN_PARAM
							.matcher(names[i]);

					if (paramMatcher.find())
						paramList.add(new NameValuePair(paramMatcher.group(1),
								(String) props
										.getProperty(paramMatcher.group())));

				}

				req.setQueryString(paramList.toArray(new NameValuePair[] {}));

				client.executeMethod(req);

				Header[] headers = req.getResponseHeaders();

				for (int i = 0; i < headers.length; i++)
					props.setProperty(PREFIX_HEADER + headers[i].getName(),
							headers[i].getValue());

				proxy.setPayload(msg, req.getResponseBodyAsString());
			} else if (METHOD_POST.equals(method)) {
				PostMethod req = new PostMethod(uri);

				for (int i = 0; i < names.length; i++) {
					Matcher headerMatcher = PATTERN_HEADER
							.matcher(names[i]);

					if (headerMatcher.find()) {
						req.addRequestHeader(headerMatcher.group(1),
								(String) props.getProperty(headerMatcher
										.group()));

						continue;
					}

					Matcher paramMatcher = PATTERN_PARAM
							.matcher(names[i]);

					if (paramMatcher.find())
						req.addParameter(new NameValuePair(paramMatcher
								.group(1), (String) props
								.getProperty(paramMatcher.group())));

				}

				req.setRequestEntity(new StringRequestEntity((String) proxy
						.getPayload(msg), null, null));

				client.executeMethod(req);

				Header[] headers = req.getResponseHeaders();

				for (int i = 0; i < headers.length; i++)
					props.setProperty(PREFIX_HEADER + headers[i].getName(),
							headers[i].getValue());

				proxy.setPayload(msg, req.getResponseBodyAsString());
			} else if (METHOD_PUT.equals(method)) {
				EntityEnclosingMethod req = new PutMethod(uri);

				for (int i = 0; i < names.length; i++) {
					Matcher headerMatcher = PATTERN_HEADER
							.matcher(names[i]);

					if (headerMatcher.find())
						req.addRequestHeader(headerMatcher.group(1),
								(String) props.getProperty(headerMatcher
										.group()));

				}

				req.setRequestEntity(new StringRequestEntity((String) proxy
						.getPayload(msg), null, null));

				client.executeMethod(req);

				Header[] headers = req.getResponseHeaders();

				for (int i = 0; i < headers.length; i++)
					props.setProperty(PREFIX_HEADER + headers[i].getName(),
							headers[i].getValue());

				proxy.setPayload(msg, req.getResponseBodyAsString());
			}

		} catch (Exception e) {
			throw new ActionProcessingException("Can't process message", e);
		}

		return msg;
	}

}