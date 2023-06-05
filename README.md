[![Release](https://jitpack.io/v/umjammer/vavi-nio-file-ia.svg)](https://jitpack.io/#umjammer/vavi-nio-file-ia)
[![Java CI](https://github.com/umjammer/vavi-nio-file-ia/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-nio-file-ia/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-nio-file-ia/actions/workflows/codeql.yml/badge.svg)](https://github.com/umjammer/vavi-nio-file-ia/actions/workflows/codeql.yml)
![Java](https://img.shields.io/badge/Java-17-b07219)

# vavi-nio-file-ia

<img src="https://github.com/umjammer/vavi-nio-file-ia/assets/493908/f1fc4e74-aa22-4ed0-8383-1996748a1893" width="120" alt="Internet Archive logo"/>
<sub><a href="https://archive.org/">© Internet Archive</a></sub>

Java nio filesystem for Internet Archive.

this is a fork of https://github.com/experimentaltvcenter/InternetArchive.NET

## References

 * https://archive.org/developers/index.html
 * JSON Patch ([RFC6902](https://datatracker.ietf.org/doc/html/rfc6902))
   * https://github.com/java-json-tools/json-patch ... jaxson base -> this project chooses gson
   * https://github.com/tananaev/json-patch ... gson base -> out of concept (has only comparison)
   * [javax.json](https://javaee.github.io/jsonp/) ... 🎯 but not sophisticated (ignore that bec only for test)