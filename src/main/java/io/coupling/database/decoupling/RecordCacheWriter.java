package io.coupling.database.decoupling;

import java.util.Collection;
import javax.cache.Cache.Entry;
import javax.cache.integration.CacheWriter;
import javax.cache.integration.CacheWriterException;

public class RecordCacheWriter implements CacheWriter<Long, Record> {

  @Override
  public void write(Entry<? extends Long, ? extends Record> entry) throws CacheWriterException {
  }

  @Override
  public void writeAll(Collection<Entry<? extends Long, ? extends Record>> entries)
      throws CacheWriterException {
  }

  @Override
  public void delete(Object key) throws CacheWriterException {
  }

  @Override
  public void deleteAll(Collection<?> keys) throws CacheWriterException {
  }
}
