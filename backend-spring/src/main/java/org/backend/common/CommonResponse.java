package org.backend.common;


import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse<T> {

    private final String status;
    private final T data;
    private final String message;

    private CommonResponse(String status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> CommonResponse<T> success(T data, String message) {
        return new CommonResponse<>("success", data, message);
    }

    public static <T> CommonResponse<T> fail(String message) {
        return new CommonResponse<>("FAIL", null, message);
    }

    public static <T> CommonResponse<T> error(String message) {
        return new CommonResponse<>("error", null, message);
    }

    public String getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
