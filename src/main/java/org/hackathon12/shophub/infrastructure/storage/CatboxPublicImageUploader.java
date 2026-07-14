package org.hackathon12.shophub.infrastructure.storage;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class CatboxPublicImageUploader implements PublicImageUploadPort {

    private static final String UPLOAD_URL = "https://catbox.moe/user/api.php";

    private final RestClient restClient;

    public CatboxPublicImageUploader(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String upload(MultipartFile image) {
        try {
            String filename = StringUtils.hasText(image.getOriginalFilename())
                    ? image.getOriginalFilename()
                    : "instagram.jpg";

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("reqtype", "fileupload");
            body.add("fileToUpload", new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });

            String response = restClient.post()
                    .uri(UPLOAD_URL)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (!StringUtils.hasText(response) || !response.startsWith("http")) {
                throw new IllegalArgumentException("공개 이미지 URL 생성에 실패했습니다.");
            }
            return response.trim();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Instagram 게시용 이미지 업로드에 실패했습니다.", exception);
        } catch (Exception exception) {
            throw new IllegalArgumentException("공개 이미지 호스팅 업로드에 실패했습니다: " + exception.getMessage(), exception);
        }
    }
}
