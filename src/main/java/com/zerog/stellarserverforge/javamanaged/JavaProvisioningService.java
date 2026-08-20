package com.zerog.stellarserverforge.javamanaged;

import com.zerog.stellarserverforge.model.JavaOverrideMode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Resolves which {@code java} executable to launch the server with, honoring the three
 * override modes from spec §7.2.
 */
public class JavaProvisioningService {

    private final AdoptiumProvisioner adoptium;

    public JavaProvisioningService(Path cacheDir) {
        this.adoptium = new AdoptiumProvisioner(cacheDir);
    }

    /** The resolved java launch command — either an absolute path or the literal "java" (PATH mode). */
    public record ResolvedJava(String command, String source) {
    }

    public ResolvedJava resolve(int majorVersion, JavaOverrideMode mode) throws IOException, InterruptedException {
        if (mode == JavaOverrideMode.SYSTEM_PATH) {
            return new ResolvedJava("java", "System PATH");
        }

        if (mode == JavaOverrideMode.AUTOMATIC) {
            Optional<Path> system = SystemJavaDetector.findJavaExecutable(majorVersion);
            if (system.isPresent()) {
                return new ResolvedJava(system.get().toAbsolutePath().toString(), "System install");
            }
        }

        Path managed = adoptium.ensureProvisioned(majorVersion);
        return new ResolvedJava(managed.toAbsolutePath().toString(), "Managed (Adoptium)");
    }
}
