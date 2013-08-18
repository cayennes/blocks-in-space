# Blocks in Space

![screenshot illustrating height coloring](doc/colored-heights.png)

This game is heavily based on xblockout and is like tetris with an extra
dimension: blocks move in space rather than in a plane.  Fill up a plane at
the bottom instead of merely a row in order to make it disappear.

However, unique to this game, the rate at which the blocks fall at doesn't
increase.  Instead the complexity of the blocks themselves does.

I am writing this in order to learn clojure while at the same time creating a
game that I'd like to play.  It's in an early development state.

![screenshot illustrating non-flat block](doc/3d-block.png)

## Usage

Requires leiningen 2.  From source directory, start game with

    $ lein run

### Keybindings

Move:

      I
    J K L

Move down (into the screen):

    space

Rotate, turning top towards this direction:

      E
    S D F

Rotate counterclockwise and clockwise, respectively:

    W   R

## Issues

### Known Bugs

* Rather than moving as necessary, blocks simply don't rotate at all if you
  attempt to rotate next to a wall that they would rotate into
* First block has already started falling when the game finishes loading

### Important missing features

* Pause
* Ability to start a new game after losing
* A way to quickly drop all the way instead of just one level
* A color key to block height (at least it's a spectrum unlike xblockout, but
  if there's a situation where there might be no blocks on a certain level it
  can be hard to tell.)
* Show keybindings in-game
* Add more complex shapes indefinitely
* Better code
* Tests (while I don't mind lots of manual testing since the point is that I
  enjoy playing it, eventually I should figure out what kind of tests are best
  for functional programming.)
* Display text with nice looking font/size/position/color
* Stand-alone jar

## License

Copyright © 2013 Cayenne Geis

Distributed under the Eclipse Public License, the same as Clojure.
