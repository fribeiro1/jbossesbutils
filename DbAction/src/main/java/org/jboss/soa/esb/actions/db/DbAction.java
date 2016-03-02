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
package org.jboss.soa.esb.actions.db;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;
import java.util.List;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.jboss.soa.esb.ConfigurationException;
import org.jboss.soa.esb.actions.AbstractActionPipelineProcessor;
import org.jboss.soa.esb.actions.ActionLifecycleException;
import org.jboss.soa.esb.actions.ActionProcessingException;
import org.jboss.soa.esb.actions.db.model.BlobField;
import org.jboss.soa.esb.actions.db.model.BlobParam;
import org.jboss.soa.esb.actions.db.model.BoolParam;
import org.jboss.soa.esb.actions.db.model.ClobField;
import org.jboss.soa.esb.actions.db.model.ClobParam;
import org.jboss.soa.esb.actions.db.model.DateField;
import org.jboss.soa.esb.actions.db.model.DateParam;
import org.jboss.soa.esb.actions.db.model.DbRequest;
import org.jboss.soa.esb.actions.db.model.DbResponse;
import org.jboss.soa.esb.actions.db.model.DecField;
import org.jboss.soa.esb.actions.db.model.DecParam;
import org.jboss.soa.esb.actions.db.model.IntField;
import org.jboss.soa.esb.actions.db.model.IntParam;
import org.jboss.soa.esb.actions.db.model.NumField;
import org.jboss.soa.esb.actions.db.model.NumParam;
import org.jboss.soa.esb.actions.db.model.ObjectFactory;
import org.jboss.soa.esb.actions.db.model.Param;
import org.jboss.soa.esb.actions.db.model.Row;
import org.jboss.soa.esb.actions.db.model.StrField;
import org.jboss.soa.esb.actions.db.model.StrParam;
import org.jboss.soa.esb.helpers.ConfigTree;
import org.jboss.soa.esb.message.Message;
import org.jboss.soa.esb.message.MessagePayloadProxy;
import org.xml.sax.InputSource;

public final class DbAction extends AbstractActionPipelineProcessor {
	private static final ObjectFactory FACTORY = new ObjectFactory();

	private static final String MESSAGE = "Can't process message";

	private static final String ATTR_DRIVER = "driver";
	private static final String ATTR_PASSWORD = "password";
	private static final String ATTR_URL = "url";
	private static final String ATTR_USERNAME = "username";

	private String driver;

	private Marshaller marshaller;

	private String password;

	private MessagePayloadProxy proxy;

	private Unmarshaller unmarshaller;

	private String url;

	private String username;

	public DbAction(final ConfigTree conf) throws ConfigurationException,
			JAXBException {
		proxy = new MessagePayloadProxy(conf);

		driver = conf.getRequiredAttribute(ATTR_DRIVER);

		password = conf.getAttribute(ATTR_PASSWORD);

		url = conf.getRequiredAttribute(ATTR_URL);

		username = conf.getAttribute(ATTR_USERNAME);

		final JAXBContext ctx = JAXBContext
				.newInstance("org.jboss.soa.esb.actions.db.model");

		unmarshaller = ctx.createUnmarshaller();

		marshaller = ctx.createMarshaller();
	}

	@Override
	public void initialise() throws ActionLifecycleException {

		try {
			Class.forName(driver);
		} catch (final ClassNotFoundException e) {
			throw new ActionLifecycleException("Can't initialize action", e);
		}

	}

	@Override
	public Message process(final Message msg) throws ActionProcessingException {
		Connection dbConn = null;

		PreparedStatement dbStmt = null;

		ResultSet dbRs = null;

		try {
			dbConn = DriverManager.getConnection(url, username, password);

			final DbRequest req = (DbRequest) unmarshaller
					.unmarshal(new InputSource(new StringReader((String) proxy
							.getPayload(msg))));

			dbStmt = dbConn.prepareStatement(req.getExpr());

			final List<JAXBElement<? extends Param>> paramElementList = req
					.getParamElementList();

			for (final JAXBElement<? extends Param> paramElement : paramElementList) {
				final Param param = paramElement.getValue();

				final boolean nil = paramElement.isNil();
				
				final int id = param.getId();

				if (param instanceof BlobParam) {

					if (!nil)
						dbStmt.setBlob(id, new ByteArrayInputStream(
								DatatypeConverter.parseBase64Binary(param
										.getContent())));
					else
						dbStmt.setNull(id, Types.BLOB);

				} else if (param instanceof BoolParam) {

					if (!nil)
						dbStmt.setBoolean(id, DatatypeConverter
								.parseBoolean(param.getContent()));
					else
						dbStmt.setNull(id, Types.BOOLEAN);

				} else if (param instanceof ClobParam) {

					if (!nil)
						dbStmt.setClob(id, new StringReader(DatatypeConverter
								.parseString(param.getContent())));
					else
						dbStmt.setNull(id, Types.CLOB);

				} else if (param instanceof DateParam) {

					if (!nil)
						dbStmt.setDate(id, new Date(DatatypeConverter
								.parseDate(param.getContent())
								.getTimeInMillis()));
					else
						dbStmt.setNull(id, Types.DATE);

				} else if (param instanceof DecParam) {

					if (!nil)
						dbStmt.setBigDecimal(id, DatatypeConverter
								.parseDecimal(param.getContent()));
					else
						dbStmt.setNull(id, Types.DECIMAL);

				} else if (param instanceof IntParam) {

					if (!nil)
						dbStmt.setInt(id, DatatypeConverter.parseInt(param
								.getContent()));
					else
						dbStmt.setNull(id, Types.INTEGER);

				} else if (param instanceof NumParam) {

					if (!nil)
						dbStmt.setBigDecimal(id, DatatypeConverter
								.parseDecimal(param.getContent()));
					else
						dbStmt.setNull(id, Types.NUMERIC);

				} else if (param instanceof StrParam) {

					if (!nil)
						dbStmt.setString(id, DatatypeConverter
								.parseString(param.getContent()));
					else
						dbStmt.setNull(id, Types.VARCHAR);

				}

			}

			final DbResponse res = FACTORY.createDbResponse();

			boolean hasMoreResults = dbStmt.execute();

			int dbCnt = dbStmt.getUpdateCount();

			while ((hasMoreResults) || (dbCnt != -1)) {

				if (hasMoreResults) {
					final org.jboss.soa.esb.actions.db.model.ResultSet rs = FACTORY
							.createResultSet();

					dbRs = dbStmt.getResultSet();

					while (dbRs.next()) {
						final Row row = FACTORY.createRow();

						final ResultSetMetaData dbMetaData = dbRs.getMetaData();

						for (int j = 1; j <= dbMetaData.getColumnCount(); j++) {
							final int type = dbMetaData.getColumnType(j);

							final String name = dbMetaData.getColumnName(j);

							if (Types.BLOB == type) {
								final BlobField field = FACTORY
										.createBlobField();

								field.setName(name);

								final Blob value = dbRs.getBlob(j);

								if (value != null) {
									final ByteBuffer buf = ByteBuffer
											.allocate((int) value.length());

									final BufferedInputStream in = new BufferedInputStream(
											value.getBinaryStream());

									int i = in.read();

									while (i != -1) {
										buf.put((byte) i);

										i = in.read();
									}

									field.setContent(DatatypeConverter
											.printBase64Binary(buf.array()));
								}

								final JAXBElement<BlobField> fieldElement = FACTORY
										.createRowBlob(field);

								if (value == null)
									fieldElement.setNil(true);

								row.getFieldElementList().add(fieldElement);
							} else if (Types.CLOB == type) {
								final ClobField field = FACTORY
										.createClobField();

								field.setName(name);

								final Clob value = dbRs.getClob(j);

								if (value != null) {
									final StringBuffer buf = new StringBuffer();

									final BufferedReader reader = new BufferedReader(
											value.getCharacterStream());

									int i = reader.read();

									while (i != -1) {
										buf.append((char) i);

										i = reader.read();
									}

									field.setContent(DatatypeConverter
											.printString(buf.toString()));
								}

								final JAXBElement<ClobField> fieldElement = FACTORY
										.createRowClob(field);

								if (value == null)
									fieldElement.setNil(true);

								row.getFieldElementList().add(fieldElement);
							} else if (Types.DATE == type) {
								final DateField field = FACTORY
										.createDateField();

								field.setName(name);

								final Date value = dbRs.getDate(j);

								if (value != null) {
									final Calendar cal = Calendar.getInstance();

									cal.setTime(value);

									field.setContent(DatatypeConverter
											.printDate(cal));
								}

								final JAXBElement<DateField> fieldElement = FACTORY
										.createRowDate(field);

								if (value == null)
									fieldElement.setNil(true);

								row.getFieldElementList().add(fieldElement);
							} else if (Types.DECIMAL == type) {
								final DecField field = FACTORY.createDecField();

								field.setName(name);

								final BigDecimal value = dbRs.getBigDecimal(j);

								if (value != null)
									field.setContent(DatatypeConverter
											.printDecimal(value));

								final JAXBElement<DecField> fieldElement = FACTORY
										.createRowDec(field);

								if (value == null)
									fieldElement.setNil(true);

								row.getFieldElementList().add(fieldElement);
							} else if (Types.INTEGER == type) {
								final IntField field = FACTORY.createIntField();

								field.setName(name);

								field.setContent(DatatypeConverter
										.printInt(dbRs.getInt(j)));

								final JAXBElement<IntField> fieldElement = FACTORY
										.createRowInt(field);

								row.getFieldElementList().add(fieldElement);
							} else if (Types.NUMERIC == type) {
								final NumField field = FACTORY.createNumField();

								field.setName(name);

								final BigDecimal value = dbRs.getBigDecimal(j);

								if (value != null)
									field.setContent(DatatypeConverter
											.printDecimal(value));

								final JAXBElement<NumField> fieldElement = FACTORY
										.createRowNum(field);

								if (value == null)
									fieldElement.setNil(true);

								row.getFieldElementList().add(fieldElement);
							} else if (Types.VARCHAR == type) {
								final StrField field = FACTORY.createStrField();

								field.setName(name);

								final String value = dbRs.getString(j);

								if (value != null)
									field.setContent(DatatypeConverter
											.printString(value));

								final JAXBElement<StrField> fieldElement = FACTORY
										.createRowStr(field);

								if (value == null)
									fieldElement.setNil(true);

								row.getFieldElementList().add(fieldElement);
							}

						}

						rs.getRowList().add(row);
					}

					res.getResult().add(rs);
				}

				if (dbCnt != -1)
					res.getResult().add(dbCnt);

				hasMoreResults = dbStmt.getMoreResults();

				dbCnt = dbStmt.getUpdateCount();
			}

			final StringWriter writer = new StringWriter();

			marshaller.marshal(res, writer);

			writer.close();

			proxy.setPayload(msg, writer.toString());

			return msg;
		} catch (final Exception e) {
			throw new ActionProcessingException(MESSAGE, e);
		} finally {

			if (dbRs != null) {

				try {
					dbRs.close();
				} catch (final SQLException e) {
					throw new ActionProcessingException(MESSAGE, e);
				} finally {

					if (dbStmt != null) {

						try {
							dbStmt.close();
						} catch (final SQLException e) {
							throw new ActionProcessingException(MESSAGE, e);
						} finally {

							if (dbConn != null) {

								try {
									dbConn.close();
								} catch (final SQLException e) {
									throw new ActionProcessingException(
											MESSAGE, e);
								}

							}

						}

					}

				}

			}

			if (dbStmt != null) {

				try {
					dbStmt.close();
				} catch (final SQLException e) {
					throw new ActionProcessingException(MESSAGE, e);
				} finally {

					if (dbConn != null) {

						try {
							dbConn.close();
						} catch (final SQLException e) {
							throw new ActionProcessingException(MESSAGE, e);
						}

					}

				}

			}

			if (dbConn != null) {

				try {
					dbConn.close();
				} catch (final SQLException e) {
					throw new ActionProcessingException(MESSAGE, e);
				}

			}

		}

	}

}