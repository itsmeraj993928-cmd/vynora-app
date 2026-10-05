package com.example.util

/**
 * Centralized configuration file for Cloudinary uploads.
 * Cloud name: x5zwfz5y
 * Unsigned upload preset: vynora_unsigned
 */
object CloudinaryConfig {
    const val CLOUD_NAME = "x5zwfz5y"
    const val UPLOAD_PRESET = "vynora_unsigned"
    const val UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/auto/upload"
    const val IMAGE_UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"
    const val VIDEO_UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/video/upload"
}
