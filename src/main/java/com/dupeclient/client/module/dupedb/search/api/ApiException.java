package com.dupeclient.client.module.dupedb.search.api;

public final class ApiException extends Exception {
   public final int statusCode;
   public final String body;

   public ApiException(int statusCode, String message, String body) {
      super(message);
      this.statusCode = statusCode;
      this.body = body;
   }

   public boolean isAuthFailure() {
      return this.statusCode == 401 || this.statusCode == 403;
   }
}
