package io.coupling.database.decoupling;

import com.google.common.collect.ImmutableMap;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class CoupledModule {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  CoupledModule(final NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  void save(final Record record) {
    jdbcTemplate.update("INSERT INTO records (id, data) VALUES (:id, :data)", record.toMap());
  }

  Record query(final Long id) {
    return jdbcTemplate.queryForObject("SELECT data FROM records WHERE id=:id",
        ImmutableMap.of("id", id), (rs, rowNum) -> extractRecord(id, rs));
  }

  private Record extractRecord(final Long id, final ResultSet rs) throws SQLException {
    final String data = rs.getString("data");
    return new Record(id, data);
  }
}
