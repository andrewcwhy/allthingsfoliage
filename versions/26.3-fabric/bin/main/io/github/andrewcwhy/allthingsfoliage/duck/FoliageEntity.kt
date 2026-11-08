package io.github.andrewcwhy.allthingsfoliage.duck

/** Per-tick state used by foliage collision handling. */
interface FoliageEntity {
    var foliageSlownessHandled: Boolean
    var partiallyInFoliage: Boolean
}
