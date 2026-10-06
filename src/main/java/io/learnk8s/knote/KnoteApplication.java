package io.learnk8s.knote;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;

@SpringBootApplication
@EnableConfigurationProperties(KnoteProperties.class)
public class KnoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnoteApplication.class, args);
    }
}

interface NotesRepository extends MongoRepository<Note, String> {
}

@Document(collection = "notes")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
class Note {
    @Id
    private String id;
    private String description;

    @Override
    public String toString() {
        return description;
    }
}

@ConfigurationProperties(prefix = "knote")
class KnoteProperties {

    @Value("${minio.host:localhost}")
    private String minioHost;

    @Value("${minio.bucket:image-storage}")
    private String minioBucket;

    @Value("${minio.access.key:}")
    private String minioAccessKey;

    @Value("${minio.secret.key:}")
    private String minioSecretKey;

    @Value("${minio.reconnect.enabled:true}")
    private boolean minioReconnectEnabled;

    public String getMinioHost() { return minioHost; }
    public String getMinioBucket() { return minioBucket; }
    public String getMinioAccessKey() { return minioAccessKey; }
    public String getMinioSecretKey() { return minioSecretKey; }
    public boolean isMinioReconnectEnabled() { return minioReconnectEnabled; }
}