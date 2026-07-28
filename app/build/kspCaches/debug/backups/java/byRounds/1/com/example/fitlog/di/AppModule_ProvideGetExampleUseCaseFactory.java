package com.example.fitlog.di;

import com.example.fitlog.domain.example.GetExampleUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvideGetExampleUseCaseFactory implements Factory<GetExampleUseCase> {
  @Override
  public GetExampleUseCase get() {
    return provideGetExampleUseCase();
  }

  public static AppModule_ProvideGetExampleUseCaseFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static GetExampleUseCase provideGetExampleUseCase() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGetExampleUseCase());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideGetExampleUseCaseFactory INSTANCE = new AppModule_ProvideGetExampleUseCaseFactory();
  }
}
