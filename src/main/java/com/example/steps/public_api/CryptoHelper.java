package com.example.steps.public_api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Blocking
class CryptoHelper {
  static String publicKey() throws IOException {
    return read("public_key.pem");
  }

  static String privateKey() throws IOException {
    return read("private_key.pem");
  }

  private static String read(String file) throws IOException {
    Path path = Paths.get("public-api", file);

    if (!path.toFile().exists()) {
      path = Paths.get("..", "public-api", file);
    }

    // original code is split into modules.
    if (!path.toFile().exists()) {
      path = Paths.get(".", file);
    }

    // TODO: consider changing into async (just the reading).
    // TODO: could move the entire reading into worker thread.
    return String.join("\n", Files.readAllLines(path, StandardCharsets.UTF_8));
  }
}
