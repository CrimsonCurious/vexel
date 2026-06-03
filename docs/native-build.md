# native.build

Controls native library compilation.

Example:

```toml
[lib:native]

src = cpp/src

include = cpp/include

cpp = true

link = android log

flags = -O2 -Wall -s
```

## Multiple Libraries

```toml
[lib:core]
src = cpp/core

include = cpp/include

cpp = true

link = log

flags = -O2 -s


[lib:renderer]
src = cpp/render

include = cpp/include

cpp = true

link = android EGL GLESv3

flags = -O2 -s
```

## Fields

### src

Source directory, wildcard, or file list.

Examples:

```toml
src = cpp/src
```

```toml
src = cpp/src/*.cpp
```

```toml
src = cpp/Main.cpp cpp/Helper.cpp
```

### include

Additional include directories.

### cpp

Use C++ compiler.

### link

Libraries to link.

### flags

Additional compiler/linker flags.