# Thin wrappers: every recipe is one command, and everything works without `just`.

# Every suite. Docker is required.
test:
    sbt test

# The living features, the glossary and the specs that name them. Needs uv.
check:
    uvx --from "git+https://github.com/thinkmorestupidless/speckit-bdd@v0.2.0#subdirectory=checker" speckit-bdd check --root . --glossary GLOSSARY.md --features features --specs specs --specs-from 004

hooks:
    git config core.hooksPath .githooks
