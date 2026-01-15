package fr.flwrian.Result;

public class GameResult {
    int id;
    String result;
    String reason;

    public GameResult(int id, String result, String reason) {
        this.id = id;
        this.result = result;
        this.reason = reason;
    }

    public int getId() {
        return id;
    }

    public String getReason() {
        return reason;
    }

    public String getResult() {
        return result;
    }
}
