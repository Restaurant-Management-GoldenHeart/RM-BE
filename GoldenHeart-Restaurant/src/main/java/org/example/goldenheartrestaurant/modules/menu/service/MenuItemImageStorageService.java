package org.example.goldenheartrestaurant.modules.menu.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.example.goldenheartrestaurant.common.config.CloudinaryProperties;
import org.example.goldenheartrestaurant.common.exception.BadRequestException;
import org.example.goldenheartrestaurant.common.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MenuItemImageStorageService {

    private static final String IMAGE_CONTENT_TYPE_PREFIX = "image/";

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    public StoredMenuItemImage uploadMenuItemImage(MultipartFile imageFile,
                                                   Integer branchId,
                                                   String menuItemName) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }
        validateImageFile(imageFile);
        ensureCloudinaryConfigured();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    imageFile.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "folder", resolveFolder(branchId),
                            "public_id", buildPublicId(menuItemName),
                            "use_filename", true,
                            "unique_filename", true,
                            "overwrite", false
                    )
            );

            String secureUrl = asString(uploadResult.get("secure_url"));
            String publicId = asString(uploadResult.get("public_id"));

            if (!StringUtils.hasText(secureUrl) || !StringUtils.hasText(publicId)) {
                throw new ConflictException("Cloudinary did not return a usable image result");
            }

            return new StoredMenuItemImage(secureUrl, publicId);
        } catch (IOException exception) {
            throw new ConflictException("Menu image upload failed");
        }
    }

    public void deleteMenuItemImage(String publicId) {
        if (!StringUtils.hasText(publicId) || !cloudinaryProperties.isConfigured()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "image",
                    "invalidate", true
            ));
        } catch (Exception ignored) {
            // Xóa ảnh cũ thất bại không nên làm hỏng luồng update món ăn đã lưu DB thành công.
        }
    }

    private void validateImageFile(MultipartFile imageFile) {
        String contentType = imageFile.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith(IMAGE_CONTENT_TYPE_PREFIX)) {
            throw new BadRequestException("Only image files are supported for menu items");
        }

        long maxBytes = cloudinaryProperties.getMaxFileSize().toBytes();
        if (imageFile.getSize() > maxBytes) {
            throw new BadRequestException("Menu image exceeds the configured upload size limit");
        }
    }

    private void ensureCloudinaryConfigured() {
        if (!cloudinaryProperties.isConfigured()) {
            throw new ConflictException("Cloudinary is not configured for menu image upload");
        }
    }

    private String resolveFolder(Integer branchId) {
        String baseFolder = StringUtils.hasText(cloudinaryProperties.getFolder())
                ? cloudinaryProperties.getFolder().trim()
                : "goldenheart/menu-items";
        return branchId != null ? baseFolder + "/branch-" + branchId : baseFolder;
    }

    private String buildPublicId(String menuItemName) {
        String baseName = StringUtils.hasText(menuItemName) ? menuItemName.trim() : "menu-item";
        String normalized = Normalizer.normalize(baseName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (!StringUtils.hasText(normalized)) {
            normalized = "menu-item";
        }
        return normalized + "-" + System.currentTimeMillis();
    }

    private String asString(Object value) {
        return value instanceof String stringValue ? stringValue : null;
    }

    public record StoredMenuItemImage(String imageUrl, String imagePublicId) {
    }
}
