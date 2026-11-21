package vn.ptit.tour.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.ptit.tour.repository.AppUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DataInitializerProfileTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(DataInitializer.class)
            .withBean(AppUserRepository.class, () -> mock(AppUserRepository.class))
            .withBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class))
            .withPropertyValues("app.demo.password=synthetic-profile-test-only");

    @Test
    void passwordAloneCannotEnableDemoUsersInProduction() {
        context.withInitializer(c -> c.getEnvironment().setActiveProfiles("production"))
                .run(c -> assertThat(c).doesNotHaveBean("seedUsers"));
    }

    @Test
    void passwordAloneCannotEnableDemoUsersWithoutAnExplicitProfile() {
        context.run(c -> assertThat(c).doesNotHaveBean("seedUsers"));
    }

    @Test
    void explicitLocalDemoProfileCanSeedUsers() {
        context.withInitializer(c -> c.getEnvironment().setActiveProfiles("demo"))
                .run(c -> assertThat(c).hasBean("seedUsers"));
    }
}
