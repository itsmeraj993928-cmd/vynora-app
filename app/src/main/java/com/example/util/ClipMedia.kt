package com.example.util

/** A still picture for a reel: the stored thumbnail, or a frame taken from the Cloudinary video. */
fun reelPoster(mediaUrl: String, thumbnailUrl: String): String {
    if (thumbnailUrl.isNotBlank()) return thumbnailUrl
    if (mediaUrl.contains("res.cloudinary.com") && mediaUrl.contains("/video/upload/")) {
        val dot = mediaUrl.lastIndexOf('.')
        val noExt = if (dot > mediaUrl.lastIndexOf('/')) mediaUrl.substring(0, dot) else mediaUrl
        return noExt.replaceFirst("/video/upload/", "/video/upload/so_0,w_540,q_auto/") + ".jpg"
    }
    return ""
}
