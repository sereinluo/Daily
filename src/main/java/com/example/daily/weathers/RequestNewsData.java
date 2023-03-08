package com.example.daily.weathers;

public class RequestNewsData {
    private String msg;
    private NewsFirstResult result;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public NewsFirstResult getResult() {
        return result;
    }

    public void setResult(NewsFirstResult result) {
        this.result = result;
    }
}
