package com.back.fairytale.global.util.impl

import com.back.fairytale.global.util.CloudStorage
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.webp.WebpWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.util.*
import java.util.stream.Collectors

@Component
class GoogleCloudStorage(
    @Value("\${spring.cloud.gcp.storage.bucket}")
    private val bucketName: String,
    private val storage: Storage
) : CloudStorage {

    override fun uploadImages(imgFiles: List<MultipartFile>): List<String> {
        return imgFiles.parallelStream()
            .map { uploadImageToCloud(it) }
            .collect(Collectors.toList())
    }

    override fun deleteImages(imageUrls: List<String>) {
        imageUrls.parallelStream().forEach { deleteImageFromCloud(it) }
    }

    override fun updateImages(imageUrls: List<String>, imgFiles: List<MultipartFile>) {
        deleteImages(imageUrls)
        uploadImages(imgFiles)
    }

    fun uploadImageBytesToCloud(imgByte: ByteArray): String {
        try {
            val compressedImage = ImmutableImage.loader()
                .fromBytes(imgByte)
                .forWriter(WebpWriter.DEFAULT.withQ(1).withM(0).withZ(0))
                .bytes()

            val uuid = UUID.randomUUID().toString()

            val blobInfo = BlobInfo.newBuilder(bucketName, uuid)
                .setContentType("image/webp")
                .build()

            storage.create(blobInfo, compressedImage)

            return formatUrl(uuid)
        } catch (e: IOException) {
            throw RuntimeException("이미지 압축 또는 업로드 실패: " + e.message, e)
        }
    }

    private fun uploadImageToCloud(imgFile: MultipartFile): String {
        try {
            val compressedImage = ImmutableImage.loader()
                .fromBytes(imgFile.bytes)
                .forWriter(WebpWriter.DEFAULT.withQ(1).withM(0).withZ(0))
                .bytes()

            val uuid = UUID.randomUUID().toString()

            val blobInfo = BlobInfo.newBuilder(bucketName, uuid)
                .setContentType("image/webp")
                .build()

            storage.create(blobInfo, compressedImage)

            return formatUrl(uuid)
        } catch (e: IOException) {
            throw RuntimeException("이미지 압축 또는 업로드 실패: " + e.message, e)
        }
    }

    private fun deleteImageFromCloud(imageUrl: String) {
        val blobId = BlobId.of(bucketName, imageUrl.substring(imageUrl.lastIndexOf("/") + 1))

        val result = storage.delete(blobId)
        if (!result) {
            throw RuntimeException("클라우드에서 이미지 삭제 실패")
        }
    }

    private fun formatUrl(uuid: String): String {
        return "https://storage.googleapis.com/$bucketName/$uuid"
    }
}
