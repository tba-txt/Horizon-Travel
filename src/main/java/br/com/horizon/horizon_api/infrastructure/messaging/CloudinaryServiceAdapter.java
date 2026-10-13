package br.com.horizon.horizon_api.infrastructure.messaging;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudinaryServiceAdapter {
	
	private final Cloudinary cloudinary;
	
	public CloudinaryServiceAdapter(Cloudinary cloudinary) {
		this.cloudinary = cloudinary;
	}
	
	public String uploadImage(MultipartFile file, String publicId) throws IOException {
		Map<String, Object> params = new HashMap<>();
		params.put("public_id", publicId);
		params.put("overwrite", true);
		params.put("resource_type", "image");
		
		Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
		return (String) uploadResult.get("secure_url");
	}
	
	public String uploadImage(MultipartFile file) throws IOException {
		Map<String, Object> params = new HashMap<>();
		params.put("folder", "horizon/profiles");
		params.put("resource_type", "auto");
		
		Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
		return (String) uploadResult.get("secure_url");
	}
	
	public String uploadImageFromUrl(String imageUrl, String publicId) throws IOException {
		Map<String, Object> params = new HashMap<>();
		params.put("public_id", publicId);
		params.put("overwrite", true);
		params.put("resource_type", "image");
		
		Map uploadResult = cloudinary.uploader().upload(imageUrl, params);
		return (String) uploadResult.get("secure_url");
	}
	
	public Map getImageDetails(String publicId) throws Exception {
		Map<String, Object> params = ObjectUtils.asMap(
			"quality_analysis", true
		);
		return cloudinary.api().resource(publicId, params);
	}
	
	public String getTransformedImageUrl(String publicId, int width, int height) {
		Transformation transformation = new Transformation()
			.crop("pad")
			.width(width)
			.height(height)
			.background("auto:predominant");
		
		return cloudinary.url().transformation(transformation).imageTag(publicId);
	}
	
	public String getTransformedImageUrl(String publicId) {
		return cloudinary.url().publicId(publicId).generate();
	}
	
	public void deleteImage(String publicId) throws IOException {
		cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
	}
}
