package com.dupeclient;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Runs before the game and before SLF4J is useful for early hangs. Writes to {@code System.err},
 * {@code java.io.tmpdir/dupeclient-bootstrap.log}, and the instance {@code logs/dupeclient-bootstrap.log}.
 */
public final class DupeClientPreLaunch implements PreLaunchEntrypoint {
	static {
		String cwd = ".";
		try {
			cwd = Path.of("").toAbsolutePath().toString();
		} catch (Throwable ignored) {
		}
		logEarly("STATIC", "DupeClientPreLaunch class loaded | cwd=" + cwd);
	}

	@Override
	public void onPreLaunch() {
		logEarly("PRELAUNCH", "onPreLaunch() start");
		try {
			Path gd = FabricLoader.getInstance().getGameDir();
			logEarly("PRELAUNCH", "Fabric gameDir=" + gd.toAbsolutePath());
		} catch (Throwable t) {
			logEarly("PRELAUNCH", "FabricLoader.getGameDir() failed: " + t);
		}
		logEarly("PRELAUNCH", "onPreLaunch() end");
	}

	private static void logEarly(String phase, String message) {
		String line = "[" + Instant.now() + "] [" + phase + "] " + message
				+ " | java=" + Runtime.version()
				+ " | thread=" + Thread.currentThread().getName();
		try {
			System.err.println("[DupeClient] " + line);
			System.err.flush();
		} catch (Throwable ignored) {
		}
		try {
			Path tmp = Path.of(System.getProperty("java.io.tmpdir", ".")).resolve("dupeclient-bootstrap.log");
			Files.writeString(tmp, line + System.lineSeparator(), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (Throwable ignored) {
		}
		try {
			Path gameDir = FabricLoader.getInstance().getGameDir();
			Path logFile = gameDir.resolve("logs").resolve("dupeclient-bootstrap.log");
			Files.createDirectories(logFile.getParent());
			Files.writeString(logFile, line + System.lineSeparator(), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (Throwable ignored) {
		}
	}
}
