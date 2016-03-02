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
package org.jboss.soa.esb.extensions.messageproperty;

import javax.xml.transform.TransformerException;

import org.apache.xalan.extensions.ExpressionContext;
import org.apache.xml.utils.QName;
import org.jboss.soa.esb.message.Properties;

public final class MessagePropertyExtension {
	private static final String PARAM_PROPERTIES = "properties";

	public static Object getProperty(final ExpressionContext ctx,
			final String name) throws TransformerException {
		final Properties props = (Properties) ctx.getVariableOrParam(
				new QName(PARAM_PROPERTIES)).object();
	
		return props.getProperty(name);
	}

	public static Object getProperty(final ExpressionContext ctx,
			final String name, final Object defaultVal)
			throws TransformerException {
		final Properties props = (Properties) ctx.getVariableOrParam(
				new QName(PARAM_PROPERTIES)).object();

		final Object val = props.getProperty(name);

		return (val != null) ? val : defaultVal;
	}

}