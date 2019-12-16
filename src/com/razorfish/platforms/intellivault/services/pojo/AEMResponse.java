package com.razorfish.platforms.intellivault.services.pojo;

import org.json.JSONObject;

public class AEMResponse {
    private String strResponse;
    private int httpCode;
    private JSONObject jsonResponse;
    private boolean success;
    private long executionTime;
    private String path;

    public AEMResponse(final String strResponse, final int httpCode) {
        this.strResponse = strResponse;
        this.httpCode = httpCode;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getStrResponse() {
        return strResponse;
    }

    public void setStrResponse(String strResponse) {
        this.strResponse = strResponse;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public void setHttpCode(int httpCode) {
        this.httpCode = httpCode;
    }

    public JSONObject getJsonResponse() {
        return jsonResponse;
    }

    public void setJsonResponse(JSONObject jsonResponse) {
        this.jsonResponse = jsonResponse;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }
}
