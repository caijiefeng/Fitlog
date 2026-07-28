package com.example.fitlog.domain.example;

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
public final class GetExampleUseCase_Factory implements Factory<GetExampleUseCase> {
  @Override
  public GetExampleUseCase get() {
    return newInstance();
  }

  public static GetExampleUseCase_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static GetExampleUseCase newInstance() {
    return new GetExampleUseCase();
  }

  private static final class InstanceHolder {
    private static final GetExampleUseCase_Factory INSTANCE = new GetExampleUseCase_Factory();
  }
}
