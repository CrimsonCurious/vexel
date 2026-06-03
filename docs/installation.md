# Installation

## Requirements

- Android 5.0+
- ARM64 device recommended
- Termux recommended
- Approximately 530MB free storage (110MB + 520MB)

## Install Vexel Base

Run setup:

```sh
sh setup.sh
```

Verify:

```sh
vexel checkup
```

## Install Native Plugin

Run:

```sh
sh setup-ndk.sh
```

## Supported Terminals

Official:

- Termux

Experimental:

- Pydroid
- Cxxdroid
- Other Android terminal environments

## Updating

Remove old installation:

```sh
rm -rf ~/.vexel
```

Install the newer release.