package com.example.fitlog.feature.progress;

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
public final class ProgressViewModel_Factory implements Factory<ProgressViewModel> {
  @Override
  public ProgressViewModel get() {
    return newInstance();
  }

  public static ProgressViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ProgressViewModel newInstance() {
    return new ProgressViewModel();
  }

  private static final class InstanceHolder {
    private static final ProgressViewModel_Factory INSTANCE = new ProgressViewModel_Factory();
  }
}
