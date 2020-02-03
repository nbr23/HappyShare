package fr.catch23.happyshare

class ShareException(user_message: String) : Exception() {
    var user_message = ""


    init {
        this.user_message = user_message
    }
}
