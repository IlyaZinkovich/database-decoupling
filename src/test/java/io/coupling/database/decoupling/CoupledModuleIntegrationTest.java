package io.coupling.database.decoupling;

import static com.wix.mysql.EmbeddedMysql.anEmbeddedMysql;
import static com.wix.mysql.ScriptResolver.classPathScript;
import static com.wix.mysql.config.Charset.UTF8;
import static com.wix.mysql.config.MysqldConfig.aMysqldConfig;
import static com.wix.mysql.distribution.Version.v5_7_latest;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wix.mysql.EmbeddedMysql;
import com.wix.mysql.config.MysqldConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.cache.configuration.FactoryBuilder;
import javax.sql.DataSource;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class CoupledModuleIntegrationTest {

  private static final MysqldConfig MYSQL_CONFIG = aMysqldConfig(v5_7_latest)
      .withCharset(UTF8)
      .withPort(3306)
      .withUser("user", "pass")
      .build();

  @Test
  void testOptimisticFlow() {
    final EmbeddedMysql mysql = startMySQL();
    final DataSource rawDataSource = createRawDataSource(mysql.getConfig().getPort());
    final NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(rawDataSource);

    final CoupledModule coupledModule = new CoupledModule(jdbcTemplate);

    final long recordId = 1L;
    final String recordData = "data";
    final Record record = new Record(recordId, recordData);

    coupledModule.save(record);

    assertEquals(record, coupledModule.query(recordId));
  }

  @Test
  void testPessimisticFlow() {
    final EmbeddedMysql mysql = startMySQL();
    final DataSource rawDataSource = createRawDataSource(mysql.getConfig().getPort());
    final NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(rawDataSource);

    final CoupledModule coupledModule = new CoupledModule(jdbcTemplate);

    final long recordId = 1L;
    final String recordData = "data";
    final Record record = new Record(recordId, recordData);

    coupledModule.save(record);

    mysql.stop();

    assertThrows(CannotGetJdbcConnectionException.class, () -> coupledModule.query(recordId));
  }

  private EmbeddedMysql startMySQL() {
    return anEmbeddedMysql(MYSQL_CONFIG)
        .addSchema("data", classPathScript("schema.sql")).start();
  }

  private DataSource createRawDataSource(final int mysqlPort) {
    HikariConfig jdbcConfig = new HikariConfig();
    jdbcConfig.setMaximumPoolSize(1);
    jdbcConfig.setConnectionTimeout(1000);
    jdbcConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
    jdbcConfig.setJdbcUrl(format("jdbc:mysql://localhost:%d/data?useSSL=false", mysqlPort));
    jdbcConfig.setUsername("user");
    jdbcConfig.setPassword("pass");
    return new HikariDataSource(jdbcConfig);
  }

  @Test
  void testDataGridBackedWithDatabase() {
    IgniteConfiguration igniteConfiguration = new IgniteConfiguration();
    CacheConfiguration<Long, Record> cacheCfg = new CacheConfiguration<>();
    cacheCfg.setName("records");
    cacheCfg.setCacheWriterFactory(FactoryBuilder.factoryOf(RecordCacheWriter.class));
    cacheCfg.setWriteThrough(true);
    cacheCfg.setWriteBehindEnabled(true);
    igniteConfiguration.setCacheConfiguration(cacheCfg);
    try (final Ignite ignite = Ignition.start(igniteConfiguration)) {
      final IgniteCache<Long, Record> recordsCache = ignite.cache("records");

    }
  }
}
