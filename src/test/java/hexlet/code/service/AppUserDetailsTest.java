package hexlet.code.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AppUserDetailsTest {

    @Test
    void toStringDoesNotLeakPasswordDigest() {
        var details = new AppUserDetails(1L, "a@b.c", "secret");

        assertThat(details.toString())
                .contains("AppUserDetails")
                .contains("id=1")
                .contains("email=a@b.c")
                .doesNotContain("secret")
                .doesNotContain("passwordDigest");
    }
}
