package com.pandau.tools.pdftool.model;

public class AjaxResult<T> {
    private boolean success;

    private String message;

    private T data;

    public boolean isSuccess() {
        return this.success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public void ajaxFalse(String message) {
        this.message = message;
        this.success = false;
    }

    public void ajaxTrue(T data) {
        this.data = data;
        this.success = true;
    }

    public AjaxResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AjaxResult() {}

    public AjaxResult(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
}
