package com.example.fitlog.feature.record;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class RecordViewModel_Factory implements Factory<RecordViewModel> {
  @Override
  public RecordViewModel get() {
    return newInstance();
  }

  public static RecordViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RecordViewModel newInstance() {
    return new RecordViewModel();
  }

  private static final class InstanceHolder {
    private static final RecordViewModel_Factory INSTANCE = new RecordViewModel_Factory();
  }
}
