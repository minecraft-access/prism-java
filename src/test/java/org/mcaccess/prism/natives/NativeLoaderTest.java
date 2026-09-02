package org.mcaccess.prism.natives;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NativeLoaderTest {

    @AfterEach
    void clearOverride() {
        System.clearProperty(NativeLoader.NATIVE_DIR_PROPERTY);
    }

    @Test
    void theOverridePropertyWins() {
        System.setProperty(NativeLoader.NATIVE_DIR_PROPERTY, Path.of("build", "custom").toString());
        assertThat(NativeLoader.extractionDir("windows", "amd64")).isEqualTo(Path.of("build", "custom"));
        assertThat(NativeLoader.extractionDir("linux", "aarch64")).isEqualTo(Path.of("build", "custom"));
        assertThat(NativeLoader.extractionDir("mac", "aarch64")).isEqualTo(Path.of("build", "custom"));
    }

    @Test
    void aBlankOverrideIsIgnored() {
        System.setProperty(NativeLoader.NATIVE_DIR_PROPERTY, "   ");
        assertThat(NativeLoader.extractionDir("windows", "amd64"))
                .endsWithRaw(Path.of("prism-java", PrismBuildInfo.PRISM_VERSION, "windows-amd64"));
    }

    @Test
    void theDefaultIsVersionAndPlatformScoped() {
        assertThat(NativeLoader.extractionDir("windows", "amd64"))
                .endsWithRaw(Path.of("prism-java", PrismBuildInfo.PRISM_VERSION, "windows-amd64"));
        assertThat(NativeLoader.extractionDir("linux", "aarch64"))
                .endsWithRaw(Path.of("prism-java", PrismBuildInfo.PRISM_VERSION, "linux-aarch64"));
        assertThat(NativeLoader.extractionDir("mac", "aarch64"))
                .endsWithRaw(Path.of("prism-java", PrismBuildInfo.PRISM_VERSION, "mac-aarch64"));
    }

    @Test
    void eachPlatformResolvesUnderItsOwnCacheRoot() {
        Path home = Path.of(System.getProperty("user.home"));
        assertThat(NativeLoader.extractionDir("mac", "aarch64"))
                .startsWithRaw(home.resolve("Library").resolve("Caches"));

        Path linux = NativeLoader.extractionDir("linux", "amd64");
        String xdg = System.getenv("XDG_CACHE_HOME");
        assertThat(linux).startsWithRaw(xdg != null && !xdg.isBlank() ? Path.of(xdg) : home.resolve(".cache"));

        Path windows = NativeLoader.extractionDir("windows", "amd64");
        String localAppData = System.getenv("LOCALAPPDATA");
        assertThat(windows).startsWithRaw(localAppData != null && !localAppData.isBlank()
                ? Path.of(localAppData)
                : home.resolve("AppData").resolve("Local"));
    }

    @Test
    void platformsDoNotShareADirectory() {
        assertThat(NativeLoader.extractionDir("windows", "amd64"))
                .isNotEqualTo(NativeLoader.extractionDir("windows", "aarch64"))
                .isNotEqualTo(NativeLoader.extractionDir("linux", "amd64"))
                .isNotEqualTo(NativeLoader.extractionDir("mac", "amd64"));
        assertThat(NativeLoader.extractionDir("mac", "amd64"))
                .isNotEqualTo(NativeLoader.extractionDir("mac", "aarch64"));
    }

    @Test
    void theLibraryLoadedAndReportsNoFailure() {
        NativeLoader.load();
        assertThat(NativeLoader.getLoadFailure()).isNull();
    }
}
