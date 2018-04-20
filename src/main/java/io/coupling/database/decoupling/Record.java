package io.coupling.database.decoupling;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;

class Record {

  private final Long id;
  private final String data;

  Record(final Long id, final String data) {
    this.id = id;
    this.data = data;
  }

  Map<String, Object> toMap() {
    return ImmutableMap.<String, Object>builder()
        .put("id", id)
        .put("data", data)
        .build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Record record = (Record) o;
    return Objects.equals(id, record.id) &&
        Objects.equals(data, record.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, data);
  }
}
