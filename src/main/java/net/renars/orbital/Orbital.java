package net.renars.orbital;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Orbital {
    public static final String ID = "Orbital";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);

    static void main(String[] args) {
        loadEnv();
        SpringApplication.run(Orbital.class, args);
    }

    static void loadEnv() {
        Dotenv dotenv = Dotenv.load();
        System.setProperty("spring.cloud.aws.credentials.access-key", dotenv.get("AWS_ACCESS_KEY"));
        System.setProperty("spring.cloud.aws.credentials.secret-key", dotenv.get("AWS_SECRET_KEY"));
    }
}
