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
import javax.sql.DataSource;
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
}
