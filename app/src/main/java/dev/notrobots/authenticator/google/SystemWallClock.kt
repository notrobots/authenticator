package dev.notrobots.authenticator.google

class SystemWallClock : Clock {
    override fun nowMillis(): Long {
        return System.currentTimeMillis()
    }
}
