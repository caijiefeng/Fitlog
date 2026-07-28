package com.example.fitlog.feature.today;

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
public final class TodayViewModel_Factory implements Factory<TodayViewModel> {
  @Override
  public TodayViewModel get() {
    return newInstance();
  }

  public static TodayViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TodayViewModel newInstance() {
    return new TodayViewModel();
  }

  private static final class InstanceHolder {
    private static final TodayViewModel_Factory INSTANCE = new TodayViewModel_Factory();
  }
}
