package com.zerog.stellarserverforge.net_port;

import java.io.IOException;
import java.util.List;

/**
 * Checks for a Windows Firewall inbound-allow rule matching the resolved java.exe and the
 * configured port (spec §9.3). Windows-only — a no-op elsewhere.
 */
public class FirewallCheckService {

    public record Result(boolean applicable, boolean pass, String message) {
    }

    public Result check(int port, String javaExePath) {
        if (!isWindows()) {
            return new Result(false, true, "Firewall rule inspection is only available on Windows.");
        }

        String script = "$profile = Get-NetFirewallProfile -Name Private; "
                + "if ($profile.Enabled -eq $false) { Write-Output 'PASS: Private profile disabled'; exit 0 }; "
                + "$rules = Get-NetFirewallRule -Direction Inbound -Action Allow -Enabled True | "
                + "Where-Object { $_.Profile -match 'Private' -or $_.Profile -match 'Any' }; "
                + "foreach ($rule in $rules) { "
                + "  $portFilter = $rule | Get-NetFirewallPortFilter; "
                + "  $appFilter = $rule | Get-NetFirewallApplicationFilter; "
                + "  if ($portFilter.LocalPort -contains '" + port + "' -and $appFilter.Program -eq '"
                + javaExePath.replace("'", "''") + "') { Write-Output 'PASS: matching rule found'; exit 0 } "
                + "}; "
                + "Write-Output 'FAIL: no matching inbound allow rule found for this port and java executable'";

        try {
            Process process = new ProcessBuilder("powershell", "-NoProfile", "-Command", script)
                    .redirectErrorStream(true)
                    .start();
            List<String> lines;
            try (var reader = process.inputReader()) {
                lines = reader.lines().toList();
            }
            process.waitFor();
            String output = String.join(" ", lines).trim();
            boolean pass = output.startsWith("PASS");
            return new Result(true, pass, output.isEmpty() ? "No output from firewall check." : output);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new Result(true, false, "Firewall check failed to run: " + e.getMessage());
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
