# Local Maven Repository

This directory contains Maven-layout launcher-local artifacts that were previously wired with Gradle `files(...)` dependencies.

Gradle resolves them through:

```groovy
maven { url = uri('local-maven') }
```

Shared engine-side binary artifacts are resolved from `../KaylasUIEngine/local-maven`.
