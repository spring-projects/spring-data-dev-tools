/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.microbenchmark.jpa;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.microbenchmark.FixtureUtils;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Test fixture for JPA and Spring Data JPA benchmarks.
 *
 * @author Oliver Drotbohm
 */
class JpaFixture {

	private final @Getter ConfigurableApplicationContext context;

	JpaFixture(String database) {

		this.context = FixtureUtils.createContext(JpaApplication.class, "jpa", database);

		withTransactionalEntityManager(em -> {

			IntStream.range(0, FixtureUtils.NUMBER_OF_BOOKS) //
					.mapToObj(it -> new Book(null, "title" + it, it)) //
					.forEach(em::persist);
		});
	}

	private void withTransactionalEntityManager(Consumer<EntityManager> consumer) {

		PlatformTransactionManager manager = context.getBean(PlatformTransactionManager.class);
		TransactionStatus status = manager.getTransaction(new DefaultTransactionDefinition());

		EntityManager em = context.getBean(EntityManager.class);

		consumer.accept(em);

		em.flush();
		manager.commit(status);
		em.close();
	}

	@SpringBootApplication
	static class JpaApplication {

//		@Bean
//		public JpaVendorAdapterPostProcessor oo() {
//			return new JpaVendorAdapterPostProcessor();
//		}
//
//		static class JpaVendorAdapterPostProcessor implements BeanPostProcessor {
//
//			@Override
//			public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
//
//				if (bean instanceof HibernateJpaVendorAdapter) {
//
//					HibernateJpaVendorAdapter adapter = (HibernateJpaVendorAdapter) bean;
//					HibernateJpaDialect dialect = adapter.getJpaDialect();
//					dialect.setPrepareConnection(false);
//				}
//
//				return bean;
//			}
//		}

		@Bean
		DataSource dataSource(DataSourceProperties properties) {

			HikariDataSource readWrite = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
			HikariDataSource readOnly = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();

			ReadOnlyDataSourceRouter readOnlyDataSourceRouter = new ReadOnlyDataSourceRouter(readOnly, readWrite);
			return readOnlyDataSourceRouter;
		}

		static class ReadOnlyDataSourceRouter extends AbstractRoutingDataSource {


			private DelegatingConnection delegatingConnection;

			public ReadOnlyDataSourceRouter(DataSource readOnlyDS, DataSource readWriteDS) {

				Map<Object, Object> dataSources = new LinkedHashMap<>();
				dataSources.put("read-write", readWriteDS);
				dataSources.put("read-only", readOnlyDS);
				setTargetDataSources(dataSources);

				delegatingConnection = new DelegatingConnection(readOnlyDS, readWriteDS);
			}

			/**
			 * Supports Integer values for the isolation level constants
			 * as well as isolation level names as defined on the
			 * {@link org.springframework.transaction.TransactionDefinition TransactionDefinition interface}.
			 */
			@Override
			protected Object resolveSpecifiedLookupKey(Object lookupKey) {

				if (lookupKey instanceof String) {
					return lookupKey;
				}

				if (lookupKey instanceof Boolean) {
					return ((Boolean) lookupKey) ? "read-only" : "read-write";
				} else {
					throw new IllegalArgumentException(
							"Invalid lookup key: " + lookupKey);
				}
			}

			@Override
			@Nullable
			protected Object determineCurrentLookupKey() {
				return TransactionSynchronizationManager.isCurrentTransactionReadOnly() ? "read-only" : "read-write";
			}

			@Override
			public Connection getConnection() throws SQLException {
				return delegatingConnection;
			}
		}

		static class DelegatingConnection implements Connection {

			Connection delegate;
			private DataSource readOnlyDS;
			private DataSource readWriteDS;

			public DelegatingConnection(DataSource readOnlyDS, DataSource readWriteDS) {

				this.readOnlyDS = readOnlyDS;
				this.readWriteDS = readWriteDS;
			}

			public DelegatingConnection(Connection delegate) {
				this.delegate = delegate;
			}

			@Override
			public Statement createStatement() throws SQLException {
				return getConnection(false).createStatement();
			}

			@Override
			public PreparedStatement prepareStatement(String sql) throws SQLException {
				return getConnection(false).prepareStatement(sql);
			}

			@Override
			public CallableStatement prepareCall(String sql) throws SQLException {
				return getConnection(false).prepareCall(sql);
			}

			@Override
			public String nativeSQL(String sql) throws SQLException {
				return getConnection(false).nativeSQL(sql);
			}

			@Override
			public void setAutoCommit(boolean autoCommit) throws SQLException {
				getConnection(false).setAutoCommit(autoCommit);
			}

			@Override
			public boolean getAutoCommit() throws SQLException {
				return getConnection(false).getAutoCommit();
			}

			@Override
			public void commit() throws SQLException {
				getConnection(false).commit();
			}

			@Override
			public void rollback() throws SQLException {
				getConnection(false).rollback();
			}

			@Override
			public void close() throws SQLException {
				if (!isReadOnly()) {
					getConnection(isReadOnly()).close();
					delegate = null;
				}
			}

			@Override
			public boolean isClosed() throws SQLException {
				return getConnection(false).isClosed();
			}

			@Override
			public DatabaseMetaData getMetaData() throws SQLException {
				return getConnection(false).getMetaData();
			}

			@Override
			public void setReadOnly(boolean readOnly) throws SQLException {

				if (readOnly) {
					if (delegate == null) {
						delegate = getConnection(readOnly);
						delegate.setReadOnly(true);
					}
				}
			}

			@Override
			public boolean isReadOnly() throws SQLException {
				return getConnection(false).isReadOnly();
			}

			@Override
			public void setCatalog(String catalog) throws SQLException {
				getConnection(false).setCatalog(catalog);
			}

			@Override
			public String getCatalog() throws SQLException {
				return getConnection(false).getCatalog();
			}

			@Override
			public void setTransactionIsolation(int level) throws SQLException {
				getConnection(false).setTransactionIsolation(level);
			}

			@Override
			public int getTransactionIsolation() throws SQLException {
				return getConnection(false).getTransactionIsolation();
			}

			@Override
			public SQLWarning getWarnings() throws SQLException {
				return getConnection(false).getWarnings();
			}

			@Override
			public void clearWarnings() throws SQLException {
				getConnection(false).clearWarnings();
			}

			@Override
			public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
				return getConnection(false).createStatement(resultSetType, resultSetConcurrency);
			}

			@Override
			public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
				return getConnection(false).prepareStatement(sql, resultSetType, resultSetConcurrency);
			}

			@Override
			public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
				return getConnection(false).prepareCall(sql, resultSetType, resultSetConcurrency);
			}

			@Override
			public Map<String, Class<?>> getTypeMap() throws SQLException {
				return getConnection(false).getTypeMap();
			}

			@Override
			public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
				getConnection(false).setTypeMap(map);
			}

			@Override
			public void setHoldability(int holdability) throws SQLException {
				getConnection(false).setHoldability(holdability);
			}

			@Override
			public int getHoldability() throws SQLException {
				return getConnection(false).getHoldability();
			}

			@Override
			public Savepoint setSavepoint() throws SQLException {
				return getConnection(false).setSavepoint();
			}

			@Override
			public Savepoint setSavepoint(String name) throws SQLException {
				return getConnection(false).setSavepoint(name);
			}

			@Override
			public void rollback(Savepoint savepoint) throws SQLException {
				getConnection(false).rollback(savepoint);
			}

			@Override
			public void releaseSavepoint(Savepoint savepoint) throws SQLException {
				getConnection(false).releaseSavepoint(savepoint);
			}

			@Override
			public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
				return getConnection(false).createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
			}

			@Override
			public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
				return getConnection(false).prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
			}

			@Override
			public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
				return getConnection(false).prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
			}

			@Override
			public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
				return getConnection(false).prepareStatement(sql, autoGeneratedKeys);
			}

			@Override
			public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
				return getConnection(false).prepareStatement(sql, columnIndexes);
			}

			@Override
			public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
				return getConnection(false).prepareStatement(sql, columnNames);
			}

			@Override
			public Clob createClob() throws SQLException {
				return getConnection(false).createClob();
			}

			@Override
			public Blob createBlob() throws SQLException {
				return getConnection(false).createBlob();
			}

			@Override
			public NClob createNClob() throws SQLException {
				return getConnection(false).createNClob();
			}

			@Override
			public SQLXML createSQLXML() throws SQLException {
				return getConnection(false).createSQLXML();
			}

			@Override
			public boolean isValid(int timeout) throws SQLException {
				return getConnection(false).isValid(timeout);
			}

			@Override
			public void setClientInfo(String name, String value) throws SQLClientInfoException {
				try {
					getConnection(false).setClientInfo(name, value);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void setClientInfo(Properties properties) throws SQLClientInfoException {
				try {
					getConnection(false).setClientInfo(properties);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

			@Override
			public String getClientInfo(String name) throws SQLException {
				return getConnection(false).getClientInfo(name);
			}

			@Override
			public Properties getClientInfo() throws SQLException {
				return getConnection(false).getClientInfo();
			}

			@Override
			public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
				return getConnection(false).createArrayOf(typeName, elements);
			}

			@Override
			public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
				return getConnection(false).createStruct(typeName, attributes);
			}

			@Override
			public void setSchema(String schema) throws SQLException {
				getConnection(false).setSchema(schema);
			}

			@Override
			public String getSchema() throws SQLException {
				return getConnection(false).getSchema();
			}

			@Override
			public void abort(Executor executor) throws SQLException {
				getConnection(false).abort(executor);
			}

			@Override
			public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
				getConnection(false).setNetworkTimeout(executor, milliseconds);
			}

			@Override
			public int getNetworkTimeout() throws SQLException {
				return getConnection(false).getNetworkTimeout();
			}

			@Override
			public <T> T unwrap(Class<T> iface) throws SQLException {
				return getConnection(false).unwrap(iface);
			}

			@Override
			public boolean isWrapperFor(Class<?> iface) throws SQLException {
				return getConnection(false).isWrapperFor(iface);
			}

			Connection getConnection(boolean readOnly) throws SQLException {
				if (delegate == null) {
					DataSource dataSource = (readOnly ? readOnlyDS : readWriteDS);
					delegate = dataSource.getConnection();
				}
				return delegate;
			}
		}
	}
}
