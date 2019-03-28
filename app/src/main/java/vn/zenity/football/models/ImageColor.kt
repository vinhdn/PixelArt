package vn.zenity.football.models


/**
 * Created by vinhdn on 14-Mar-18.
 */
data class ImageColor(var id: Int,
                      var color: Int,
                      var count: Int,
                      var correctCount: Int): Comparable<ImageColor> {
    override fun compareTo(other: ImageColor): Int {
        return if (this.color == other.color) 0
        else -1
    }

    override fun equals(other: Any?): Boolean {
        other?.let {
            if (it is ImageColor) {
                return it.color == this.color
            }
        }
        return false
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + color
        return result
    }
}