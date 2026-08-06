package za.ac.cput.guiltfreecookie.dto;

public class AuthResponse {
    private String token;

    protected AuthResponse() {}

    private AuthResponse(Builder builder) {
        this.token = builder.token;
    }

    public String getToken() {
        return token;
    }

    public static class Builder {
        private String token;

        public Builder setToken(String token) {
            this.token = token;
            return this;
        }

        public Builder copy(AuthResponse authResponse) {
            this.token = authResponse.getToken();
            return this;
        }

        public AuthResponse build() {return new AuthResponse(this);}
    }
}
