//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.net

open class ConnectionStep(open val stepText: String) {
    open fun waitForEnd() {
        return
    }
}