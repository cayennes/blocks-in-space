# blocks-in-space

This game is heaveily based on xblockout and is like tetris with an extra
dimension: blocks move in space rather than in a plane.  Fill up a plane at
the bottom instead of merely a row in order to make it disappear.

However, unique to this game, the rate at which the blocks fall at doesn't
increase.  Instead the complexity of the blocks themselves does.

I am writing this in order to learn clojure while at the same time creating a
game that I'd like to play.  It's in an early development state.

## Usage

Keybindings:

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

## Known Bugs

* When you lose, the game crashes
* Rather than moving as necessary, blocks simply don't rotate at all if you
  attempt to rotate next to a wall that they would rotate into
* For just a couple shapes, it's possible to rotate part of the block over the
  top of the walls.  If you do the game will crash when the block tries to
  fall there.
* First block has already started falling when the game finishes loading

Important missing features:

* Pause
* Not only should the game not crash when you lose, it should let you start a
  new one
* A color key to block height (at least it's a spectrum unlike xblockout, but
  if there's a situation where there might be no blocks on a certain level it
  can be hard to tell.)
* Show keybindings in-game
* Add more complex shapes indefinitely
* Better code

## License

Copyright © 2013 Cayenne Geis

Distributed under the Eclipse Public License, the same as Clojure.