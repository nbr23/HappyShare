package fr.catch23.happyshare;

public class ShareException extends Exception {
    private String user_message = "";


    public ShareException(String user_message)
    {
        this.user_message = user_message;
    }

    public String getUser_message()
    {
        return user_message;
    }
}
