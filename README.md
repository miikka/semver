# semver

**Status:** Experiment, not usable. This is a playground project for me!

Clojure library for parsing and evaluating [semver](https://github.com/npm/node-semver) ranges.

## Usage

The main entry point is the function `semver.core/satisfies?,
which takes a version string and a semver range string and returns true if the version is included in the range.

```clojure
(require '[semver.core :as semver])

(semver/satisfies? "1.2.3" "^1.2.3") ; => true
(semver/satisfies? "1.3.0" "^1.2.3") ; => true
(semver/satisfies? "2.0.0" "^1.2.3") ; => false
```

## To do list

For the first release:

- [x] Support multiple ranges `1.2.3 || 4.5.6`
- [ ] Support hyphen ranges `1.2.3 - 1.2.4`
- [ ] Support X ranges `1.2.X`
- [x] Support tilde ranges `~1.2.3`
- [ ] Support pre-release versions `1.2.3-alpha1`
- [x] Set up a CI job for running tests
- [ ] Support ClojureScript

Possibly later:

- [ ] Deal with version numbers that do not have three main components `1.2`, `1.2.3.4` etc.
- [ ] Support comparing and/or sorting releases

## Development

- We use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) for commit messages.
- There's a [devcontainer configuration](https://code.visualstudio.com/docs/remote/containers) which you should be able to use with VS Code or GitHub Codespaces.

## License

Copyright © 2022 Miikka Koskinen.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
