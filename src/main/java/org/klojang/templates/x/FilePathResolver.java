package org.klojang.templates.x;

import org.klojang.templates.PathResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public final class FilePathResolver implements PathResolver {

  @Override
  public boolean isValidPath(String path) {
    return new File(path).isFile();
  }

  @Override
  public InputStream resolve(String path) throws FileNotFoundException {
    return new FileInputStream(path);
  }

  @Override
  public boolean equals(Object obj) {
    return (this == obj || obj instanceof FilePathResolver);
  }

}
