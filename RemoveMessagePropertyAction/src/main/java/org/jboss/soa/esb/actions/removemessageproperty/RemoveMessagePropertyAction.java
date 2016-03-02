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
package org.jboss.soa.esb.actions.removemessageproperty;

import org.jboss.soa.esb.ConfigurationException;
import org.jboss.soa.esb.actions.AbstractActionPipelineProcessor;
import org.jboss.soa.esb.helpers.ConfigTree;
import org.jboss.soa.esb.message.Message;

public final class RemoveMessagePropertyAction extends
		AbstractActionPipelineProcessor {
	private static final String ATTR_NAME = "name";

	private String name;

	public RemoveMessagePropertyAction(final ConfigTree conf)
			throws ConfigurationException {
		name = conf.getRequiredAttribute(ATTR_NAME);
	}

	public Message process(final Message msg) {
		msg.getProperties().remove(name);

		return msg;
	}

}