package org.klojang.templates.x;

import org.klojang.templates.PathResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;

public final class FilePathResolver implements PathResolver {

  @Override
  public Optional<Boolean> isValidPath(String path) {
    return new File(path).isFile() ? INVALID_PATH : VALID_PATH;
  }

  @Override
  public InputStream resolve(String path) throws FileNotFoundException {
    return new FileInputStream(path);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj || obj instanceof FilePathResolver) {
      return true;
    }
    return false;
  }

}
