package com.example.fitlog.feature.plan;

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
public final class PlanViewModel_Factory implements Factory<PlanViewModel> {
  @Override
  public PlanViewModel get() {
    return newInstance();
  }

  public static PlanViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PlanViewModel newInstance() {
    return new PlanViewModel();
  }

  private static final class InstanceHolder {
    private static final PlanViewModel_Factory INSTANCE = new PlanViewModel_Factory();
  }
}
