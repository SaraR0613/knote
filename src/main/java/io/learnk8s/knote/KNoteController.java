package io.learnk8s.knote;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.apache.commons.io.IOUtils;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
public class KNoteController {

    @Autowired
    private NotesRepository notesRepository;

    @Autowired
    private KnoteProperties properties;

    private MinioClient minioClient;
    private Parser parser = Parser.builder().build();
    private HtmlRenderer renderer = HtmlRenderer.builder().build();

    @PostConstruct
    public void init() throws InterruptedException {
        initMinio();
    }

    private void initMinio() throws InterruptedException {
        boolean success = false;
        while (!success) {
            try {
                minioClient = MinioClient.builder()
                        .endpoint("http://" + properties.getMinioHost() + ":9000")
                        .credentials(properties.getMinioAccessKey(), properties.getMinioSecretKey())
                        .build();

                boolean isExist = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(properties.getMinioBucket()).build()
                );
                if (isExist) {
                    System.out.println("> Bucket already exists.");
                } else {
                    minioClient.makeBucket(
                            MakeBucketArgs.builder().bucket(properties.getMinioBucket()).build()
                    );
                    System.out.println("> Bucket created.");
                }
                success = true;
            } catch (Exception e) {
                e.printStackTrace();
                if (properties.isMinioReconnectEnabled()) {
                    System.out.println("> Minio Reconnect in 5s...");
                    Thread.sleep(5000);
                } else {
                    success = true;
                }
            }
        }
        System.out.println("> Minio initialized!");
    }

    @GetMapping("/")
    public String index(Model model) {
        getAllNotes(model);
        return "index";
    }

    @PostMapping("/note")
    public String saveNotes(@RequestParam("image") MultipartFile file,
                            @RequestParam String description,
                            @RequestParam(required = false) String publish,
                            @RequestParam(required = false) String upload,
                            Model model) throws Exception {

        if (publish != null && publish.equals("Publish")) {
            saveNote(description, model);
            getAllNotes(model);
            return "redirect:/";
        }
        if (upload != null && upload.equals("Upload")) {
            if (file != null && file.getOriginalFilename() != null &&
                    !file.getOriginalFilename().isEmpty()) {
                uploadImage(file, description, model);
            }
            getAllNotes(model);
            return "index";
        }
        return "index";
    }

    private void getAllNotes(Model model) {
        List<Note> notes = notesRepository.findAll();
        Collections.reverse(notes);
        model.addAttribute("notes", notes);
    }

    private void uploadImage(MultipartFile file, String description, Model model) throws Exception {
        String fileId = UUID.randomUUID().toString() + file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'));
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(properties.getMinioBucket())
                        .object(fileId)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        model.addAttribute("description",
                description + " ![](/img/" + fileId + ")");
    }

    private void saveNote(String description, Model model) {
        if (description != null && !description.trim().isEmpty()) {
            Node document = parser.parse(description.trim());
            String html = renderer.render(document);
            notesRepository.save(new Note(null, html));
            model.addAttribute("description", "");
        }
    }

    @GetMapping(value = "/img/{name}", produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] getImageByName(@PathVariable String name) throws Exception {
        InputStream imageStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(properties.getMinioBucket())
                        .object(name)
                        .build()
        );
        return IOUtils.toByteArray(imageStream);
    }
}
