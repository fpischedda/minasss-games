# minasss-games

Experimenting with ClojureScript and [PIXIjs](https://www.pixijs.com) and having fun.

First "real" game developed with this "thing" is the [Entry](https://minasss.itch.io/awwwliens) for the [Autumn Lisp Game Jam 2019](https://itch.io/jam/autumn-lisp-game-jam-2019).
The code for the entry can be found in [its branch](https://github.com/fpischedda/minasss-games/tree/lisp-gamejam-oct-2019).
Graphics and game design by [Camilla Cali](https://www.instagram.com/camilla_cali/).
Code and game design by [me](https://github.com/fpischedda/)


## Overview

FIXME: Write a paragraph about the library/project and highlight its goals.

## Setup

To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL.

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
