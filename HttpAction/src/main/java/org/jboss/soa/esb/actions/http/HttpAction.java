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

public final class HttpAction extends AbstractActionPipelineProcessor {
	private static final String ATTR_METHOD = "method";
	private static final String ATTR_URI = "uri";

	private static final String METHOD_DELETE = "DELETE";
	private static final String METHOD_GET = "GET";
	private static final String METHOD_POST = "POST";
	private static final String METHOD_PUT = "PUT";

	private static final Pattern PATTERN_HEADER = Pattern
			.compile("org\\.jboss\\.soa\\.esb\\.actions\\.http\\.header\\.(.*)");
	private static final Pattern PATTERN_PARAM = Pattern
			.compile("org\\.jboss\\.soa\\.esb\\.actions\\.http\\.param\\.(.*)");

	private static final String PREFIX_HEADER = "org.jboss.soa.esb.actions.http.header.";

	private String method;

	private MessagePayloadProxy proxy;

	private String uri;

	public HttpAction(final ConfigTree conf) throws ConfigurationException {
		method = conf.getRequiredAttribute(ATTR_METHOD);

		proxy = new MessagePayloadProxy(conf);

		uri = conf.getRequiredAttribute(ATTR_URI);
	}

	public Message process(final Message msg) throws ActionProcessingException {

		try {
			final HttpClient client = new HttpClient();

			final Properties props = msg.getProperties();

			final String[] names = props.getNames();

			if (METHOD_DELETE.equals(method)) {
				final HttpMethodBase req = new DeleteMethod(uri);

				for (int i = 0; i < names.length; i++) {
					final Matcher headerMatcher = PATTERN_HEADER
							.matcher(names[i]);

					if (headerMatcher.find())
						req.addRequestHeader(headerMatcher.group(1),
								(String) props.getProperty(headerMatcher
										.group()));

				}

				client.executeMethod(req);

				final Header[] headers = req.getResponseHeaders();

				for (int i = 0; i < headers.length; i++)
					props.setProperty(PREFIX_HEADER + headers[i].getName(),
							headers[i].getValue());

				proxy.setPayload(msg, req.getResponseBodyAsString());
			} else if (METHOD_GET.equals(method)) {
				final HttpMethodBase req = new GetMethod(uri);

				final List<NameValuePair> paramList = new ArrayList<NameValuePair>();

				for (int i = 0; i < names.length; i++) {
					final Matcher headerMatcher = PATTERN_HEADER
							.matcher(names[i]);

					if (headerMatcher.find()) {
						req.addRequestHeader(headerMatcher.group(1),
								(String) props.getProperty(headerMatcher
										.group()));

						continue;
					}

					final Matcher paramMatcher = PATTERN_PARAM
							.matcher(names[i]);

					if (paramMatcher.find())
						paramList.add(new NameValuePair(paramMatcher.group(1),
								(String) props
										.getProperty(paramMatcher.group())));

				}

				req.setQueryString(paramList.toArray(new NameValuePair[] {}));

				client.executeMethod(req);

				final Header[] headers = req.getResponseHeaders();

				for (int i = 0; i < headers.length; i++)
					props.setProperty(PREFIX_HEADER + headers[i].getName(),
							headers[i].getValue());

				proxy.setPayload(msg, req.getResponseBodyAsString());
			} else if (METHOD_POST.equals(method)) {
				final PostMethod req = new PostMethod(uri);

				for (int i = 0; i < names.length; i++) {
					final Matcher headerMatcher = PATTERN_HEADER
							.matcher(names[i]);

					if (headerMatcher.find()) {
						req.addRequestHeader(headerMatcher.group(1),
								(String) props.getProperty(headerMatcher
										.group()));

						continue;
					}

					final Matcher paramMatcher = PATTERN_PARAM
							.matcher(names[i]);

					if (paramMatcher.find())
						req.addParameter(new NameValuePair(paramMatcher
								.group(1), (String) props
								.getProperty(paramMatcher.group())));

				}

				req.setRequestEntity(new StringRequestEntity((String) proxy
						.getPayload(msg), null, null));

				client.executeMethod(req);

				final Header[] headers = req.getResponseHeaders();

				for (int i = 0; i < headers.length; i++)
					props.setProperty(PREFIX_HEADER + headers[i].getName(),
							headers[i].getValue());

				proxy.setPayload(msg, req.getResponseBodyAsString());
			} else if (METHOD_PUT.equals(method)) {
				final EntityEnclosingMethod req = new PutMethod(uri);

				for (int i = 0; i < names.length; i++) {
					final Matcher headerMatcher = PATTERN_HEADER
							.matcher(names[i]);

					if (headerMatcher.find())
						req.addRequestHeader(headerMatcher.group(1),
								(String) props.getProperty(headerMatcher
										.group()));

				}

				req.setRequestEntity(new StringRequestEntity((String) proxy
						.getPayload(msg), null, null));

				client.executeMethod(req);

				final Header[] headers = req.getResponseHeaders();

				for (int i = 0; i < headers.length; i++)
					props.setProperty(PREFIX_HEADER + headers[i].getName(),
							headers[i].getValue());

				proxy.setPayload(msg, req.getResponseBodyAsString());
			}

		} catch (final Exception e) {
			throw new ActionProcessingException("Can't process message", e);
		}

		return msg;
	}

}