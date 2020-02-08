package fr.catch23.happyshare

class ShareException(user_message: String, e: Exception = Exception()) : Exception() {
    var user_message: String
    val parent: Exception

    init {
        this.user_message = user_message
        this.parent = e
    }
}
